package io.cordis408.runtime.loader;

import io.cordis408.runtime.Component;
import io.cordis408.runtime.CordisRuntime;
import io.cordis408.runtime.Fiber;
import io.cordis408.runtime.FiberState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 声明式组件加载器（论文 §5.2 Component Loader）。
 *
 * <p>编排者给出一份"期望配置"（entries 列表），加载器把它增量对账（reconciliation）
 * 为运行中的 Fiber 集合，并保持二者同步：</p>
 * <ul>
 *   <li>新增 id → 实例化；消失 id → 卸载（Corollary 62：撤离者不留痕迹）；</li>
 *   <li>type 变化 → 重建；config 变化 → 交给组件最小重载；disabled 变化 → 装载/卸载；</li>
 *   <li>无需关心加载顺序：依赖未满足的 Fiber 停在 INACTIVE 等待，provider 出现后由
 *       响应式协效果级联唤醒（Theorem 63 / Theorem 73：静止态只由最终配置决定）。</li>
 * </ul>
 *
 * <p>{@link #hotReload} 实现论文 §5.2.2 的事务性热替换：全部成功才提交，任一失败
 * 回滚到备份，系统永远不会进入"半装载"状态。</p>
 */
@org.springframework.stereotype.Component
public class AgentLoader {

    private static final Logger log = LoggerFactory.getLogger(AgentLoader.class);

    private final CordisRuntime runtime;
    private final ComponentRegistry registry;

    /** 期望配置（权威记录，authoritative record）。 */
    private final LinkedHashMap<String, ComponentEntry> entries = new LinkedHashMap<>();
    /** entryId -> 运行 Fiber。 */
    private final LinkedHashMap<String, Fiber> fibers = new LinkedHashMap<>();

    public AgentLoader(CordisRuntime runtime, ComponentRegistry registry) {
        this.runtime = runtime;
        this.registry = registry;
    }

    /** 以一份全新期望配置进行对账（首次装配或整体替换）。 */
    public synchronized void reconcile(List<ComponentEntry> desired) {
        runtime.transition(() -> doReconcile(desired));
    }

    private void doReconcile(List<ComponentEntry> desired) {
        Set<String> nextIds = new LinkedHashSet<>();
        for (ComponentEntry e : desired) {
            nextIds.add(e.id());
            ComponentEntry old = entries.get(e.id());
            if (old == null) {
                instantiate(e);
            } else {
                diff(old, e);
            }
            entries.put(e.id(), e);
        }
        // 移除被删除的条目
        for (String id : new ArrayList<>(entries.keySet())) {
            if (!nextIds.contains(id)) retire(id);
        }
    }

    /** 仅切换某个 entry 的禁用状态（行政开关）。 */
    public synchronized void setDisabled(String id, boolean disabled) {
        ComponentEntry e = entries.get(id);
        if (e == null) throw new NoSuchElementException("无此组件条目: " + id);
        entries.put(id, e.withDisabled(disabled));
        runtime.transition(() -> {
            Fiber f = fibers.get(id);
            if (f != null) f.setDisabled(disabled);
        });
    }

    /** 更新某个 entry 的配置。 */
    public synchronized void updateConfig(String id, com.fasterxml.jackson.databind.JsonNode config) {
        ComponentEntry e = entries.get(id);
        if (e == null) throw new NoSuchElementException("无此组件条目: " + id);
        entries.put(id, e.withConfig(config));
        runtime.transition(() -> {
            Fiber f = fibers.get(id);
            if (f != null) f.updateConfig(config);
        });
    }

    private void instantiate(ComponentEntry e) {
        Component comp = registry.require(e.type());
        Fiber f = runtime.use(runtime.root, comp, e.config(), e.id());
        if (e.disabled()) f.setDisabled(true);
        fibers.put(e.id(), f);
        log.info("mount entry '{}' (type={}, state={})", e.id(), e.type(), f.state);
    }

    private void diff(ComponentEntry old, ComponentEntry now) {
        Fiber f = fibers.get(now.id());
        if (!old.type().equals(now.type())) {
            retire(now.id());
            instantiate(now);
            return;
        }
        if (old.disabled() != now.disabled()) f.setDisabled(now.disabled());
        if (!Objects.equals(old.config(), now.config())) f.updateConfig(now.config());
    }

    private void retire(String id) {
        Fiber f = fibers.remove(id);
        if (f != null) {
            f.forceUnload();
            removeFromRuntime(f);
        }
        entries.remove(id);
        log.info("retire entry '{}'", id);
    }

    private void removeFromRuntime(Fiber f) {
        runtime.deregisterFiber(f.uid);
    }

    /**
     * 事务性热替换某 type 的全部 Fiber（论文 Algorithm 10）。
     * 备份 → 卸载重建到暂存区 → 全部成功才提交；任一 FAILED/异常则丢弃暂存、按原配置回滚，
     * 保证系统永远不会停留在"半装载"状态。
     */
    public synchronized HotReloadReport hotReload(String type) {
        final List<String> targets = fibers.values().stream()
                .filter(f -> f.type().equals(type)).map(f -> f.entryId).toList();
        if (targets.isEmpty()) return new HotReloadReport(type, List.of(), "无运行中的该类型组件");

        final String[] note = new String[]{""};
        runtime.transition(() -> {
            LinkedHashMap<String, Fiber> staging = new LinkedHashMap<>();
            try {
                for (String id : targets) {
                    Fiber oldF = fibers.get(id);
                    oldF.forceUnload();
                    runtime.deregisterFiber(oldF.uid);
                    ComponentEntry e = entries.get(id);
                    Fiber nf = runtime.use(runtime.root, registry.require(type), e.config(), id);
                    if (e.disabled()) nf.setDisabled(true);
                    if (nf.state == FiberState.FAILED)
                        throw new IllegalStateException("组件 " + id + " 重建失败: "
                                + (nf.lastError == null ? "" : nf.lastError.getMessage()));
                    staging.put(id, nf);
                }
                fibers.putAll(staging); // 提交
                note[0] = "热替换成功，共 " + staging.size() + " 个实例，会话不中断";
            } catch (RuntimeException ex) {
                staging.values().forEach(v -> { v.forceUnload(); runtime.deregisterFiber(v.uid); });
                // 回滚到热替换前的等价静止态（Theorem 73：静止态只由配置决定）
                for (String id : targets) {
                    ComponentEntry e = entries.get(id);
                    Fiber rf = runtime.use(runtime.root, registry.require(e.type()), e.config(), id);
                    if (e.disabled()) rf.setDisabled(true);
                    fibers.put(id, rf);
                }
                note[0] = "热替换失败，已事务回滚: " + ex.getMessage();
                throw ex;
            }
        });
        return new HotReloadReport(type, targets, note[0]);
    }

    public record HotReloadReport(String type, List<String> affectedEntries, String message) { }

    // ===== 只读视图 =====
    public Collection<Fiber> liveFibers() { return fibers.values(); }
    public Map<String, ComponentEntry> entries() { return Collections.unmodifiableMap(entries); }

    public Fiber fiberOfEntry(String id) { return fibers.get(id); }

    public long countByState(FiberState s) {
        return fibers.values().stream().filter(f -> f.state == s).count();
    }
}
