package com.slilio.xiaohashu.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.slilio.xiaohashu.auth.domain.mapper")
@EnableFeignClients(basePackages = "com.slilio.xiaohashu")
public class XiaohashuAuthApplication {

  public static void main(String[] args) {

    SpringApplication.run(XiaohashuAuthApplication.class, args);
  }
}
