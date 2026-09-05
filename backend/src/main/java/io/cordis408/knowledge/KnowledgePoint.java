package io.cordis408.knowledge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 408 知识点（考纲条目）。
 *
 * @param id         全局唯一编号，如 DS-0203
 * @param subject    学科代码：ds / co / cn / os
 * @param chapter    章名
 * @param title      考点标题
 * @param frequency  考频：high / mid / low
 * @param summary    一句话结论（速记）
 * @param detail     成段讲解
 * @param keyPoints  关键要点 / 易错点
 * @param related    关联知识点 id
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KnowledgePoint(
        String id,
        String subject,
        String chapter,
        String title,
        String frequency,
        String summary,
        String detail,
        List<String> keyPoints,
        List<String> related
) { }
