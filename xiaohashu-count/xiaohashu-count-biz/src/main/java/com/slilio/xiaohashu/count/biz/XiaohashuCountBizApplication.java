package com.slilio.xiaohashu.count.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author: slilio @CreateTime: 2025-04-28 @Description: 启动类 @Version: 1.0
 */
@SpringBootApplication
@MapperScan("com.slilio.xiaohashu.count.biz.domain.mapper")
public class XiaohashuCountBizApplication {
  public static void main(String[] args) {
    SpringApplication.run(XiaohashuCountBizApplication.class, args);
  }
}
