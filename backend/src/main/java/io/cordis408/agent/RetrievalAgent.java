package io.cordis408.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.cordis408.knowledge.KnowledgeBase;
import io.cordis408.knowledge.KnowledgePoint;
import io.cordis408.knowledge.QuizItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 知识检索 Agent（RAG Retriever）。
 *
 * <p>提供协效果 {@code knowledge.base}：在本地 408 知识库中做关键词检索并拼装证据文本，
 * 供专家 Agent 基于证据作答，降低幻觉。它是四个学科专家共同依赖的空间维上游。</p>
 */
@Component
public class RetrievalAgent extends AbstractAgent {

    public static final String KEY = "knowledge.base";
    private final KnowledgeBase kb;
    private final Random random = new Random();

    public RetrievalAgent(KnowledgeBase kb) {
        super("retrieval");
        this.kb = kb;
    }

    @Override public String agentName() { return "知识检索 Agent"; }
    @Override protected String provideKey(JsonNode c) { return KEY; }

    public KnowledgeBase kb() { return kb; }

    /** 检索并把证据写回工作上下文。 */
    public void retrieve(WorkContext w, int topK) {
        long t = System.currentTimeMillis();
        List<KnowledgeBase.Scored> hits = kb.search(w.subject, w.rawQuestion, topK);
        // 若限定学科无果，放宽到全学科
        if (hits.isEmpty() && w.subject != null) hits = kb.search(null, w.rawQuestion, topK);
        w.retrieved = hits;
        StringBuilder sb = new StringBuilder();
        for (KnowledgeBase.Scored s : hits) {
            KnowledgePoint p = s.point();
            sb.append("【").append(Subjects.name(p.subject())).append("·").append(p.chapter())
              .append("】").append(p.title()).append("\n");
            sb.append("- 速记：").append(p.summary()).append("\n");
            sb.append("- 详解：").append(p.detail()).append("\n");
            if (p.keyPoints() != null)
                p.keyPoints().forEach(k -> sb.append("- 要点：").append(k).append("\n"));
            sb.append("\n");
        }
        w.evidenceText = sb.toString().trim();
        w.trace(type(), agentName(), "检索Top" + topK + "命中" + hits.size() + "个考点",
                hits.isEmpty() ? "无直接命中，走通识回答" : hits.get(0).point().title(), t);
    }

    public List<QuizItem> pick(WorkContext w, String chapter, Integer difficulty, int n) {
        return kb.pickQuizzes(w.subject, chapter, difficulty, n, random);
    }
}
