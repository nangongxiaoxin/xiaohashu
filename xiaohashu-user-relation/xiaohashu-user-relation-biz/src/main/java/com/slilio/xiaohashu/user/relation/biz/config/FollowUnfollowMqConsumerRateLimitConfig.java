package com.slilio.xiaohashu.user.relation.biz.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: slilio @CreateTime: 2025-04-22 @Description: 限流配置类 @Version: 1.0
 */
@Configuration
@RefreshScope
public class FollowUnfollowMqConsumerRateLimitConfig {

  @Value("${mq-consumer.follow-unfollow.rate-limit}")
  private double rateLimit;

  @Bean
  @RefreshScope
  public RateLimiter rateLimiter() {
    return RateLimiter.create(rateLimit);
  }
}
