package com.slilio.xiaohashu.data.align.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: slilio @CreateTime: 2025-05-19 @Description: @Version: 1.0
 */
@Configuration
@Slf4j
public class XxlJobConfig {

  @Resource XxlJobProperties xxlJobProperties;

  /**
   * 初始化执行器
   *
   * @return
   */
  @Bean
  public XxlJobSpringExecutor xxlJobExecutor() {
    log.info(">>>>>>>>>>> xxl-job config init.");
    XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
    xxlJobSpringExecutor.setAdminAddresses(xxlJobProperties.getAdminAddresses());
    xxlJobSpringExecutor.setAppname(xxlJobProperties.getAppName());
    xxlJobSpringExecutor.setIp(xxlJobProperties.getIp());
    xxlJobSpringExecutor.setPort(xxlJobProperties.getPort());
    xxlJobSpringExecutor.setAccessToken(xxlJobProperties.getAccessToken());
    xxlJobSpringExecutor.setLogPath(xxlJobProperties.getLogPath());
    xxlJobSpringExecutor.setLogRetentionDays(xxlJobProperties.getLogRetentionDays());
    return xxlJobSpringExecutor;
  }
}
