package io.cordis408.web;

import io.cordis408.llm.LlmGateway;
import io.cordis408.runtime.loader.AgentLoader;
import io.cordis408.runtime.RuntimeTopologyService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 运行时治理接口：实时查看 Fiber 拓扑、动态启停组件（时间/空间可组合演示）、
 * 事务性热替换（HMR）。这些操作无需重启进程、不中断其它组件与进行中的会话。
 */
@RestController
@RequestMapping("/api/runtime")
@CrossOrigin
public class RuntimeAdminController {

    private final RuntimeTopologyService topology;
    private final AgentLoader loader;
    private final LlmGateway llm;

    public RuntimeAdminController(RuntimeTopologyService topology, AgentLoader loader, LlmGateway llm) {
        this.topology = topology;
        this.loader = loader;
        this.llm = llm;
    }

    /** 当前 Agent 拓扑（节点状态 + 协效果依赖边）。 */
    @GetMapping("/topology")
    public Map<String, Object> topology() {
        Map<String, Object> snap = topology.snapshot();
        snap.put("llmEnabled", llm.enabled());
        snap.put("model", llm.model());
        return snap;
    }

    /** 动态启用/停用某个组件条目；停用会级联停用其依赖者，重新启用会级联唤醒。 */
    @PostMapping("/entries/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable String id, @RequestParam boolean disabled) {
        loader.setDisabled(id, disabled);
        return Map.of("ok", true, "id", id, "disabled", disabled, "topology", topology.snapshot());
    }

    /** 事务性热替换某类组件（失败自动回滚，会话不中断）。 */
    @PostMapping("/hot-reload/{type}")
    public Map<String, Object> hotReload(@PathVariable String type) {
        AgentLoader.HotReloadReport report = loader.hotReload(type);
        return Map.of("ok", true, "report", report, "topology", topology.snapshot());
    }
}
