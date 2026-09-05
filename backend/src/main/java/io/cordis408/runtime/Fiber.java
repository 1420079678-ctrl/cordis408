package io.cordis408.runtime;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

/**
 * Fiber —— 组件在运行时的一次实例化（论文 §4.1 Components and Fibers），
 * 同时是一台"惯性状态机"（§4.3.3 / Algorithm 4、5）。
 *
 * <p>{@link #target} 是由"依赖是否满足 + 是否被禁用"推导出的期望状态；{@link #state}
 * 是当前实际状态。refresh 只在二者不一致时才发起一次转换（inertia），因此 neutral
 * 变化天然幂等无害（Definition 26 的 activating/deactivating/neutral 三分类）。</p>
 */
public class Fiber {

    public final long uid;
    public final Component component;
    public final RuntimeContext parentCtx;
    public final RuntimeContext ctx;
    public final String entryId;

    public JsonNode config;
    public final Set<String> injectKeys;
    public final Set<String> provideKeys;

    public volatile FiberState state = FiberState.INACTIVE;
    /** 期望是否应处于激活态（⊥ = false）。 */
    boolean target = false;
    boolean disabled = false;
    private Disposable committed = Disposable.NOOP;

    public long lastTransitionAt = System.currentTimeMillis();
    public long activateDurationMs = -1;
    public Throwable lastError;

    Fiber(CordisRuntime runtime, RuntimeContext parent, Component component, JsonNode config, String entryId) {
        this.uid = runtime.nextUid();
        this.component = component;
        this.parentCtx = parent;
        this.config = config;
        this.entryId = entryId;
        this.injectKeys = Set.copyOf(component.inject(config));
        this.provideKeys = Set.copyOf(component.provide(config));
        this.ctx = parent.derive(this);
    }

    public String type() { return component.type(); }
    public String displayName() { return component.displayName(config); }

    /** 依赖是否全部被 ACTIVE provider 满足（provided-by，Definition 46）。 */
    boolean dependenciesSatisfied() {
        for (String key : injectKeys) {
            if (!runtime().isProvided(ctx.resolveRealm(key), key)) return false;
        }
        return true;
    }

    private CordisRuntime runtime() { return ctx.runtime; }

    /** Algorithm 5 refresh：重算 target，仅在期望变化时发起转换（neutral 变化幂等）。 */
    void refresh() {
        boolean desired = !disabled && dependenciesSatisfied();
        if (desired == this.target && state != FiberState.FAILED) return;
        this.target = desired;
        if (desired) reload();
        else beginUnload();
    }

    /** loader 改变 disabled 后调用。 */
    public void setDisabled(boolean disabled) {
        if (this.disabled == disabled) return;
        this.disabled = disabled;
        runtime().transition(this::refresh);
    }

    /** 配置变更：交给组件，这里直接以新配置重新走一遍生命周期（最小扰动由 loader 决定是否调用）。 */
    public void updateConfig(JsonNode config) {
        runtime().transition(() -> {
            this.config = config;
            if (state == FiberState.ACTIVE || state == FiberState.FAILED) {
                beginUnload();
                reload();
            }
        });
    }

    /** 重新加载（LOADING → ACTIVE / FAILED）。 */
    private void reload() {
        long t0 = System.currentTimeMillis();
        state = FiberState.LOADING;
        lastError = null;
        lastTransitionAt = t0;
        try {
            Disposable d = component.apply(ctx, config);
            this.committed = d != null ? d : Disposable.NOOP;
            this.activateDurationMs = System.currentTimeMillis() - t0;
            this.state = FiberState.ACTIVE;
            this.lastTransitionAt = System.currentTimeMillis();
            // 宣布本组件 provide 的 key 可用，唤醒正在等待的依赖者（级联激活，Theorem 63）
            runtime().notify(provideKeys, ctx);
        } catch (Exception e) {
            this.lastError = e;
            this.state = FiberState.FAILED;
            this.target = false;
            this.lastTransitionAt = System.currentTimeMillis();
            System.err.println("[Cordis] fiber " + type() + " failed: " + e);
        }
    }

    /** 卸载（UNLOADING → INACTIVE），含"撤离提前可见"的依赖级联。 */
    private void beginUnload() {
        if (state == FiberState.INACTIVE) return;
        state = FiberState.UNLOADING;
        lastTransitionAt = System.currentTimeMillis();
        // 关键（§5.1.2 withdrawal）：此刻 state 已非 ACTIVE，isProvided 立即返回 false，
        // 但 binding 尚未删除；先 notify，让依赖者在我们的值仍在时同步开始各自 teardown。
        runtime().notify(provideKeys, ctx);
        // 依赖者级联卸载完成后，再 LIFO 回滚本组件的全部效果（删除 binding、注销资源等）
        ctx.disposeAll();
        this.committed = Disposable.NOOP;
        this.state = FiberState.INACTIVE;
        this.lastTransitionAt = System.currentTimeMillis();
    }

    /** 强制卸载（移除 entry 时）。 */
    public void forceUnload() {
        this.target = false;
        beginUnload();
    }
}
