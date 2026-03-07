package com.dormpower.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步配置类
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 配置异步线程池（轻量级配置，适合2核2G服务器）
     * @return 线程池执行器
     */
    /**
     * 创建并配置一个名为"taskExecutor"的线程池执行器
     * @return 配置好的线程池执行器
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        // 创建ThreadPoolTaskExecutor实例，它是Spring提供的线程池实现
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程池大小为2，即使空闲也会保持的线程数量
        executor.setCorePoolSize(2);
        // 设置最大线程池大小为4，当任务队列满时，可以创建的最大线程数
        executor.setMaxPoolSize(4);
        // 设置任务队列容量为50，当所有线程都在忙碌时，可以排队等待执行的任务数量
        executor.setQueueCapacity(50);
        // 设置线程名称前缀为"async-"，方便识别和监控线程
        executor.setThreadNamePrefix("async-");
        // 设置拒绝执行策略，当队列满时的处理方式
        // 这里使用Lambda表达式实现RejectedExecutionHandler接口
        // 当队列满时，将在调用线程中执行该任务（而不是拒绝任务）
        executor.setRejectedExecutionHandler((r, e) -> {
            // 队列满时在调用线程执行
        });
        // 初始化线程池执行器
        executor.initialize();
        // 返回配置好的线程池执行器
        return executor;
    }

}
