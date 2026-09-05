package io.cordis408.web.dto;

/** 单题批改请求。 */
public record GradeRequest(String sessionId, String quizId, String answer) {
    public String safeSession() { return sessionId == null || sessionId.isBlank() ? "default" : sessionId; }
}
