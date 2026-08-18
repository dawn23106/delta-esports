package com.delta.esports.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数归一化。防止客户端传入超大或非法 page/size 导致全表拉取。
 */
public final class PageSupport {

    public static final int MAX_SIZE = 100;

    private PageSupport() {
    }

    public static int normalizePage(int page) {
        return page < 1 ? 1 : page;
    }

    public static int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    public static <T> Page<T> of(int page, int size) {
        return new Page<>(normalizePage(page), normalizeSize(size));
    }
}
