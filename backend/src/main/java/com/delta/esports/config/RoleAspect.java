package com.delta.esports.config;

import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Aspect
@Component
public class RoleAspect {

    @Around("@within(com.delta.esports.config.RequireRole) || @annotation(com.delta.esports.config.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireRole requireRole = joinPoint.getTarget().getClass().getAnnotation(RequireRole.class);
        if (requireRole == null) {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            requireRole = sig.getMethod().getAnnotation(RequireRole.class);
        }

        if (requireRole == null || requireRole.value().length == 0) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = ((ServletRequestAttributes)
            RequestContextHolder.currentRequestAttributes()).getRequest();

        String role = (String) request.getAttribute("role");
        if (role == null) {
            throw new BusinessException(401, "未登录");
        }
        if (Arrays.stream(requireRole.value()).noneMatch(r -> r.equals(role))) {
            throw new BusinessException(403, "无权限");
        }
        return joinPoint.proceed();
    }
}
