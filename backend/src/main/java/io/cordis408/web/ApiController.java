package io.cordis408.web;

import io.cordis408.agent.*;
import io.cordis408.knowledge.KnowledgeBase;
import io.cordis408.knowledge.KnowledgePoint;
import io.cordis408.knowledge.QuizItem;
import io.cordis408.llm.LlmGateway;
import io.cordis408.web.dto.AskRequest;
import io.cordis408.web.dto.GradeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 408 学习主接口：多 Agent 问答、学科知识、练习、批改、诊断、画像。 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class ApiController {

    private final SupervisorAgent supervisor;
    private final ProblemAgent problem;
    private final DiagnosisAgent diagnosis;
    private final LearnerModelAgent learner;
    private final RetrievalAgent retrieval;
    private final KnowledgeBase kb;
    private final LlmGateway llm;

    public ApiController(SupervisorAgent supervisor, ProblemAgent problem, DiagnosisAgent diagnosis,
                         LearnerModelAgent learner, RetrievalAgent retrieval, KnowledgeBase kb, LlmGateway llm) {
        this.supervisor = supervisor;
        this.problem = problem;
        this.diagnosis = diagnosis;
        this.learner = learner;
        this.retrieval = retrieval;
        this.kb = kb;
        this.llm = llm;
    }

    /** 统一问答入口（答疑/引导/出题/规划）。 */
    @PostMapping("/ask")
    public Map<String, Object> ask(@Valid @RequestBody AskRequest req) {
        WorkContext w = new WorkContext();
        w.sessionId = req.safeSession();
        w.rawQuestion = req.message();
        if (req.mode() != null && !req.mode().isBlank()) w.mode = req.mode();
        if (req.subject() != null && Subjects.isValid(req.subject())) w.subject = req.subject();
        if (req.count() != null) w.extra.put("count", req.count());
        if (req.difficulty() != null) w.extra.put("difficulty", req.difficulty());

        supervisor.orchestrate(w);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("answer", w.finalAnswer);
        resp.put("mode", w.mode);
        resp.put("modeName", RouterAgent.modeName(w.mode));
        resp.put("subject", w.subject);
        resp.put("subjectName", w.subject == null ? null : Subjects.name(w.subject));
        resp.put("traces", w.traces);
        resp.put("evidence", w.evidencePoints().stream().map(this::pointBrief).toList());
        resp.put("followUps", w.followUps);
        resp.put("llmEnabled", llm.enabled());
        resp.put("model", llm.model());
        if (w.extra.containsKey("quizzes"))
            resp.put("quizzes", ((List<QuizItem>) w.extra.get("quizzes")).stream().map(this::studentView).toList());
        if (w.extra.containsKey("plan")) resp.put("plan", w.extra.get("plan"));
        resp.put("learner", learner.snapshot(w.sessionId));
        return resp;
    }

    /** 学科总览：统计 + 章节。 */
    @GetMapping("/subjects")
    public Map<String, Object> subjects() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stats", kb.stats());
        Map<String, Object> chapters = new LinkedHashMap<>();
        for (String code : Subjects.NAME.keySet()) chapters.put(code, kb.chaptersOf(code));
        m.put("chapters", chapters);
        return m;
    }

    /** 某学科全部考点。 */
    @GetMapping("/subjects/{code}/points")
    public List<Map<String, Object>> points(@PathVariable String code,
                                            @RequestParam(required = false) String frequency) {
        return kb.pointsOf(code).stream()
                .filter(p -> frequency == null || frequency.equals(p.frequency()))
                .map(this::pointBrief).toList();
    }

    /** 抽题练习（面向学生，不含答案）。 */
    @GetMapping("/practice/quiz")
    public List<Map<String, Object>> quiz(@RequestParam(required = false) String subject,
                                          @RequestParam(required = false) Integer difficulty,
                                          @RequestParam(required = false) String chapter,
                                          @RequestParam(defaultValue = "5") int n) {
        WorkContext w = new WorkContext();
        w.subject = subject;
        return problem.compose(w, n, difficulty).stream().map(this::studentView).toList();
    }

    /** 提交答案批改（此时返回正确答案与解析）。 */
    @PostMapping("/practice/grade")
    public Map<String, Object> grade(@RequestBody GradeRequest req) {
        QuizItem q = kb.quiz(req.quizId());
        if (q == null) return Map.of("error", "题目不存在: " + req.quizId());
        WorkContext w = new WorkContext();
        w.sessionId = req.safeSession();
        w.subject = q.subject();
        Map<String, Object> result = diagnosis.grade(w, q, req.answer(), learner);
        result.put("quiz", q); // 回传完整题面
        result.put("learner", learner.snapshot(w.sessionId));
        return result;
    }

    /** 薄弱点诊断报告。 */
    @GetMapping("/diagnosis")
    public Map<String, Object> diagnose(@RequestParam(defaultValue = "default") String sessionId) {
        return diagnosis.diagnose(sessionId, learner);
    }

    /** 学习者画像。 */
    @GetMapping("/learner")
    public Map<String, Object> learner(@RequestParam(defaultValue = "default") String sessionId) {
        return learner.snapshot(sessionId);
    }

    @PostMapping("/learner/reset")
    public Map<String, Object> resetLearner(@RequestParam(defaultValue = "default") String sessionId) {
        learner.reset(sessionId);
        return Map.of("ok", true, "learner", learner.snapshot(sessionId));
    }

    // ===== 视图裁剪 =====
    private Map<String, Object> pointBrief(KnowledgePoint p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.id());
        m.put("subject", p.subject());
        m.put("chapter", p.chapter());
        m.put("title", p.title());
        m.put("frequency", p.frequency());
        m.put("summary", p.summary());
        m.put("detail", p.detail());
        m.put("keyPoints", p.keyPoints());
        m.put("related", p.related());
        return m;
    }

    /** 学生侧题目视图：隐藏答案与解析。 */
    private Map<String, Object> studentView(QuizItem q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.id());
        m.put("subject", q.subject());
        m.put("chapter", q.chapter());
        m.put("difficulty", q.difficulty());
        m.put("type", q.type());
        m.put("stem", q.stem());
        m.put("options", q.options());
        return m;
    }
}
