package io.cordis408.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.cordis408.knowledge.KnowledgePoint;
import io.cordis408.llm.LlmGateway;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 苏格拉底式教学 Agent：不直接抛出答案，而是用递进式提问引导学生自己推出结论。
 * provide {@code tutor}，依赖检索与画像。
 */
@Component
public class TutorAgent extends AbstractAgent {

    public static final String KEY = "tutor";
    private final LlmGateway llm;

    public TutorAgent(LlmGateway llm) { super("tutor"); this.llm = llm; }

    @Override public String agentName() { return "苏格拉底教学 Agent"; }
    @Override protected String provideKey(JsonNode c) { return KEY; }
    @Override protected Set<String> requireKeys(JsonNode c) {
        return Set.of(RetrievalAgent.KEY, LearnerModelAgent.KEY);
    }

    public void guide(WorkContext w) {
        long t = System.currentTimeMillis();
        String sys = "你是擅长苏格拉底产婆术的408辅导老师。不要直接给最终答案，而是用3-4个由浅入深的问题，"
                + "引导学生自己发现答案；每个问题后用括号给出你期待学生想到的关键点。最后用一句话总结学生应当得出的结论。";
        String user = "学生卡住的问题：" + w.rawQuestion + "\n相关考点证据：\n" + w.evidenceText;
        String out = llm.complete(sys, user);
        if (out == null || out.isBlank()) out = builtinGuide(w);
        w.draftAnswer = out;
        w.trace(type(), agentName(), "生成引导式问题链", "不直接给答案", t);
    }

    private String builtinGuide(WorkContext w) {
        StringBuilder sb = new StringBuilder("## 一起来推一遍（先别要答案）\n\n");
        if (w.retrieved.isEmpty()) {
            sb.append("1. 这个问题涉及哪一章的哪个概念？先把名词圈出来。\n2. 它的定义和成立条件是什么？\n3. 题目条件和标准情形哪里不同？\n");
            return sb.toString();
        }
        KnowledgePoint p = w.retrieved.get(0).point();
        sb.append("我们围绕「").append(p.title()).append("」一步步来：\n\n");
        sb.append("1. **识别**：题目描述的情形，对应下面哪个结论——").append(p.summary()).append("（先自己判断对不对）\n\n");
        sb.append("2. **条件**：这个结论成立需要哪些前提？（想想关键/易错点）\n");
        if (p.keyPoints() != null) p.keyPoints().forEach(k -> sb.append("   - 提示方向：").append(k).append("\n"));
        sb.append("\n3. **变式**：如果改变其中一个条件，结论还成立吗？为什么？\n\n");
        sb.append("4. **回扣**：用一句话说出你的判断，我再帮你确认。\n\n");
        sb.append("> 当你能自己回答第 2、3 问时，这类题就真正吃透了。");
        return sb.toString();
    }
}
