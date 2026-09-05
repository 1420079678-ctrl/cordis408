package io.cordis408.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 大模型网关：统一封装"检索增强生成"。
 *
 * <p>{@link #complete} 在未配置或调用失败时返回 {@code null}，由上层 Agent 自动回退到
 * 内置确定性讲解，保证系统在离线/无 Key/服务故障时仍可完整作答（优雅降级）。
 * 协议采用 OpenAI Chat Completions 兼容格式，DeepSeek、vLLM、Ollama、各类网关均可直接对接。</p>
 */
@Component
public class LlmGateway {

    private static final Logger log = LoggerFactory.getLogger(LlmGateway.class);
    private final LlmProperties props;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public LlmGateway(LlmProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public boolean enabled() { return props.isEnabled(); }
    public String model() { return props.getModel(); }

    /** 以 system/user 双消息请求一次补全；任何不可用情况返回 null。 */
    public String complete(String systemPrompt, String userPrompt) {
        if (!props.isEnabled()) return null;
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", props.getModel());
            body.put("temperature", props.getTemperature());
            body.put("max_tokens", props.getMaxTokens());
            body.put("stream", false);
            ArrayNode msgs = body.putArray("messages");
            msgs.addObject().put("role", "system").put("content", systemPrompt);
            msgs.addObject().put("role", "user").put("content", userPrompt);

            String url = normalizeUrl(props.getBaseUrl());
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("LLM 调用失败 status={}, body={}", resp.statusCode(),
                        resp.body() == null ? "" : resp.body().substring(0, Math.min(300, resp.body().length())));
                return null;
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() || content.isNull() ? null : content.asText();
        } catch (Exception e) {
            log.warn("LLM 调用异常，降级为内置引擎: {}", e.getMessage());
            return null;
        }
    }

    private String normalizeUrl(String base) {
        String b = base.trim();
        if (b.endsWith("/chat/completions")) return b;
        if (b.endsWith("/v1")) return b + "/chat/completions";
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b + "/v1/chat/completions";
    }
}
