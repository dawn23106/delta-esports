package com.delta.common;

/**
 * 业务异常 — 用于在 Service 层抛出自定义错误。
 * 继承 RuntimeException（非受检异常），无需在方法签名上声明 throws。
 *
 * 抛出的异常会被 GlobalExceptionHandler 捕获，转为 Result.fail() 返回给前端。
 *
 * 使用:
 *   throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号已注册");
 *   throw new BusinessException(ErrorCode.FORBIDDEN);  // 使用枚举默认msg
 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String msg) {
        super(msg);          // RuntimeException的message
        this.code = code;
    }

    public BusinessException(ErrorCode ec) {
        super(ec.msg);
        this.code = ec.code;
    }

    public BusinessException(ErrorCode ec, String msg) {
        super(msg);          // 覆盖枚举默认msg
        this.code = ec.code;
    }

    public int getCode() { return code; }
}
