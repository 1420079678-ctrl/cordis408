package io.cordis408.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.cordis408.knowledge.KnowledgeBase;
import io.cordis408.knowledge.KnowledgePoint;
import io.cordis408.llm.LlmGateway;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 学科专家 Agent（数据结构 / 组成原理 / 计算机网络 / 操作系统）。
 *
 * <p>同一个无状态组件定义，由声明式加载器按 config.subject 实例化为 4 个 Fiber，
 * 分别 provide {@code expert.ds/co/cn/os}。它们都 inject {@code knowledge.base} 与
 * {@code learner.state}：只有当检索与画像两个上游都 ACTIVE 时专家才会被激活（空间维约束）。</p>
 *
 * <p>作答采用 RAG：优先用大模型基于检索证据生成；未配置/失败时回退到确定性模板，保证离线可用。</p>
 */
@Component
public class SubjectExpertAgent extends AbstractAgent {

    private final LlmGateway llm;

    public SubjectExpertAgent(LlmGateway llm) { super("subject-expert"); this.llm = llm; }

    @Override public String agentName() { return "学科专家 Agent"; }

    @Override protected Set<String> requireKeys(JsonNode c) {
        return Set.of(RetrievalAgent.KEY, LearnerModelAgent.KEY);
    }

    /** 按 config.subject 发布为 expert.ds / expert.co / expert.cn / expert.os。 */
    @Override protected String provideKey(JsonNode config) {
        JsonNode s = config.get("subject");
        return s == null ? null : Subjects.EXPERT_KEY.get(s.asText());
    }

    @Override public String displayName(JsonNode config) {
        JsonNode s = config.get("subject");
        return s == null ? agentName() : Subjects.name(s.asText()) + "专家 Agent";
    }

    /** 基于证据产出讲解，写回 work.draftAnswer。 */
    public void explain(WorkContext w) {
        long t = System.currentTimeMillis();
        String subjectName = Subjects.name(w.subject);
        String llmAnswer = llm.complete(buildSystem(subjectName), buildUser(w));
        if (llmAnswer != null && !llmAnswer.isBlank()) {
            w.draftAnswer = llmAnswer.trim();
            w.trace("expert." + w.subject, subjectName + "专家", "大模型基于证据生成",
                    "生成" + llmAnswer.length() + "字讲解", t);
        } else {
            w.draftAnswer = builtinExplain(w, subjectName);
            w.trace("expert." + w.subject, subjectName + "专家", "内置确定性讲解引擎",
                    "基于" + w.retrieved.size() + "个考点组织答案", t);
        }
    }

    private String buildSystem(String subjectName) {
        return "你是中国考研408计算机学科专业基础中《" + subjectName + "》的资深命题研究与辅导专家。"
                + "严格依据用户提供的【检索证据】作答，概念、公式、复杂度必须准确；"
                + "结构为：先给一句话结论，再分层讲解，随后列出易错点/对比，最后给一道自测小题。"
                + "证据未覆盖的内容明确说明，不要编造。使用 Markdown，语言精炼。";
    }

    private String buildUser(WorkContext w) {
        return "学生问题：" + w.rawQuestion + "\n\n检索证据：\n"
                + (w.evidenceText.isBlank() ? "（本次无强匹配证据，请结合" + Subjects.name(w.subject) + "通用知识谨慎作答）" : w.evidenceText)
                + "\n\n学习者画像：" + w.learnerSnapshot;
    }

    private String builtinExplain(WorkContext w, String subjectName) {
        if (w.retrieved.isEmpty()) {
            return "## " + subjectName + " · 答疑\n\n"
                    + "知识库中没有与该问题强匹配的考点，我先给出通用思路：\n\n"
                    + "1. 先定位它属于哪一章、哪个核心概念；\n"
                    + "2. 回忆该概念的**定义、成立条件、边界情况**；\n"
                    + "3. 用一道典型例题验证理解。\n\n"
                    + "> 你可以补充更具体的关键词（如“哈夫曼 WPL”“循环队列判满”“TCP 快恢复”），"
                    + "我会调出对应高频考点做精确讲解。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(subjectName).append(" · 考点精讲\n\n");
        int idx = 1;
        for (KnowledgeBase.Scored s : w.retrieved) {
            KnowledgePoint p = s.point();
            sb.append("### ").append(idx++).append(". ").append(p.title()).append("\n\n");
            sb.append("**一句话结论：** ").append(p.summary()).append("\n\n");
            sb.append(p.detail()).append("\n\n");
            if (p.keyPoints() != null && !p.keyPoints().isEmpty()) {
                sb.append("**关键 / 易错点：**\n");
                p.keyPoints().forEach(k -> sb.append("- ").append(k).append("\n"));
                sb.append("\n");
            }
        }
        KnowledgePoint top = w.retrieved.get(0).point();
        sb.append("**自测：** 请合上书，复述「").append(top.title())
          .append("」的结论与一个易错点，确认是否真正掌握。");
        return sb.toString();
    }
}
