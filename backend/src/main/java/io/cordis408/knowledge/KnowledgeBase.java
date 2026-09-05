package io.cordis408.knowledge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 408 知识库（RAG 的本地语料 / 对应论文中由 RetrievalAgent provide 的 "knowledge.base" 协效果）。
 *
 * <p>启动时从 classpath:/data 加载四学科 JSON，建立倒排关键词检索。无需外部数据库即可运行；
 * 接入真实向量库时，只需替换 {@link #search} 的实现，对上层 Agent 透明。</p>
 */
@Component
public class KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBase.class);
    private static final List<String> SUBJECT_FILES = List.of("ds", "co", "cn", "os");

    private final ObjectMapper mapper;
    private final Map<String, KnowledgePoint> points = new LinkedHashMap<>();
    private final Map<String, QuizItem> quizzes = new LinkedHashMap<>();
    private final Map<String, SubjectDoc> subjects = new LinkedHashMap<>();

    public KnowledgeBase(ObjectMapper mapper) { this.mapper = mapper; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubjectDoc(String subject, String name, String english,
                             List<String> chapters, List<KnowledgePoint> points,
                             List<QuizItem> quizzes) { }

    @PostConstruct
    void load() {
        try {
            for (String code : SUBJECT_FILES) {
                var res = new ClassPathResource("data/" + code + ".json");
                SubjectDoc doc = mapper.readValue(res.getInputStream(), SubjectDoc.class);
                subjects.put(doc.subject(), doc);
                if (doc.points() != null)
                    doc.points().forEach(p -> points.put(p.id(), p));
                if (doc.quizzes() != null)
                    doc.quizzes().forEach(q -> quizzes.put(q.id(), q));
            }
            log.info("408 知识库加载完成：{} 个考点，{} 道题", points.size(), quizzes.size());
        } catch (Exception e) {
            throw new IllegalStateException("加载 408 知识库失败: " + e.getMessage(), e);
        }
    }

    public record Scored(KnowledgePoint point, int score) { }

    /** 轻量关键词检索：标题/章名/要点命中加权，subject 非空时限定学科。 */
    public List<Scored> search(String subject, String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        List<String> terms = tokenize(query);
        List<Scored> ranked = new ArrayList<>();
        for (KnowledgePoint p : points.values()) {
            if (subject != null && !subject.isBlank() && !subject.equalsIgnoreCase(p.subject())) continue;
            int score = scoreAgainst(p, terms, query);
            if (score > 0) ranked.add(new Scored(p, score));
        }
        ranked.sort(Comparator.comparingInt(Scored::score).reversed());
        return ranked.size() > limit ? ranked.subList(0, limit) : ranked;
    }

    private int scoreAgainst(KnowledgePoint p, List<String> terms, String raw) {
        int s = 0;
        String title = p.title() == null ? "" : p.title();
        String chapter = p.chapter() == null ? "" : p.chapter();
        for (String t : terms) {
            if (title.contains(t)) s += 5;
            if (chapter.contains(t)) s += 2;
            if (p.summary() != null && p.summary().contains(t)) s += 3;
            if (p.detail() != null && p.detail().contains(t)) s += 1;
            if (p.keyPoints() != null && p.keyPoints().stream().anyMatch(k -> k.contains(t))) s += 2;
        }
        // 英文/缩写直接匹配
        String low = raw.toLowerCase();
        if (title.toLowerCase().contains(low) && low.length() > 2) s += 4;
        return s;
    }

    private List<String> tokenize(String q) {
        String cleaned = q.replaceAll("[\\p{Punct}\\s，。、；：？！（）()]+", " ");
        Set<String> out = new LinkedHashSet<>();
        for (String w : cleaned.split("\\s+")) {
            if (!w.isBlank()) out.add(w);
        }
        // 中文按 2-gram 补充，提升短词命中
        String onlyCn = q.replaceAll("[^\\u4e00-\\u9fa5]", "");
        for (int i = 0; i + 2 <= onlyCn.length(); i++) out.add(onlyCn.substring(i, i + 2));
        return new ArrayList<>(out);
    }

    public KnowledgePoint point(String id) { return points.get(id); }
    public QuizItem quiz(String id) { return quizzes.get(id); }
    public Collection<KnowledgePoint> allPoints() { return points.values(); }
    public Collection<QuizItem> allQuizzes() { return quizzes.values(); }
    public SubjectDoc subject(String code) { return subjects.get(code); }
    public Collection<SubjectDoc> allSubjects() { return subjects.values(); }

    public List<KnowledgePoint> pointsOf(String subject) {
        return points.values().stream().filter(p -> p.subject().equals(subject)).toList();
    }

    public List<String> chaptersOf(String subject) {
        SubjectDoc d = subjects.get(subject);
        return d == null || d.chapters() == null ? List.of() : d.chapters();
    }

    /** 按学科/章/难度抽题（null 表示不限制）。 */
    public List<QuizItem> pickQuizzes(String subject, String chapter, Integer difficulty, int n, Random rnd) {
        List<QuizItem> pool = quizzes.values().stream()
                .filter(q -> subject == null || subject.equals(q.subject()))
                .filter(q -> chapter == null || chapter.equals(q.chapter()))
                .filter(q -> difficulty == null || q.difficulty() == difficulty)
                .collect(Collectors.toList());
        Collections.shuffle(pool, rnd);
        return pool.size() > n ? pool.subList(0, n) : pool;
    }

    /** 学科统计：考点数、高频考点数、题目数、章节数。 */
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (SubjectDoc d : subjects.values()) {
            List<KnowledgePoint> ps = pointsOf(d.subject());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", d.name());
            m.put("english", d.english());
            m.put("chapters", d.chapters() == null ? 0 : d.chapters().size());
            m.put("points", ps.size());
            m.put("highFrequency", ps.stream().filter(p -> "high".equals(p.frequency())).count());
            m.put("quizzes", quizzes.values().stream().filter(q -> q.subject().equals(d.subject())).count());
            out.put(d.subject(), m);
        }
        return out;
    }
}
