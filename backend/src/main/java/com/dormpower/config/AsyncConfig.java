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
