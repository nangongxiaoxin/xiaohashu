package com.slilio.xiaohashu.data.align;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: slilio @CreateTime: 2025-05-19 @Description: 启动类 @Version: 1.0
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.slilio.xiaohashu")
@MapperScan("com.slilio.xiaohashu.data.align.domain.mapper")
public class XiaohashuDataAlignApplication {
  public static void main(String[] args) {
    SpringApplication.run(XiaohashuDataAlignApplication.class, args);
  }
}
