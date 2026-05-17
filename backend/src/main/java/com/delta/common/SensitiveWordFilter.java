package com.delta.common;

import java.util.List;
import java.util.Set;

public class SensitiveWordFilter {

    // 飞单关键词 + 敏感词（demo 级别词库，实际应接入内容安全 API）
    private static final Set<String> BLOCKED = Set.of(
            "加微信", "加我微信", "加我vx", "加vx", "加q", "加我q",
            "私下交易", "私下", "免平台", "免手续费", "绕过平台",
            "私单", "走私下", "不加平台", "直接转账", "扫码支付",
            "外挂", "作弊", "脚本", "代练脚本",
            "赌博", "赌", "彩票", "时时彩"
    );

    public static boolean hasSensitive(String text) {
        if (text == null || text.isBlank()) return false;
        for (String word : BLOCKED) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    public static String getFirstMatch(String text) {
        if (text == null || text.isBlank()) return null;
        for (String word : BLOCKED) {
            if (text.contains(word)) return word;
        }
        return null;
    }
}
