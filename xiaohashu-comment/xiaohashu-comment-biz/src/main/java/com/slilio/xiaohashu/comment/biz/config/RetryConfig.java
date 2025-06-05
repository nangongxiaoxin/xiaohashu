package com.slilio.xiaohashu.comment.biz.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: Retry配置类 @Version: 1.0
 */
@Configuration
public class RetryConfig {

  @Resource private RetryProperties retryProperties;

  @Bean
  public RetryTemplate retryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();

    // 定义重试策略（最多重试三次）
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(retryProperties.getMaxAttempts()); // 设置最大重试次数

    // 定义间隔时间
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(retryProperties.getInitInterval()); // 设置初始间隔时间
    backOffPolicy.setMultiplier(retryProperties.getMultiplier()); // 设置间隔时间的倍数

    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);

    return retryTemplate;
  }
}
