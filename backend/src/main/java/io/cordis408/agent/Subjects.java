package io.cordis408.agent;

import java.util.LinkedHashMap;
import java.util.Map;

/** 408 四学科常量与展示信息。 */
public final class Subjects {

    public static final String DS = "ds";
    public static final String CO = "co";
    public static final String CN = "cn";
    public static final String OS = "os";

    public static final Map<String, String> NAME = new LinkedHashMap<>();
    public static final Map<String, String> EXPERT_KEY = new LinkedHashMap<>();

    static {
        NAME.put(DS, "数据结构");
        NAME.put(CO, "计算机组成原理");
        NAME.put(CN, "计算机网络");
        NAME.put(OS, "操作系统");
        EXPERT_KEY.put(DS, "expert.ds");
        EXPERT_KEY.put(CO, "expert.co");
        EXPERT_KEY.put(CN, "expert.cn");
        EXPERT_KEY.put(OS, "expert.os");
    }

    private Subjects() { }

    public static String name(String code) { return NAME.getOrDefault(code, code); }

    public static String expertKey(String code) { return EXPERT_KEY.get(code); }

    public static boolean isValid(String code) { return NAME.containsKey(code); }
}
