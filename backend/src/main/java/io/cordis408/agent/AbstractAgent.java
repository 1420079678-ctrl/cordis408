package io.cordis408.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.cordis408.runtime.Component;
import io.cordis408.runtime.Disposable;
import io.cordis408.runtime.RuntimeContext;

import java.util.Set;

/**
 * Agent 组件基类：把"408 智能体"适配为 Cordis {@link Component}。
 *
 * <p>激活时通过唯一的变更原语 {@code ctx.set(provideKey, this)} 把自身能力发布到统一上下文；
 * 卸载时该效果的逆操作由运行时自动执行（注销能力），无需手写清理路径 —— 这正是论文强调的
 * "locality of concern"：正确性由抽象一次性保证，而不依赖每个 Agent 作者记得卸载。</p>
 */
public abstract class AbstractAgent implements Component {

    private final String type;

    protected AbstractAgent(String type) { this.type = type; }

    @Override public String type() { return type; }

    /** d：依赖的协效果 key（默认无）。 */
    protected Set<String> requireKeys(JsonNode config) { return Set.of(); }

    /** p：本 Agent 对外发布的能力 key（默认不发布）。 */
    protected String provideKey(JsonNode config) { return null; }

    @Override public final Set<String> inject(JsonNode config) { return requireKeys(config); }

    @Override public final Set<String> provide(JsonNode config) {
        String k = provideKey(config);
        return k == null ? Set.of() : Set.of(k);
    }

    /** 激活前的初始化钩子（可选）。 */
    protected void onActivate(JsonNode config) { }

    @Override
    public Disposable apply(RuntimeContext ctx, JsonNode config) throws Exception {
        onActivate(config);
        String key = provideKey(config);
        if (key != null) return ctx.set(key, this);   // 发布能力，逆操作即注销
        return Disposable.NOOP;
    }

    public abstract String agentName();

    @Override public String displayName(JsonNode config) { return agentName(); }
}
