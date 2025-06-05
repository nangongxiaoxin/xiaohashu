package com.slilio.xiaohashu.comment.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.slilio.xiaohashu.comment.biz.constant.MQConstants;
import jakarta.annotation.PreDestroy;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: 评论入库消费者 @Version: 1.0
 */
@Component
@Slf4j
public class Comment2DBConsumer {
  @Value("${rocketmq.name-server}")
  private String namesrvAddr;

  private DefaultMQPushConsumer consumer;

  // 每秒创建1000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(1000);

  @Bean
  public DefaultMQPushConsumer mqPushConsumer() throws MQClientException {
    // group组
    String group = "xiaohashu_group_" + MQConstants.TOPIC_PUBLISH_COMMENT;

    // 创建一个新的DefaultMQPublishConsumer实例，并指定消费者的消费组名
    consumer = new DefaultMQPushConsumer(group);

    // 设置rocketMQ的NameServer地址
    consumer.setNamesrvAddr(namesrvAddr);

    // 订阅指定的主题，并指定主题的订阅规则（“*”表示订阅所有标签的消息）
    consumer.subscribe(MQConstants.TOPIC_PUBLISH_COMMENT, "*");

    // 设置消费者消费消息的起始位置，如果队列中没有消息，则从最新的消息开始消费
    consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

    // 设置消息的消费模式，这里使用集群模式（clustering）
    consumer.setMessageModel(MessageModel.CLUSTERING);

    // 设置每批次的最大消费数量消息，这里设置为30，表示每次拉取时最多30条消息
    consumer.setConsumeMessageBatchMaxSize(30);

    // 注册消息监听器
    consumer.registerMessageListener(
        (MessageListenerConcurrently)
            (msgs, context) -> {
              log.info("==> 本批次消息大小：{}", msgs.size());
              try {
                // 令牌桶流量控制
                rateLimiter.acquire();

                for (MessageExt msg : msgs) {
                  String message = new String(msg.getBody());
                  log.info("==> Consumer - Received message: {}", message);

                  // todo 处理业务
                }

                // 手动ACK，告诉RocketMQ这批消息已被成功消费
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
              } catch (Exception e) {
                log.error("", e);
                // 手动ACK，告诉RocketMQ这批消息消费失败，稍后进行重试
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
              }
            });

    // 启动消费者
    consumer.start();
    return consumer;
  }

  @PreDestroy
  public void destroy() {
    if (Objects.nonNull(consumer)) {
      try {
        consumer.shutdown(); // 关闭消费者
      } catch (Exception e) {
        log.error("", e);
      }
    }
  }
}
