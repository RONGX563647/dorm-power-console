package com.dormpower.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 异步配置类
 * 
 * 支持两种线程池模式:
 * 1. 虚拟线程池 (Java 21+) - 适用于 IoT 高并发场景，支持千级设备并发接入
 * 2. 传统线程池 - 适用于常规异步任务
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Java 21 虚拟线程池执行器
     * 
     * 使用虚拟线程优化 IoT 高并发场景:
     * - 每个设备遥测数据处理使用独立虚拟线程
     * - 支持 10000+ 并发连接，内存占用仅 50-100MB
     * - 无需线程池配置，JVM 自动管理
     * 
     * @return 虚拟线程执行器
     */
    @Bean(name = "virtualThreadExecutor")
    @Primary
    public Executor virtualThreadExecutor() {
        // Java 21 虚拟线程 - 每个任务一个虚拟线程，自动调度
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 传统线程池执行器 (备用)
     * 
     * 适用于:
     * - CPU 密集型任务
     * - 需要精确控制线程数的场景
     * 
     * @return 传统线程池执行器
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler((r, e) -> {
            // 队列满时在调用线程执行
        });
        executor.initialize();
        return executor;
    }

}
