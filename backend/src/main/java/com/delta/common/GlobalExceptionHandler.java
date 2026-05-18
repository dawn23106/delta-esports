package com.delta.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 — 拦截所有 Controller 抛出的异常，统一返回 Result 格式。
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 *   作用：AOP 切面，拦截所有 @RestController 的方法。
 *
 * 异常处理链路:
 *   Service → throws BusinessException → 此handler → Result.fail(code, msg) → 前端
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常 — 返回自定义code和msg，不打印日志 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBiz(BusinessException e) {
        return ResponseEntity.ok(Result.fail(e.getCode(), e.getMessage()));
    }

    /** 参数校验异常 — @Valid 校验失败时触发的 MethodArgumentNotValidException */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ResponseEntity.ok(Result.fail(ErrorCode.BAD_REQUEST.code, msg));
    }

    /** 未知异常 — 打印堆栈日志，前端返回模糊提示（不暴露内部错误给用户） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception e) {
        log.error("未捕获异常", e);
        return ResponseEntity.ok(Result.fail(ErrorCode.SERVER_ERROR.code, "服务器繁忙，请稍后重试"));
    }
}
