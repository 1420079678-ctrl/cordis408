package io.cordis408.runtime;

/**
 * Fiber 生命周期状态（论文 §4.3.3 的"惯性状态机"，对应形式化中的 θ）。
 *
 * <pre>
 *   INACTIVE ──依赖满足──▶ LOADING ──apply 成功──▶ ACTIVE
 *      ▲                     │                      │
 *      │                     ▼                      ▼
 *      └────逆操作回收──── UNLOADING ◀──依赖撤离/禁用──┘
 *                            │
 *                            ▼
 *                          FAILED（Inactive(ξ)，记录错误 ξ，等待下次 refresh）
 * </pre>
 */
public enum FiberState {
    /** 未激活：依赖未满足或被禁用（对应 ⊥ / INACTIVE）。 */
    INACTIVE,
    /** 加载中：依赖已满足，正在执行组件 apply（对应 LOADING / Reloading）。 */
    LOADING,
    /** 已激活：组件效果已提交，对外 provide 的协效果可用（对应 ACTIVE / committed ω）。 */
    ACTIVE,
    /** 卸载中：正在 LIFO 回滚全部效果（对应 L-Leave / UNLOADING）。 */
    UNLOADING,
    /** 失败：apply 抛错，携带错误原因，target 置为 ⊥，等待依赖或配置变化后重试。 */
    FAILED;

    /** 是否为"存活"状态（会参与响应式通知）。 */
    public boolean isLive() {
        return this != INACTIVE;
    }
}
