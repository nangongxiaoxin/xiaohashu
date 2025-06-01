package com.slilio.xiaohashu.search;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Author: slilio @CreateTime: 2025-05-27 @Description: 启动类 @Version: 1.0
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.slilio.xiaohashu.search.domain.mapper")
public class XiaohashuSearchApplication {
  public static void main(String[] args) {
    SpringApplication.run(XiaohashuSearchApplication.class, args);
  }
}
