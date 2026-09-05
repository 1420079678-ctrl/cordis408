package io.cordis408.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.cordis408.knowledge.KnowledgeBase;
import io.cordis408.knowledge.KnowledgePoint;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 学习路径规划 Agent：依据学习者画像中的掌握度与薄弱点，生成分阶段、分学科的复习计划。
 * provide {@code planner}，依赖 learner.state 与 knowledge.base。
 */
@Component
public class PlannerAgent extends AbstractAgent {

    public static final String KEY = "planner";
    private final KnowledgeBase kb;

    public PlannerAgent(KnowledgeBase kb) { super("planner"); this.kb = kb; }

    @Override public String agentName() { return "学习路径规划 Agent"; }
    @Override protected String provideKey(JsonNode c) { return KEY; }
    @Override protected Set<String> requireKeys(JsonNode c) {
        return Set.of(LearnerModelAgent.KEY, RetrievalAgent.KEY);
    }

    public Map<String, Object> makePlan(WorkContext w, LearnerModelAgent learner) {
        long t = System.currentTimeMillis();
        // 学科优先级：薄弱者优先，未开始学科按 408 常规学习顺序补齐
        List<String> order = new ArrayList<>(learner.weakSubjects(w.sessionId));
        for (String s : List.of(Subjects.DS, Subjects.CO, Subjects.OS, Subjects.CN))
            if (!order.contains(s)) order.add(s);

        List<Map<String, Object>> phases = new ArrayList<>();
        phases.add(phase("阶段一 · 基础精讲（约6-8周）",
                "按优先级顺序系统过一遍考纲，理解为主、建立知识框架",
                buildBasicTasks(order)));
        phases.add(phase("阶段二 · 专题强化（约4周）",
                "针对薄弱学科与高频考点专项突破，配合分章刷题",
                buildStrongTasks(w, learner, order)));
        phases.add(phase("阶段三 · 真题冲刺（约3-4周）",
                "成套真题限时训练，错题回归考点，形成做题节奏",
                List.of("每2天一套408真题/模拟，严格计时180分钟",
                        "建立错题本，按考点归类并回溯对应章节",
                        "复盘数据结构算法题与组成原理综合题的固定套路",
                        "最后一周只看错题本与高频考点速记")));

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("subjectOrder", order.stream().map(Subjects::name).toList());
        plan.put("phases", phases);
        plan.put("markdown", render(order, phases));
        w.draftAnswer = (String) plan.get("markdown");
        w.trace(type(), agentName(), "生成个性化学习路径",
                "学科优先级：" + String.join(">", order.stream().map(Subjects::name).toList()), t);
        return plan;
    }

    private List<String> buildBasicTasks(List<String> order) {
        List<String> tasks = new ArrayList<>();
        for (String s : order) {
            List<String> chapters = kb.chaptersOf(s);
            tasks.add(Subjects.name(s) + "：依次学习 " + String.join("、", chapters));
        }
        return tasks;
    }

    private List<String> buildStrongTasks(WorkContext w, LearnerModelAgent learner, List<String> order) {
        List<String> tasks = new ArrayList<>();
        Map<String, Object> snap = learner.snapshot(w.sessionId);
        for (String s : order) {
            List<String> high = kb.pointsOf(s).stream()
                    .filter(p -> "high".equals(p.frequency())).map(KnowledgePoint::title).limit(6).toList();
            tasks.add("【" + Subjects.name(s) + "高频】" + String.join("、", high));
        }
        return tasks;
    }

    private Map<String, Object> phase(String stage, String goal, List<String> items) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stage", stage);
        m.put("goal", goal);
        m.put("items", items);
        return m;
    }

    private String render(List<String> order, List<Map<String, Object>> phases) {
        StringBuilder sb = new StringBuilder("## 408 个性化复习规划\n\n");
        sb.append("**学科优先级：** ").append(String.join(" → ",
                order.stream().map(Subjects::name).toList())).append("\n\n");
        for (Map<String, Object> p : phases) {
            sb.append("### ").append(p.get("stage")).append("\n\n");
            sb.append("> ").append(p.get("goal")).append("\n\n");
            for (Object o : (List<?>) p.get("items")) sb.append("- ").append(o).append("\n");
            sb.append("\n");
        }
        return sb.toString();
    }
}
