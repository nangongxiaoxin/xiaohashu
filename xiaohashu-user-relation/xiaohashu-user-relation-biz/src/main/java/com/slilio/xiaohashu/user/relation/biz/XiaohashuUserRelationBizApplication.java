package com.slilio.xiaohashu.user.relation.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: slilio @CreateTime: 2025-04-18 @Description: 启动类 @Version: 1.0
 */
@SpringBootApplication
@MapperScan("com.slilio.xiaohashu.user.relation.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.slilio.xiaohashu")
public class XiaohashuUserRelationBizApplication {
  public static void main(String[] args) {
    SpringApplication.run(XiaohashuUserRelationBizApplication.class, args);
  }
}
