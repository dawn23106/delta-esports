package com.delta.common;

public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "无权操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "操作冲突，请重试"),
    SERVER_ERROR(500, "服务器内部错误");

    public final int code;
    public final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
