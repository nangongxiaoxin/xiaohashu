package com.slilio.xiaohashu.search.canal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-01 @Description: Canal读取配置 @Version: 1.0
 */
@ConfigurationProperties(prefix = CanalProperties.PREFIX)
@Component
@Data
public class CanalProperties {
  public static final String PREFIX = "canal";

  private String address; // Canal服务器地址
  private String destination; // Canal数据目标
  private String username; // 用户名
  private String password; // 密码
  private String subscribe; // 订阅规则
  private int batchSize = 1000; // 批量读取大小
}
