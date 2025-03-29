package com.slilio.xiaohashu.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.slilio.xiaohashu.user.biz.domain.mapper")
public class XiaohashuUserBizApplication {
  public static void main(String[] args) {
    SpringApplication.run(XiaohashuUserBizApplication.class, args);
  }
}
