package io.cordis408.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import io.cordis408.runtime.Fiber;
import io.cordis408.runtime.FiberState;
import io.cordis408.runtime.loader.AgentLoader;
import io.cordis408.runtime.loader.ComponentEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 启动时读取声明式配置 agents.yml，并通过加载器对账（reconcile）为运行中的 Fiber 拓扑。
 * 由于响应式协效果，书写顺序不影响最终静止态：依赖未满足者先等待，provider 激活后级联唤醒。
 */
@Component
public class RuntimeBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RuntimeBootstrap.class);

    private final AgentLoader loader;
    private final ObjectMapper mapper;

    public RuntimeBootstrap(AgentLoader loader, ObjectMapper mapper) {
        this.loader = loader;
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run(String... args) throws Exception {
        try (var is = new ClassPathResource("agents.yml").getInputStream()) {
            Map<String, Object> root = new Yaml().load(is);
            List<Map<String, Object>> comps = (List<Map<String, Object>>) root.get("components");
            List<ComponentEntry> entries = new ArrayList<>();
            for (Map<String, Object> c : comps) {
                String id = (String) c.get("id");
                String type = (String) c.get("type");
                Object cfg = c.get("config");
                JsonNode config = cfg == null ? NullNode.getInstance()
                        : mapper.convertValue(cfg, JsonNode.class);
                boolean disabled = Boolean.TRUE.equals(c.get("disabled"));
                String isolate = (String) c.get("isolate");
                String intercept = (String) c.get("intercept");
                entries.add(new ComponentEntry(id, type, config, disabled, isolate, intercept));
            }
            loader.reconcile(entries);
        }

        long active = loader.liveFibers().stream().filter(f -> f.state == FiberState.ACTIVE).count();
        log.info("======== Cordis408 Agent 拓扑装配完成：{}/{} 个 Fiber 处于 ACTIVE ========",
                active, loader.liveFibers().size());
        for (Fiber f : loader.liveFibers()) {
            log.info("  [{}] {} ({}) inject={} provide={}",
                    f.state, f.entryId, f.type(), f.injectKeys, f.provideKeys);
        }
    }
}
