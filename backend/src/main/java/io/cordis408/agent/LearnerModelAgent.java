package io.cordis408.agent;

import com.fasterxml.jackson.databind.JsonNode;
import io.cordis408.knowledge.KnowledgePoint;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 学习者建模 / 记忆 Agent。
 *
 * <p>提供协效果 {@code learner.state}：记录各学科提问次数、答题正确率、薄弱考点、最近学习主题，
 * 供专家、出题、诊断、规划 Agent 以 coeffect 方式订阅。它一更新，依赖它的组件就会被响应式通知。</p>
 */
@Component
public class LearnerModelAgent extends AbstractAgent {

    public static final String KEY = "learner.state";

    /** sessionId -> 画像 */
    private final Map<String, Profile> store = new ConcurrentHashMap<>();

    public LearnerModelAgent() { super("learner-model"); }

    @Override public String agentName() { return "学习者建模 Agent"; }

    @Override protected String provideKey(JsonNode config) { return KEY; }

    static final class Profile {
        long createdAt = System.currentTimeMillis();
        int totalAsked = 0;
        int totalAnswered = 0;
        int totalCorrect = 0;
        Map<String, int[]> subjectStat = new LinkedHashMap<>(); // [asked, answered, correct]
        Map<String, Integer> pointAsked = new LinkedHashMap<>();
        Map<String, Integer> pointWrong = new LinkedHashMap<>();
        LinkedList<String> recentTopics = new LinkedList<>();

        int[] stat(String s) { return subjectStat.computeIfAbsent(s, k -> new int[3]); }
    }

    private Profile profile(String sessionId) {
        return store.computeIfAbsent(sessionId == null ? "default" : sessionId, k -> new Profile());
    }

    /** 观察一次答疑行为，更新画像。 */
    public void observe(WorkContext w) {
        Profile p = profile(w.sessionId);
        p.totalAsked++;
        if (w.subject != null) {
            p.stat(w.subject)[0]++;
            for (KnowledgePoint kp : w.evidencePoints()) {
                p.pointAsked.merge(kp.id(), 1, Integer::sum);
                remember(p, kp.title());
            }
        }
        w.learnerSnapshot = snapshot(w.sessionId);
    }

    /** 记录一次答题结果。 */
    public void recordQuiz(String sessionId, String subject, String kpId, boolean correct, int difficulty) {
        Profile p = profile(sessionId);
        int[] st = p.stat(subject);
        st[1]++; p.totalAnswered++;
        if (correct) { st[2]++; p.totalCorrect++; }
        else { p.pointWrong.merge(kpId, 1, Integer::sum); }
    }

    private void remember(Profile p, String topic) {
        p.recentTopics.remove(topic);
        p.recentTopics.addFirst(topic);
        while (p.recentTopics.size() > 8) p.recentTopics.removeLast();
    }

    /** 生成可序列化画像。 */
    public Map<String, Object> snapshot(String sessionId) {
        Profile p = profile(sessionId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalAsked", p.totalAsked);
        m.put("totalAnswered", p.totalAnswered);
        double acc = p.totalAnswered == 0 ? 0 : Math.round(100.0 * p.totalCorrect / p.totalAnswered);
        m.put("accuracy", acc);
        Map<String, Object> subs = new LinkedHashMap<>();
        for (String s : Subjects.NAME.keySet()) {
            int[] st = p.stat(s);
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("name", Subjects.name(s));
            sm.put("asked", st[0]);
            sm.put("answered", st[1]);
            sm.put("correct", st[2]);
            sm.put("accuracy", st[1] == 0 ? 0 : Math.round(100.0 * st[2] / st[1]));
            subs.put(s, sm);
        }
        m.put("subjects", subs);
        m.put("recentTopics", new ArrayList<>(p.recentTopics));
        m.put("weakPointIds", p.pointWrong.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8).map(Map.Entry::getKey).toList());
        return m;
    }

    /** 薄弱学科：答得越多且正确率越低越薄弱；未答题学科不参与。 */
    public List<String> weakSubjects(String sessionId) {
        Profile p = profile(sessionId);
        List<String> weak = new ArrayList<>();
        p.subjectStat.entrySet().stream()
                .filter(e -> e.getValue()[1] >= 1)
                .sorted(Comparator.comparingDouble(e -> e.getValue()[1] == 0 ? 1 :
                        (double) e.getValue()[2] / e.getValue()[1]))
                .forEach(e -> weak.add(e.getKey()));
        return weak;
    }

    /** 重置画像（学习进度清零）。 */
    public void reset(String sessionId) { store.remove(sessionId); }
}
