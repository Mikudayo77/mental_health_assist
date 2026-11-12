package com.example.aispring.common.constant;

import java.util.List;

/**
 * 危机干预敏感词库（第一道快速筛查，命中后才调用大模型评级）
 */
public class SensitiveWordBank {

    public static final List<String> SENSITIVE_WORDS = List.of(
            "自杀", "自残", "轻生", "不想活", "活不下去", "想死", "去死",
            "结束生命", "了结生命", "了结自己", "告别世界", "活够了", "活着没意思",
            "跳楼", "割腕", "上吊", "卧轨", "跳河", "烧炭", "吞药", "安眠药",
            "遗书", "遗言", "伤害自己", "自杀计划", "慢性自杀", "崩溃到想死"
    );

    private SensitiveWordBank() {
    }
}
