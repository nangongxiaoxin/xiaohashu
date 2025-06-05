package com.slilio.xiaohashu.comment.biz.retry;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: 发送MQ重试工具类 @Version: 1.0
 */
@Component
@Slf4j
public class SendMqRetryHelper {

  @Resource private RocketMQTemplate rocketMQTemplate;
  @Resource private RetryTemplate retryTemplate;

  @Resource(name = "taskExecutor")
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  /**
   * 异步发送MQ
   *
   * @param topic 主题
   * @param body 消息体
   */
  public void asyncSend(String topic, String body) {
    log.info("==> 开始异步发送 MQ, Topic: {}, body: {}", topic, body);

    // 构建消息对象，并将DTO转换为Json字符串设置到消息体中
    Message<String> message = MessageBuilder.withPayload(body).build();

    // 异步发丝MQ
    rocketMQTemplate.asyncSend(
        topic,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==> 【评论发布】MQ 发送成功，SendResult: {}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==> 【评论发布】MQ 发送异常：", throwable);
            handleRetry(topic, message);
          }
        });
  }

  /**
   * 重试处理
   *
   * @param topic
   * @param message
   */
  private void handleRetry(String topic, Message<String> message) {
    // 异步处理
    threadPoolTaskExecutor.submit(
        () -> {
          try {
            // 通过retryTemplate执行重试
            retryTemplate.execute(
                (RetryCallback<Void, RuntimeException>)
                    context -> {
                      log.info(
                          "==> 开始重试MQ发送，当前重试次数：{}，时间：{}",
                          context.getRetryCount() + 1,
                          LocalDateTime.now());
                      // 同步发送MQ
                      rocketMQTemplate.syncSend(topic, message);
                      return null;
                    });
          } catch (Exception e) {
            // 多次重试失败，进入兜底方案
            fallback(e, topic, message.getPayload());
          }
        });
  }

  /**
   * 兜底方案：将发送失败的MQ写入数据库，之后，通过定时任务扫表，将发送失败的MQ再次发送，最终发送成功后，将删除该物理记录
   *
   * @param e
   * @param topic
   * @param bodyJson
   */
  private void fallback(Exception e, String topic, String bodyJson) {
    log.error("==> 多次发送失败, 进入兜底方案, Topic: {}, bodyJson: {}", topic, bodyJson);
    // todo
  }
}
