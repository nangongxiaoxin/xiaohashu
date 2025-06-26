package com.slilio.xiaohashu.count.biz.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Author: slilio @CreateTime: 2025-06-26 @Description: @Version: 1.0
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
    // 线程活跃时间
    executor.setKeepAliveSeconds(30);
    // 线程名前缀
    executor.setThreadNamePrefix("CountExecutor-");
    // 拒绝策略：由调用线程处理（一般为主线程）
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    // 等待所有任务结束再关闭
    executor.setWaitForTasksToCompleteOnShutdown(true);
    // 设置等待时间，超过时间没有销毁就强制销毁，确保应用能关闭，而不是被阻塞
    executor.setAwaitTerminationSeconds(60);

    executor.initialize();
    return executor;
  }
}
