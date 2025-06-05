package com.slilio.xiaohashu.comment.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @Author: slilio @CreateTime: 2025-06-04 @Description: 启动类 @Version: 1.0
 */
@SpringBootApplication
@MapperScan("com.slilio.xiaohashu.comment.biz.domain.mapper")
@EnableRetry // 启用重试功能
public class XiaohashuCommentBizApplication {
  public static void main(String[] args) {
    SpringApplication.run(XiaohashuCommentBizApplication.class, args);
  }
}
