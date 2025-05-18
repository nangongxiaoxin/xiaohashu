package com.slilio.xiaohashu.data.align.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-19 @Description: @Version: 1.0
 */
@ConfigurationProperties(prefix = XxlJobProperties.PREFIX) // 读取配置变量 并绑定
@Component
@Data
public class XxlJobProperties {
  public static final String PREFIX = "xxl.job";

  private String adminAddresses;

  private String accessToken;

  private String appName;

  private String ip;

  private int port;

  private String logPath;

  private int logRetentionDays = 30;
}
