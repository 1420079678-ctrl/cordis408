package io.cordis408.knowledge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 408 练习题。
 *
 * @param id         题号
 * @param subject    学科代码
 * @param chapter    章名
 * @param difficulty 难度 1-5
 * @param type       题型：single（单选）/ comprehensive（综合）
 * @param stem       题干
 * @param options    选项（单选时使用，key 为 A/B/C/D）
 * @param answer     答案（单选为选项 key；综合题为参考答案）
 * @param analysis   解析
 * @param kpIds      关联知识点
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizItem(
        String id,
        String subject,
        String chapter,
        int difficulty,
        String type,
        String stem,
        java.util.Map<String, String> options,
        String answer,
        String analysis,
        List<String> kpIds
) { }
