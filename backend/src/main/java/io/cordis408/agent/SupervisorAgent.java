package io.cordis408.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.cordis408.knowledge.KnowledgePoint;
import io.cordis408.knowledge.QuizItem;
import io.cordis408.runtime.CordisRuntime;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 督导 / 编排 Agent（Supervisor）——多 Agent 协同的总控门面，provide {@code supervisor}。
 *
 * <p>它本身不产生学科知识，只负责按"路由→检索→专家/教学→画像更新"的顺序组织协同，
 * 并在每一步通过运行时判断对应能力当前是否 ACTIVE（provided-by）：某个 Agent 被停用/热替换时，
 * 相关环节自动降级而不是整体崩溃，这正是响应式协效果（空间维）在应用层的体现。</p>
 */
@Component
public class SupervisorAgent extends AbstractAgent {

    public static final String KEY = "supervisor";

    private final CordisRuntime runtime;
    private final RouterAgent router;
    private final RetrievalAgent retrieval;
    private final LearnerModelAgent learner;
    private final SubjectExpertAgent expert;
    private final TutorAgent tutor;
    private final ProblemAgent problem;
    private final PlannerAgent planner;

    public SupervisorAgent(CordisRuntime runtime, RouterAgent router, RetrievalAgent retrieval,
                           LearnerModelAgent learner, SubjectExpertAgent expert, TutorAgent tutor,
                           ProblemAgent problem, PlannerAgent planner) {
        super("supervisor");
        this.runtime = runtime;
        this.router = router;
        this.retrieval = retrieval;
        this.learner = learner;
        this.expert = expert;
        this.tutor = tutor;
        this.problem = problem;
        this.planner = planner;
    }

    @Override public String agentName() { return "督导编排 Agent"; }
    @Override protected String provideKey(JsonNode c) { return KEY; }
    @Override protected Set<String> requireKeys(JsonNode c) {
        return Set.of(RouterAgent.KEY, RetrievalAgent.KEY, LearnerModelAgent.KEY);
    }

    private boolean up(String key) { return runtime.isProvided(CordisRuntime.GLOBAL_REALM, key); }

    /** 主协同流程。 */
    public WorkContext orchestrate(WorkContext w) {
        // 1) 路由
        if (up(RouterAgent.KEY)) router.classify(w);
        else w.mode = "ask";

        // 2) 取当前画像
        if (up(LearnerModelAgent.KEY)) w.learnerSnapshot = learner.snapshot(w.sessionId);

        // 3) 检索证据
        if (up(RetrievalAgent.KEY)) retrieval.retrieve(w, 5);
        inferSubject(w);

        // 4) 按模式分派
        switch (w.mode) {
            case "tutor" -> {
                if (up(TutorAgent.KEY)) tutor.guide(w);
                else fallbackExpert(w);
            }
            case "quiz" -> {
                int n = intExtra(w, "count", 3);
                Integer diff = (Integer) w.extra.get("difficulty");
                if (up(ProblemAgent.KEY)) {
                    List<QuizItem> quiz = problem.compose(w, n, diff);
                    w.extra.put("quizzes", quiz);
                    w.draftAnswer = renderQuiz(quiz);
                } else fallbackExpert(w);
            }
            case "plan" -> {
                if (up(PlannerAgent.KEY)) {
                    w.extra.put("plan", planner.makePlan(w, learner));
                } else fallbackExpert(w);
            }
            default -> fallbackExpert(w);
        }

        // 5) 更新学习者画像（时间维：本次协同产生的学习行为被记录，停用本组件时其效果自动回滚）
        if (up(LearnerModelAgent.KEY)) learner.observe(w);

        w.finalAnswer = w.draftAnswer;
        buildFollowUps(w);
        return w;
    }

    /** 学科专家讲解（专家离线则直接用检索证据兜底）。 */
    private void fallbackExpert(WorkContext w) {
        String expertKey = w.subject == null ? null : Subjects.expertKey(w.subject);
        if (expertKey != null && up(expertKey)) {
            expert.explain(w);
        } else if (up(RetrievalAgent.KEY) && !w.retrieved.isEmpty()) {
            // 专家未激活：直接把检索证据组织成简版回答（优雅降级）
            StringBuilder sb = new StringBuilder("## ").append(Subjects.name(w.subject)).append(" · 速答\n\n");
            int i = 1;
            for (var s : w.retrieved) {
                KnowledgePoint p = s.point();
                sb.append(i++).append(". **").append(p.title()).append("**：").append(p.summary()).append("\n\n");
            }
            w.draftAnswer = sb.toString();
            w.trace("supervisor", agentName(), "专家离线降级", "直接使用检索结果", 0);
        } else {
            expert.explain(w); // 最终兜底
        }
    }

    /** 路由未判出学科、但检索结果高度集中于某科时回填学科。 */
    private void inferSubject(WorkContext w) {
        if (w.subject != null || w.retrieved.isEmpty()) return;
        String first = w.retrieved.get(0).point().subject();
        boolean same = w.retrieved.stream().allMatch(s -> s.point().subject().equals(first));
        if (same) w.subject = first;
    }

    private int intExtra(WorkContext w, String k, int dft) {
        Object o = w.extra.get(k);
        return o instanceof Number n ? n.intValue() : dft;
    }

    private String renderQuiz(List<QuizItem> quiz) {
        StringBuilder sb = new StringBuilder("## 针对性练习（").append(quiz.size()).append("题）\n\n");
        int i = 1;
        for (QuizItem q : quiz) {
            sb.append("**").append(i++).append(". [").append(Subjects.name(q.subject()))
              .append("·难度").append(q.difficulty()).append("]** ").append(q.stem()).append("\n\n");
            if (q.options() != null) q.options().forEach((k, v) -> sb.append("- ").append(k).append(". ").append(v).append("\n"));
            sb.append("\n");
        }
        sb.append("> 先独立作答，回复「查看解析」或在练习区提交答案即可批改。");
        return sb.toString();
    }

    private void buildFollowUps(WorkContext w) {
        String s = w.subject == null ? "这门课" : Subjects.name(w.subject);
        switch (w.mode) {
            case "quiz" -> w.followUps.addAll(List.of("换一批题", "难度调高一点", "逐题给我解析"));
            case "plan" -> w.followUps.addAll(List.of(s + "的高频考点有哪些", "帮我细化第一周计划", "先从哪一科开始"));
            case "tutor" -> w.followUps.addAll(List.of("我想出来了，帮我确认", "还是直接讲给我听", "出一道同类题"));
            default -> w.followUps.addAll(List.of("给我出几道" + s + "相关题", "用提问的方式引导我", s + "这章一般怎么考"));
        }
    }
}
