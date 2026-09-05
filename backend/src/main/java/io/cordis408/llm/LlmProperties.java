package io.cordis408.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型接入配置（application.yml 中以 llm.* 配置）。
 *
 * <p>默认 enabled=false：系统完全依赖内置 408 知识库 + 模板化多 Agent 推理即可运行。
 * 填入任意 OpenAI 兼容服务（DeepSeek / OpenAI / 本地 vLLM 等）的 base-url、api-key、model
 * 并将 enabled 置 true 后，专家 Agent 的讲解将由大模型基于检索证据生成。</p>
 */
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** 是否启用外部大模型增强；关闭时走内置确定性教学引擎。 */
    private boolean enabled = false;
    /** 提供商标识，仅用于展示。 */
    private String provider = "builtin";
    /** OpenAI 兼容地址，如 https://api.deepseek.com（路径 /chat/completions 自动补全）。 */
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private double temperature = 0.3;
    private int maxTokens = 1600;
    private int timeoutSeconds = 60;

    public boolean isEnabled() { return enabled && apiKey != null && !apiKey.isBlank(); }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
