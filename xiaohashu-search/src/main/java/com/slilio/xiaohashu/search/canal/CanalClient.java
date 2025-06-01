package com.slilio.xiaohashu.search.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import jakarta.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-01 @Description: @Version: 1.0
 */
@Component
@Slf4j
public class CanalClient implements DisposableBean {
  @Resource private CanalProperties canalProperties;

  private CanalConnector canalConnector;

  /**
   * 实例化Canal链接对象
   *
   * @return
   */
  @Bean
  public CanalConnector getCanalConnector() {
    // Canal链接地址
    String address = canalProperties.getAddress();
    String[] addressArr = address.split(":");
    // IP地址
    String host = addressArr[0];
    // 端口号
    int port = Integer.parseInt(addressArr[1]);

    // 创建一个CanalConnector实例，连接到指定Canal服务器
    canalConnector =
        CanalConnectors.newSingleConnector(
            new InetSocketAddress(host, port),
            canalProperties.getDestination(),
            canalProperties.getUsername(),
            canalProperties.getPassword());

    // 连接到canal服务端
    canalConnector.connect();
    // 订阅canal中的数据变化，指定要监听的数据库和表（可以使用表名、数据库名的通配符）
    canalConnector.subscribe(canalProperties.getSubscribe());
    // 回滚canal消费者的位点，回滚到上次提交的消息位置
    canalConnector.rollback();
    return canalConnector;
  }

  /***
   * 在spring容器销毁时释放资源
   * @throws Exception
   */
  @Override
  public void destroy() throws Exception {
    if (Objects.nonNull(canalConnector)) {
      // 断开canalConnector与Canal服务的连接
      canalConnector.disconnect();
    }
  }
}
