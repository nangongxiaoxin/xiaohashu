package com.slilio.xiaohashu.comment.biz.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: 线程池配置类 @Version: 1.0
 */
@Configuration
public class ThreadPoolConfig {

  @Bean(name = "taskExecutor")
  public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // 核心线程数
    executor.setCorePoolSize(10);
    // 最大线程数
    executor.setMaxPoolSize(50);
    // 队列容量
    executor.setQueueCapacity(200);
    // 线程池维护线程所允许的空闲时间
    executor.setKeepAliveSeconds(30);
    // 线程名前缀
    executor.setThreadNamePrefix("NoteExecutor-");

    // 拒绝策略 由调用线程处理（一般为主线程）
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    // 等待所有任务结束后再关闭线程池
    executor.setWaitForTasksToCompleteOnShutdown(true);
    // 设置等待时间，如果超过这个时间还没有结束，则强制关闭线程池，以确保应用能被正常关闭，而不是被没有完成的任务阻塞
    executor.setAwaitTerminationSeconds(60);

    executor.initialize();
    return executor;
  }
}
