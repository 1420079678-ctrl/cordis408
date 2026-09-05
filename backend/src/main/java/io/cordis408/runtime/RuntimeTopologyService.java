package io.cordis408.runtime;

import io.cordis408.runtime.loader.AgentLoader;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 把运行时 Fiber 拓扑（状态 + 协效果依赖边）组织成前端可渲染的结构，
 * 直观展示"空间维可组合"：谁向谁提供能力、谁在等待谁、谁当前被停用。
 */
@Service
public class RuntimeTopologyService {

    private final CordisRuntime runtime;
    private final AgentLoader loader;

    public RuntimeTopologyService(CordisRuntime runtime, AgentLoader loader) {
        this.runtime = runtime;
        this.loader = loader;
    }

    public Map<String, Object> snapshot() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        // key -> provider entryId
        Map<String, String> keyProvider = new HashMap<>();

        for (Fiber f : loader.liveFibers()) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("uid", f.uid);
            n.put("entryId", f.entryId);
            n.put("type", f.type());
            n.put("name", f.displayName());
            n.put("state", f.state.name());
            n.put("inject", new ArrayList<>(f.injectKeys));
            n.put("provide", new ArrayList<>(f.provideKeys));
            n.put("disabled", f.disabled);
            n.put("activateDurationMs", f.activateDurationMs);
            n.put("lastTransitionAt", f.lastTransitionAt);
            n.put("error", f.lastError == null ? null : f.lastError.getMessage());
            nodes.add(n);
            for (String k : f.provideKeys) keyProvider.put(k, f.entryId);
        }

        // 依赖边：provider -> dependent
        List<Map<String, String>> edges = new ArrayList<>();
        Set<String> edgeSeen = new HashSet<>();
        for (Fiber f : loader.liveFibers()) {
            for (String need : f.injectKeys) {
                String provider = keyProvider.get(need);
                if (provider == null) continue;
                String sig = provider + "->" + f.entryId + ":" + need;
                if (edgeSeen.add(sig)) {
                    Map<String, String> e = new LinkedHashMap<>();
                    e.put("from", provider);
                    e.put("to", f.entryId);
                    e.put("key", need);
                    edges.add(e);
                }
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nodes", nodes);
        out.put("edges", edges);
        Map<String, Long> summary = new LinkedHashMap<>();
        for (FiberState s : FiberState.values())
            summary.put(s.name(), loader.countByState(s));
        out.put("summary", summary);
        out.put("llmEnabled", false); // 由 controller 覆盖
        return out;
    }
}
