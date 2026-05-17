package com.delta.common;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BusinessException(ErrorCode ec) {
        super(ec.msg);
        this.code = ec.code;
    }

    public BusinessException(ErrorCode ec, String msg) {
        super(msg);
        this.code = ec.code;
    }

    public int getCode() { return code; }
}
