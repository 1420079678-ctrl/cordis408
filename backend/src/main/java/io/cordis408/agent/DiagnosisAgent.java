package io.cordis408.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.cordis408.knowledge.KnowledgeBase;
import io.cordis408.knowledge.KnowledgePoint;
import io.cordis408.knowledge.QuizItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 批改 / 诊断 Agent：判定作答、定位薄弱考点、回写学习者画像。
 * provide {@code diagnosis}，依赖 learner.state 与 knowledge.base。
 */
@Component
public class DiagnosisAgent extends AbstractAgent {

    public static final String KEY = "diagnosis";
    private final KnowledgeBase kb;

    public DiagnosisAgent(KnowledgeBase kb) { super("diagnosis"); this.kb = kb; }

    @Override public String agentName() { return "批改诊断 Agent"; }
    @Override protected String provideKey(JsonNode c) { return KEY; }
    @Override protected Set<String> requireKeys(JsonNode c) {
        return Set.of(LearnerModelAgent.KEY, RetrievalAgent.KEY);
    }

    /** 判定一道单选题/对照综合题，并把结果写入画像。 */
    public Map<String, Object> grade(WorkContext w, QuizItem quiz, String userAnswer, LearnerModelAgent learner) {
        long t = System.currentTimeMillis();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("quizId", quiz.id());
        r.put("type", quiz.type());
        boolean correct;
        if ("single".equals(quiz.type())) {
            correct = userAnswer != null && userAnswer.trim().equalsIgnoreCase(quiz.answer().trim());
        } else {
            correct = false; // 综合题需人工/大模型评判，这里只给参考
            r.put("subjective", true);
        }
        r.put("correct", correct);
        r.put("rightAnswer", quiz.answer());
        r.put("analysis", quiz.analysis());
        String kp = quiz.kpIds() != null && !quiz.kpIds().isEmpty() ? quiz.kpIds().get(0) : quiz.id();
        learner.recordQuiz(w.sessionId, quiz.subject(), kp, correct, quiz.difficulty());
        w.trace(type(), agentName(), "批改", (correct ? "回答正确" : "回答错误") + "（" + quiz.id() + "）", t);
        return r;
    }

    /** 基于画像生成薄弱点诊断报告。 */
    public Map<String, Object> diagnose(String sessionId, LearnerModelAgent learner) {
        Map<String, Object> snap = learner.snapshot(sessionId);
        List<String> weakIds = (List<String>) snap.getOrDefault("weakPointIds", List.of());
        List<Map<String, String>> weakPoints = new ArrayList<>();
        for (String id : weakIds) {
            KnowledgePoint p = kb.point(id);
            if (p != null) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id", p.id());
                m.put("subject", Subjects.name(p.subject()));
                m.put("chapter", p.chapter());
                m.put("title", p.title());
                m.put("summary", p.summary());
                weakPoints.add(m);
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("snapshot", snap);
        report.put("weakSubjects", learner.weakSubjects(sessionId).stream().map(Subjects::name).toList());
        report.put("weakPoints", weakPoints);
        return report;
    }
}
