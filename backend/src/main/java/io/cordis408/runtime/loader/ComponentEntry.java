package io.cordis408.runtime.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 声明式配置条目（论文 Definition 74 Entry）。
 *
 * <p>编排者只需要声明"期望的系统组成"，加载器负责把它实现为 Fiber 并持续保持同步，
 * 双向绑定：改条目 → 调整 Fiber；组件自禁用/自改配置 → 回写条目。</p>
 */
public record ComponentEntry(
        String id,            // 稳定标识，reconcile 时作为 diff key
        String type,          // 组件模块（对应论文 url，这里是 ComponentRegistry 中的 type）
        JsonNode config,      // 绑定进组件形成 apply 的配置
        boolean disabled,     // 是否被行政关闭（对应 τ）
        String isolate,       // 隔离注解（可选）：true=私有 realm；字符串=命名共享 realm
        String intercept      // 拦截注解（可选）
) {
    public static ComponentEntry of(String id, String type, JsonNode config) {
        return new ComponentEntry(id, type, config == null ? ObjectNodeBuilder.get() : config, false, null, null);
    }

    public ComponentEntry withDisabled(boolean d) {
        return new ComponentEntry(id, type, config, d, isolate, intercept);
    }

    public ComponentEntry withConfig(JsonNode c) {
        return new ComponentEntry(id, type, c, disabled, isolate, intercept);
    }

    static final class ObjectNodeBuilder {
        static JsonNode get() { return JsonNodeFactory.instance.objectNode(); }
    }
}
