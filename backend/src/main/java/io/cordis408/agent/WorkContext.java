package io.cordis408.agent;

import io.cordis408.knowledge.KnowledgeBase;
import io.cordis408.knowledge.KnowledgePoint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 协同黑板（Blackboard）：一次问答请求在多个 Agent 之间流转的统一工作上下文，
 * 是论文"统一 Context 范式"在应用层的体现——每个 Agent 只读写自己关心的槽位，
 * 并通过协效果依赖决定自己能否参与本次协同。
 */
public class WorkContext {

    public String sessionId = "default";
    public String rawQuestion = "";
    /** ask 答疑 / tutor 苏格拉底引导 / quiz 出题 / grade 批改诊断 / plan 学习规划 / free 自由对话 */
    public String mode = "ask";
    /** router 判定的学科，null 表示跨学科/未定。 */
    public String subject;
    public List<String> keywords = new ArrayList<>();

    public List<KnowledgeBase.Scored> retrieved = new ArrayList<>();
    public String evidenceText = "";
    public String draftAnswer = "";
    public String finalAnswer = "";
    public final List<String> followUps = new ArrayList<>();
    public final List<AgentTrace> traces = new ArrayList<>();
    public final Map<String, Object> extra = new LinkedHashMap<>();

    /** 画像（由 LearnerModelAgent 填充/更新）。 */
    public java.util.Map<String, Object> learnerSnapshot = new LinkedHashMap<>();

    public void trace(String agent, String role, String action, String output, long startMs) {
        traces.add(new AgentTrace(agent, role, action, output, System.currentTimeMillis() - startMs));
    }

    public List<KnowledgePoint> evidencePoints() {
        return retrieved.stream().map(KnowledgeBase.Scored::point).toList();
    }
}
