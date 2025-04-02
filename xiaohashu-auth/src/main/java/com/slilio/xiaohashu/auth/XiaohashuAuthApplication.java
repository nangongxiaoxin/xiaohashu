package com.slilio.xiaohashu.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.slilio.xiaohashu")
public class XiaohashuAuthApplication {

  public static void main(String[] args) {

    SpringApplication.run(XiaohashuAuthApplication.class, args);
  }
}
