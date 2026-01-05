package com.ticket.system.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Pointcut("execution(* com.ticket.system.controller..*.*(..))")
    public void controllerLog() {}

    @Before("controllerLog()")
    public void doBefore(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            log.info("=============== 请求开始 ===============");
            log.info("请求地址: {} {}", request.getMethod(), request.getRequestURL().toString());
            log.info("请求IP: {}", request.getRemoteAddr());
            log.info("请求方法: {}.{}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());
            log.info("请求参数: {}", Arrays.toString(joinPoint.getArgs()));
        }
    }

    @AfterReturning(pointcut = "controllerLog()", returning = "result")
    public void doAfterReturning(Object result) {
        log.info("返回结果: {}", result);
        log.info("=============== 请求结束 ===============");
    }

    @AfterThrowing(pointcut = "controllerLog()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable e) {
        log.error("异常方法: {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
        log.error("异常信息: {}", e.getMessage());
        log.error("=============== 请求异常结束 ===============");
    }
}