package io.cordis408.runtime;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 一等上下文（论文 §3.3 Unified Context，形式化 Γ∞）。
 *
 * <p>统一了"效果上下文"与"协效果上下文"，并以父子链接构成<b>上下文树</b>。
 * 组件在自己的派生子上下文里运行，因此它造成的一切效果都被该上下文边界圈定，
 * 卸载时整棵子树随之回收 —— 时间维可组合。</p>
 *
 * <p>三张符号表对应论文 §5.1.2：</p>
 * <ul>
 *   <li>{@code isolate}（@@isolate / ρ）：coeffect key → realm，支持隔离；</li>
 *   <li>值存储落在 {@link CordisRuntime#bindings}（@@store / σ）；</li>
 *   <li>{@code intercept}（@@intercept / ι）：访问某 key 时叠加的元数据。</li>
 * </ul>
 */
public class RuntimeContext {

    final CordisRuntime runtime;
    final RuntimeContext parent;
    /** ρ：本层对 key 的 realm 重定向（isolate）。 */
    private final Map<String, String> isolate = new HashMap<>();
    /** ι：本层对 key 的拦截元数据（intercept）。 */
    private final Map<String, JsonNode> intercept = new HashMap<>();
    /** 本上下文累积的逆操作（accumulator g），卸载时 LIFO 回滚。 */
    Disposable disposeChain = Disposable.NOOP;
    /** 拥有该上下文的 Fiber（根上下文为 null）。 */
    Fiber owner;

    RuntimeContext(CordisRuntime runtime, RuntimeContext parent) {
        this.runtime = runtime;
        this.parent = parent;
    }

    /** 派生一个子上下文（组件实例化时使用）。 */
    public RuntimeContext derive(Fiber owner) {
        RuntimeContext c = new RuntimeContext(runtime, this);
        c.owner = owner;
        return c;
    }

    // ===== 时间维：唯一的变更原语 ctx.effect（Algorithm 1）=====

    /**
     * 效果原语：执行 body，body 返回它的逆操作；逆操作被登记到本上下文，
     * 卸载时自动 LIFO 执行。<b>任何</b>对共享环境的修改都必须经由它，才能被追踪与回收。
     */
    public Disposable effect(EffectBody body) throws Exception {
        Disposable raw = body.run();
        final Disposable inverse = raw == null ? Disposable.NOOP : raw;
        final Disposable accumulated = this.disposeChain;
        // 新逆操作前插：后申请者先释放
        this.disposeChain = () -> {
            Disposable.safeDispose(inverse);
            Disposable.safeDispose(accumulated);
        };
        return inverse;
    }

    /** 效果体：执行变更并返回其逆操作。 */
    @FunctionalInterface
    public interface EffectBody { Disposable run() throws Exception; }

    // ===== 空间维：协效果 get/set/provide（Algorithm 2）=====

    /** ρ(k)：沿上下文树解析 key 所属 realm。 */
    String resolveRealm(String key) {
        for (RuntimeContext c = this; c != null; c = c.parent) {
            String r = c.isolate.get(key);
            if (r != null) return r;
        }
        return CordisRuntime.GLOBAL_REALM;
    }

    /** get(k)：两层解析 k→ρ(k)→σ(ρ(k))，沿父链向上查找。 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        String realm = resolveRealm(key);
        CordisRuntime.Binding b = runtime.resolve(realm, key);
        return b == null ? null : (T) b.value();
    }

    /** set(k,v)：绑定本身就是一次可回滚效果，安装/移除都会 notify 依赖者。 */
    public Disposable set(String key, Object value) throws Exception {
        String realm = resolveRealm(key);
        long provider = owner != null ? owner.uid : -1L;
        CordisRuntime.Binding previous = runtime.resolve(realm, key);
        return effect(() -> {
            runtime.putBinding(realm, key, value, provider);
            runtime.notify(Set.of(key), this);
            return () -> {
                if (previous == null) runtime.deleteBinding(realm, key);
                else runtime.putBinding(realm, key, previous.value(), previous.providerUid());
                runtime.notify(Set.of(key), this);
            };
        });
    }

    /** isolate(k, realm)：派生一个把 key 重定向到独立 realm 的子上下文（隔离，丢弃子上下文即回收）。 */
    public RuntimeContext isolate(String key, String realm) {
        RuntimeContext child = new RuntimeContext(runtime, this);
        child.isolate.put(key, realm);
        if (owner != null) child.owner = owner;
        return child;
    }

    /** intercept(k, meta)：在本层叠加访问元数据（读取时生效，无需重载）。 */
    public void intercept(String key, JsonNode meta) { intercept.put(key, meta); }

    public JsonNode interceptionOf(String key) {
        for (RuntimeContext c = this; c != null; c = c.parent) {
            JsonNode n = c.intercept.get(key);
            if (n != null) return n;
        }
        return null;
    }

    /** 卸载本上下文：LIFO 执行全部累积逆操作。 */
    void disposeAll() {
        Disposable d = this.disposeChain;
        this.disposeChain = Disposable.NOOP;
        Disposable.safeDispose(d);
    }

    Set<String> ownedIsolationKeys() { return new HashSet<>(isolate.keySet()); }
}
