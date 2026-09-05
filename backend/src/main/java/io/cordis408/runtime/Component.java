package io.cordis408.runtime;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

/**
 * 动态组合的最小单元 —— 组件（论文 §4 的 Component，形式化七元组 {@code <d,p,e,π,σ,τ,θ>}
 * 中由组件作者负责声明的前三项）。
 *
 * <ul>
 *   <li><b>d / inject</b>：协效果规格，声明本组件"需要"环境提供哪些 key（空间维依赖）；</li>
 *   <li><b>p / provide</b>：本组件激活后向上下文"提供"哪些 key；</li>
 *   <li><b>e / apply</b>：效果函数，在派生子上下文中执行，返回一个逆操作 Disposable，
 *       组件卸载时由运行时自动调用，实现时间维可组合。</li>
 * </ul>
 *
 * 一个组件只有当其 inject 的全部 key 都存在 ACTIVE 的提供者时才会被激活（Theorem 63：
 * 依赖只约束"何时激活"，不约束"何时加载模块"），因此模块可以并发预取、按需激活。
 */
public interface Component {

    /** 组件类型名（对应 loader entry 的 url / type，也是 ComponentFactory 的注册键）。 */
    String type();

    /** d：协效果（依赖）声明。返回空集合表示无外部依赖。 */
    default Set<String> inject(JsonNode config) { return Set.of(); }

    /** p：本组件激活后对外提供的协效果 key。 */
    default Set<String> provide(JsonNode config) { return Set.of(); }

    /**
     * e：效果函数。仅当所有 inject 依赖被满足时调用。
     *
     * @param ctx    本组件专属的派生子上下文（derived realization）
     * @param config 绑定进来的声明式配置
     * @return 逆操作：组件卸载时自动执行，撤销这里造成的一切修改
     */
    Disposable apply(RuntimeContext ctx, JsonNode config) throws Exception;

    /** 供拓扑/监控展示的人类可读名称。 */
    default String displayName(JsonNode config) { return type(); }
}
