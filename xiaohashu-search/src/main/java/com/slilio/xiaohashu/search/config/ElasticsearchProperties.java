package com.slilio.xiaohashu.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-27 @Description: @Version: 1.0
 */
@Component
@ConfigurationProperties(prefix = "elasticsearch")
@Data
public class ElasticsearchProperties {
  private String address;
}
