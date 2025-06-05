package com.slilio.xiaohashu.comment.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description:Retry 配置读取 @Version: 1.0
 */
@ConfigurationProperties(prefix = RetryProperties.PREFIX)
@Component
@Data
public class RetryProperties {
  public static final String PREFIX = "retry";

  private Integer maxAttempts = 3; // 最大重试次数
  private Integer initInterval = 1000; // 间隔时间 ms
  private Double multiplier = 2.0; // 乘积，间隔时间
}
