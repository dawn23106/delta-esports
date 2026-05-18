package com.delta.common;

import java.util.Set;

/**
 * 敏感词过滤器 — 下单时检测订单描述是否包含敏感词。
 *
 * 目前是内存词库（demo级别），生产环境应接入：
 *   - 阿里云内容安全 API
 *   - 网易易盾
 *   - 或自建 DFA 算法+词库
 *
 * 主要拦截场景：
 *   飞单 — 加微信/私下交易/绕过平台（防止平台抽成流失）
 *   外挂 — 作弊/脚本（违反游戏规则）
 *   赌博 — 赌/彩票（法律红线）
 */
public class SensitiveWordFilter {

    private static final Set<String> BLOCKED = Set.of(
            // 飞单关键词 — 用户试图绕开平台私下交易
            "加微信", "加我微信", "加我vx", "加vx", "加q", "加我q",
            "私下交易", "私下", "免平台", "免手续费", "绕过平台",
            "私单", "走私下", "不加平台", "直接转账", "扫码支付",
            // 外挂/作弊
            "外挂", "作弊", "脚本", "代练脚本",
            // 赌博
            "赌博", "赌", "彩票", "时时彩"
    );

    /** 检测文本是否包含任一敏感词 */
    public static boolean hasSensitive(String text) {
        if (text == null || text.isBlank()) return false;
        for (String word : BLOCKED) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    /** 返回第一个匹配到的敏感词（用于提示用户"包含敏感词: xxx"） */
    public static String getFirstMatch(String text) {
        if (text == null || text.isBlank()) return null;
        for (String word : BLOCKED) {
            if (text.contains(word)) return word;
        }
        return null;
    }
}
