package com.slilio.xiaohashu.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.slilio.xiaohashu.user.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.slilio.xiaohashu")
public class XiaohashuUserBizApplication {
  public static void main(String[] args) {
    SpringApplication.run(XiaohashuUserBizApplication.class, args);
  }
}
