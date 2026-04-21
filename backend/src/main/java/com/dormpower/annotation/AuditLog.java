package com.dormpower.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * 
 * 用于记录关键操作的审计日志:
 * - 用户登录/登出
 * - 设备控制
 * - 数据导出
 * - 配置修改
 * 
 * 使用方式:
 * @AuditLog(action = "DEVICE_CONTROL", resource = "device:{deviceId}")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 操作类型
     */
    String action() default "";

    /**
     * 资源标识
     * 支持 SpEL 表达式，如：#deviceId
     */
    String resource() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 是否记录请求参数
     */
    boolean logParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean logResult() default false;

}
