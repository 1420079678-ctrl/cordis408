package io.cordis408.runtime.loader;

import io.cordis408.runtime.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 组件目录：type → 无状态组件定义。Spring 自动注入所有 {@link Component} Bean。
 *
 * <p>组件定义是无状态的，同一定义可用不同 config 实例化为多个 Fiber
 * （例如 SubjectExpertAgent 一个定义，按 config.subject 实例化为数据结构 / 组成原理 /
 * 计算机网络 / 操作系统四个专家 Fiber）。</p>
 */
@org.springframework.stereotype.Component
public class ComponentRegistry {

    private final Map<String, Component> byType = new LinkedHashMap<>();

    public ComponentRegistry(List<Component> components) {
        for (Component c : components) byType.put(c.type(), c);
    }

    public Component require(String type) {
        Component c = byType.get(type);
        if (c == null) throw new IllegalArgumentException("未注册的组件类型: " + type);
        return c;
    }

    public boolean exists(String type) { return byType.containsKey(type); }

    public Set<String> types() { return byType.keySet(); }
}
