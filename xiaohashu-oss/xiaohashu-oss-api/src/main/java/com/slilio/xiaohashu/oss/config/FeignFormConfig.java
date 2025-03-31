package com.slilio.xiaohashu.oss.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 表单配置类 */
@Configuration
public class FeignFormConfig {
  @Bean
  public Encoder feignFormEncoder() {
    return new SpringFormEncoder();
  }
}
