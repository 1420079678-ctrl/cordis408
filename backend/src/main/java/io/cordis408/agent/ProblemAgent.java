package io.cordis408.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.cordis408.knowledge.KnowledgeBase;
import io.cordis408.knowledge.QuizItem;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 出题 Agent：按学科 / 章节 / 难度组卷，优先围绕刚检索到的薄弱考点出题。
 * provide {@code problem}，依赖 knowledge.base 与 learner.state。
 */
@Component
public class ProblemAgent extends AbstractAgent {

    public static final String KEY = "problem";
    private final KnowledgeBase kb;
    private final Random rnd = new Random();

    public ProblemAgent(KnowledgeBase kb) { super("problem"); this.kb = kb; }

    @Override public String agentName() { return "智能出题 Agent"; }
    @Override protected String provideKey(JsonNode c) { return KEY; }
    @Override protected Set<String> requireKeys(JsonNode c) {
        return Set.of(RetrievalAgent.KEY, LearnerModelAgent.KEY);
    }

    /** 组卷：优先命中检索考点所在章节，再用学科内题目补足。 */
    public List<QuizItem> compose(WorkContext w, int n, Integer difficulty) {
        long t = System.currentTimeMillis();
        List<QuizItem> result = new ArrayList<>();
        Set<String> used = new HashSet<>();

        // 1) 围绕检索到的考点关联题
        for (KnowledgeBase.Scored s : w.retrieved) {
            for (QuizItem q : kb.allQuizzes()) {
                if (result.size() >= n) break;
                if (!Objects.equals(q.subject(), w.subject)) continue;
                if (difficulty != null && q.difficulty() != difficulty) continue;
                if (q.kpIds() != null && q.kpIds().contains(s.point().id()) && used.add(q.id())) result.add(q);
            }
        }
        // 2) 同章节补充
        for (KnowledgeBase.Scored s : w.retrieved) {
            if (result.size() >= n) break;
            for (QuizItem q : kb.pickQuizzes(w.subject, s.point().chapter(), difficulty, n, rnd))
                if (used.add(q.id())) result.add(q);
        }
        // 3) 学科内随机补足
        if (result.size() < n) {
            for (QuizItem q : kb.pickQuizzes(w.subject, null, difficulty, n * 3, rnd)) {
                if (result.size() >= n) break;
                if (used.add(q.id())) result.add(q);
            }
        }
        Collections.shuffle(result, rnd);
        w.extra.put("quizCount", result.size());
        w.trace(type(), agentName(), "组卷", "产出" + result.size() + "道题", t);
        return result;
    }
}
