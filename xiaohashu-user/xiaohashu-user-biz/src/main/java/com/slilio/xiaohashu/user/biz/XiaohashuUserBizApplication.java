package com.slilio.xiaohashu.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.slilio.xiaohashu.user.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.slilio.xiaohashu")
@ComponentScan({"com.slilio.xiaohashu.user.biz", "com.slilio.xiaohashu.count"})
public class XiaohashuUserBizApplication {
  public static void main(String[] args) {
    SpringApplication.run(XiaohashuUserBizApplication.class, args);
  }
}
