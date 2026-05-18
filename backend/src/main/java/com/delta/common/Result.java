package com.delta.common;

/**
 * 统一响应格式 — 所有接口返回此结构。
 *
 * 格式: { "code": 200, "msg": "success", "data": {...} }
 *
 * 泛型 T 根据接口不同而变化：
 *   Result<User>          — 返回用户对象
 *   Result<PageResult<X>> — 返回分页列表
 *   Result<Void>          — 无数据返回（如删除操作）
 */
public class Result<T> {
    private int code;      // HTTP状态码风格：200成功，400参数错误，401未登录...
    private String msg;    // 提示信息
    private T data;        // 响应数据

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    /** 成功 + 数据 */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = ErrorCode.SUCCESS.code;
        r.msg = ErrorCode.SUCCESS.msg;
        r.data = data;
        return r;
    }

    /** 成功 + 无数据 */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /** 失败 — 自定义code和msg */
    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
