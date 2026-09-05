package io.cordis408.runtime;

/**
 * 可逆效果（Revertible Effect）的"逆操作"。
 *
 * <p>对应论文 §3.1：每一次对上下文的变换都携带一个显式 inverse，运行时负责追踪，
 * 组件被卸载时按 LIFO（后申请、先释放）顺序逐个执行 inverse，从而把共享环境
 * 完整、安全地恢复到组件加载之前 —— 这就是"时间维可组合性"。</p>
 */
@FunctionalInterface
public interface Disposable {

    /** 释放该效果占用的一切资源 / 撤销其对上下文的全部修改。 */
    void dispose() throws Exception;

    Disposable NOOP = () -> { };

    /**
     * LIFO 组合：返回的 Disposable 被调用时，先执行 {@code after}，再执行 {@code before}。
     * 与论文 Algorithm 1 中 inverse ← value ∘ inverse（新逆操作前插）保持一致。
     */
    static Disposable compose(Disposable newer, Disposable older) {
        return () -> {
            // newer 是后登记的效果，必须先回滚
            safeDispose(newer);
            safeDispose(older);
        };
    }

    static void safeDispose(Disposable d) {
        if (d == null) return;
        try {
            d.dispose();
        } catch (Exception e) {
            // 单个逆操作失败不应阻断其余清理；记录并继续，保证"有序回收"尽力而为
            System.err.println("[Cordis] inverse failed: " + e.getMessage());
        }
    }
}
