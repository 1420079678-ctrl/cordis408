package io.cordis408.runtime;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cordis 运行时容器 —— 论文 §5.1 "Core Library" 的 Java 落地。
 *
 * <p>持有全局一等上下文树根、协效果值存储（@@store）、provider 索引，以及一条
 * <b>单线程转换循环</b>（transition loop）。论文 §4.3.3 处理了异步交错，这里用
 * actor 式单线程把所有生命周期转换串行化，在等价保证（无竞争、可收敛到静止态
 * Theorem 66）的同时极大简化工程实现。</p>
 */
@org.springframework.stereotype.Component
public class CordisRuntime {
    // Spring 托管为单例 Bean（注解用全限定名以区别本包的 Component 接口）

    public static final String GLOBAL_REALM = "__global__";

    /** 一个协效果绑定：值 + 提供它的 Fiber（provider）。 */
    public record Binding(Object value, long providerUid) { }

    private final AtomicLong uidGen = new AtomicLong(1);
    final Map<Long, Fiber> fibers = new LinkedHashMap<>();
    /** @@store：realm#key -> Binding（两层解析 k→ρ(k)→σ(ρ(k)) 的存储层）。 */
    final Map<String, Binding> bindings = new ConcurrentHashMap<>();

    private volatile Thread loopThread;
    private final java.util.concurrent.ExecutorService loop =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "cordis-transition");
                t.setDaemon(true);
                this.loopThread = t;
                return t;
            });

    public final RuntimeContext root = new RuntimeContext(this, null);

    long nextUid() { return uidGen.getAndIncrement(); }

    /**
     * 把一次状态转换提交到串行循环并等待其收敛（调用方因此能观察到 quiescent 静止态）。
     * 可重入：若已在转换线程上（级联 refresh / 事务热替换内部），直接执行以避免自死锁。
     */
    public void transition(Runnable action) {
        if (Thread.currentThread() == loopThread) {
            action.run();
            return;
        }
        try {
            Future<?> f = loop.submit(action);
            f.get();
        } catch (Exception e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            if (c instanceof RuntimeException re) throw re;
            throw new RuntimeException(c);
        }
    }

    /** ctx.use：在指定父上下文上实例化一个组件为 Fiber，并立即尝试 refresh。 */
    public Fiber use(RuntimeContext parent, Component component, JsonNode config, String entryId) {
        Fiber fiber = new Fiber(this, parent, component, config, entryId);
        transition(() -> {
            fibers.put(fiber.uid, fiber);
            fiber.refresh();
        });
        return fiber;
    }

    /** 移除 Fiber（O-Remove）。 */
    void removeFiber(Fiber f) {
        transition(() -> {
            f.forceUnload();
            fibers.remove(f.uid);
        });
    }

    /** 从注册表摘除 Fiber；调用方须已在 transition 线程内完成卸载（供 loader 使用）。 */
    public void deregisterFiber(long uid) { fibers.remove(uid); }

    // ===== 协效果存储（@@store），realm 由 RuntimeContext 解析 =====
    String storeKey(String realm, String key) { return realm + "#" + key; }

    void putBinding(String realm, String key, Object value, long providerUid) {
        bindings.put(storeKey(realm, key), new Binding(value, providerUid));
    }

    void deleteBinding(String realm, String key) {
        bindings.remove(storeKey(realm, key));
    }

    /** 读取某 key 在给定 realm 下的绑定（get：ρ→σ 两层解析的终点）。 */
    public Binding resolve(String realm, String key) {
        return bindings.get(storeKey(realm, key));
    }

    /** 该 key 当前是否存在一个 <b>ACTIVE</b> 的 provider（provided-by 关系，Definition 46）。 */
    public boolean isProvided(String realm, String key) {
        Binding b = bindings.get(storeKey(realm, key));
        if (b == null) return false;
        Fiber p = fibers.get(b.providerUid());
        return p != null && p.state == FiberState.ACTIVE;
    }

    /**
     * 响应式通知（Algorithm 3）：上下文 key 发生变化，遍历所有存活 Fiber，
     * 若变化 key 落在其 inject 规格内且解析到同一 realm，则触发 refresh 重新评估。
     */
    void notify(Set<String> changedKeys, RuntimeContext origin) {
        // 复制，避免级联 refresh 改变集合。注意：不能跳过 INACTIVE 且 target=false 的 fiber——
        // 它们正是"因依赖缺失而等待"者，provider 恢复时必须被重新评估唤醒（refresh 内部自带幂等）。
        for (Fiber f : new ArrayList<>(fibers.values())) {
            for (String key : changedKeys) {
                if (f.injectKeys.contains(key)) {
                    String realm = f.ctx.resolveRealm(key);
                    String originRealm = origin.resolveRealm(key);
                    if (Objects.equals(realm, originRealm)) {
                        f.refresh();
                    }
                    break;
                }
            }
        }
    }

    public Collection<Fiber> allFibers() { return Collections.unmodifiableCollection(fibers.values()); }

    public Fiber fiber(long uid) { return fibers.get(uid); }

    public void shutdown() { loop.shutdownNow(); }
}
