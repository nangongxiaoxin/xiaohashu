package com.slilio.xiaohashu.oss.biz.factory;

import com.slilio.xiaohashu.oss.biz.strategy.FileStrategy;
import com.slilio.xiaohashu.oss.biz.strategy.impl.AliyunOSSFileStrategy;
import com.slilio.xiaohashu.oss.biz.strategy.impl.MinioFileStrategy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class FileStrategyFactory {
  @Value("${storage.type}")
  private String strategyType; // 策略类型

  @Bean
  @RefreshScope
  public FileStrategy getFileStrategy() {
    if (StringUtils.equals(strategyType, "minio")) {
      return new MinioFileStrategy();
    } else if (StringUtils.equals(strategyType, "aliyun")) {
      return new AliyunOSSFileStrategy();
    }

    throw new IllegalArgumentException("不可用的存储类型");
  }
}
