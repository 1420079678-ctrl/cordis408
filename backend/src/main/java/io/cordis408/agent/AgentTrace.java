package io.cordis408.agent;

/**
 * 一次请求中某个 Agent 的处理留痕（用于前端可视化"多 Agent 协同链路"，
 * 让用户看到问题如何在路由→检索→专家→教学/诊断等组件间流转）。
 */
public class AgentTrace {
    public String agent;
    public String role;
    public String action;
    public String output;
    public long durationMs;

    public AgentTrace(String agent, String role, String action, String output, long durationMs) {
        this.agent = agent;
        this.role = role;
        this.action = action;
        this.output = output;
        this.durationMs = durationMs;
    }
}
