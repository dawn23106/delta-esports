package com.delta.common;

import java.util.List;

/**
 * 分页响应格式 — 包装在 Result.data 中返回。
 *
 * 格式: { "list": [...], "total": 50, "page": 1, "pageSize": 20 }
 *
 * total 用于前端计算总页数: Math.ceil(total / pageSize)
 */
public class PageResult<T> {
    private List<T> list;     // 当前页数据
    private long total;       // 总记录数
    private int page;         // 当前页码
    private int pageSize;     // 每页条数

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    /** 工厂方法 — 封装分页数据 */
    public static <T> PageResult<T> of(List<T> list, long total, int page, int pageSize) {
        PageResult<T> r = new PageResult<>();
        r.list = list;
        r.total = total;
        r.page = page;
        r.pageSize = pageSize;
        return r;
    }
}
