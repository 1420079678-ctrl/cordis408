package io.cordis408.agent;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 意图路由 Agent（Dispatcher）。
 *
 * <p>提供协效果 {@code router}：识别问题所属学科（ds/co/cn/os）与任务模式
 * （答疑/引导/出题/批改/规划），决定后续由哪些专家与教学 Agent 参与协同。</p>
 */
@Component
public class RouterAgent extends AbstractAgent {

    public static final String KEY = "router";

    private static final Map<String, String[]> SUBJECT_HINTS = new LinkedHashMap<>();
    static {
        SUBJECT_HINTS.put("ds", new String[]{"数据结构", "链表", "顺序表", "栈", "队列", "二叉树", "森林",
                "哈夫曼", "图", "邻接", "拓扑", "最短路径", "最小生成树", "排序", "查找", "折半", "b树",
                "红黑", "avl", "复杂度", "散列", "哈希", "堆", "遍历", "线索", "kruskal", "prim", "dijkstra"});
        SUBJECT_HINTS.put("co", new String[]{"组成原理", "补码", "原码", "反码", "移码", "浮点", "ieee",
                "阶码", "尾数", "溢出", "cache", "高速缓存", "主存", "虚存", "页表", "tlb", "指令",
                "寻址", "流水线", "cpu", "alu", "总线", "dma", "中断", "寄存器", "数据通路", "微程序",
                "硬布线", "cpi", "mips", "字长"});
        SUBJECT_HINTS.put("cn", new String[]{"计算机网络", "网络", "tcp", "udp", "ip", "http", "https",
                "dns", "dhcp", "arp", "icmp", "路由", "rip", "ospf", "bgp", "子网", "cidr", "csma",
                "以太网", "帧", "vlan", "交换机", "三次握手", "四次挥手", "拥塞", "流量控制", "滑动窗口",
                "奈氏", "香农", "物理层", "链路层", "网络层", "传输层", "应用层", "smtp", "ftp", "nat"});
        SUBJECT_HINTS.put("os", new String[]{"操作系统", "进程", "线程", "死锁", "信号量", "临界区",
                "互斥", "同步", "调度", "分页", "分段", "虚拟内存", "缺页", "页面置换", "lru", "fifo",
                "clock", "belady", "文件", "inode", "磁盘", "scan", "sstf", "银行家", "管程", "pv",
                "spooling", "缓冲", "作业", "并发", "上下文切换"});
    }

    public RouterAgent() { super("router"); }
    @Override public String agentName() { return "意图路由 Agent"; }
    @Override protected String provideKey(JsonNode c) { return KEY; }

    public void classify(WorkContext w) {
        long t = System.currentTimeMillis();
        String q = w.rawQuestion.toLowerCase();

        // 1) 任务模式
        if (containsAny(q, "出题", "来几道", "练习题", "组卷", "刷题", "来一题", "考我")) w.mode = "quiz";
        else if (containsAny(q, "怎么学", "学习计划", "规划", "复习计划", "备考", "安排", "路径")) w.mode = "plan";
        else if (containsAny(q, "提示", "引导", "别直接给答案", "启发", "一步步问我")) w.mode = "tutor";
        else if (containsAny(q, "批改", "我选", "答案是", "对不对", "判一下", "这题我做")) w.mode = "grade";
        else w.mode = "ask";

        // 2) 学科
        Map<String, Integer> score = new LinkedHashMap<>();
        SUBJECT_HINTS.forEach((s, words) -> {
            int n = 0;
            for (String word : words) if (q.contains(word.toLowerCase())) n++;
            if (n > 0) score.put(s, n);
        });
        String best = null; int bestN = 0;
        for (var e : score.entrySet()) if (e.getValue() > bestN) { bestN = e.getValue(); best = e.getKey(); }
        w.subject = best;

        String desc = "模式=" + modeName(w.mode) + "；学科=" + (best == null ? "待检索判定/跨学科" : Subjects.name(best));
        w.trace(type(), agentName(), "意图识别", desc, t);
    }

    public static String modeName(String m) {
        return switch (m) {
            case "quiz" -> "出题练习";
            case "plan" -> "学习规划";
            case "tutor" -> "苏格拉底引导";
            case "grade" -> "批改诊断";
            default -> "知识答疑";
        };
    }

    private boolean containsAny(String q, String... keys) {
        for (String k : keys) if (q.contains(k.toLowerCase())) return true;
        return false;
    }
}
