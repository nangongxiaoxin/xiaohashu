package com.slilio.xiaohashu.search.biz.config;

import jakarta.annotation.Resource;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: slilio @CreateTime: 2025-05-27 @Description: @Version: 1.0
 */
@Configuration
public class ElasticsearchRestHighLevelClient {
  @Resource private ElasticsearchProperties elasticsearchProperties;

  private static final String COLON = ":";
  private static final String HTTP = "http";

  @Bean
  public RestHighLevelClient restHighLevelClient() {
    String address = elasticsearchProperties.getAddress();

    // 按冒号分隔
    String[] addressArr = address.split(COLON);
    // ip地址
    String host = addressArr[0];
    // 端口
    int port = Integer.parseInt(addressArr[1]);
    HttpHost httpHost = new HttpHost(host, port, HTTP);

    return new RestHighLevelClient(RestClient.builder(httpHost));
  }
}
