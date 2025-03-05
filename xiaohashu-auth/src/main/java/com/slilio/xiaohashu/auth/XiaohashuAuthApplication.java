package com.slilio.xiaohashu.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.slilio.xiaohashu.auth.domain.mapper")
public class XiaohashuAuthApplication {

  public static void main(String[] args) {

    SpringApplication.run(XiaohashuAuthApplication.class, args);
  }
}
