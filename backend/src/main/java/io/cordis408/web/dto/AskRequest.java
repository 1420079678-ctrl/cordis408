package io.cordis408.web.dto;

import jakarta.validation.constraints.NotBlank;

/** 统一问答请求。 */
public record AskRequest(
        String sessionId,
        @NotBlank String message,
        String mode,        // 可空，由路由判定；可强制 ask/tutor/quiz/plan
        String subject,     // 可空，可强制 ds/co/cn/os
        Integer count,
        Integer difficulty
) {
    public String safeSession() { return sessionId == null || sessionId.isBlank() ? "default" : sessionId; }
}
