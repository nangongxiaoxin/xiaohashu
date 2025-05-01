package com.slilio.xiaohashu.count.biz;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.user.relation.biz.enums.FollowUnfollowTypeEnum;
import com.slilio.xiaohashu.user.relation.biz.model.dto.CountFollowUnfollowMqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @Author: slilio @CreateTime: 2025-04-30 @Description: 测试 @Version: 1.0
 */
@SpringBootTest
@Slf4j
public class MQTest {
  @Resource private RocketMQTemplate rocketMQTemplate;

  /** 测试：发送计数 MQ, 以统计粉丝数 */
  @Test
  void testSendCountFollowUnfollowMQ() {
    // 循环发送 3200 条 MQ
    for (long i = 0; i < 3200; i++) {
      // 构建消息体 DTO
      CountFollowUnfollowMqDTO countFollowUnfollowMqDTO =
          CountFollowUnfollowMqDTO.builder()
              .userId(i + 1) // 关注者用户 ID
              .targetUserId(1L) // 目标用户
              .type(FollowUnfollowTypeEnum.FOLLOW.getCode())
              .build();

      // 构建消息对象，并将 DTO 转成 Json 字符串设置到消息体中
      org.springframework.messaging.Message<String> message =
          MessageBuilder.withPayload(JsonUtils.toJsonString(countFollowUnfollowMqDTO)).build();

      // 发送 MQ 通知计数服务：统计粉丝数
      rocketMQTemplate.asyncSend(
          MQConstants.TOPIC_COUNT_FANS,
          message,
          new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
              log.info("==> 【计数服务：粉丝数】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
              log.error("==> 【计数服务：粉丝数】MQ 发送异常: ", throwable);
            }
          });

      // 发送 MQ 通知计数服务：统计关注数
      rocketMQTemplate.asyncSend(
          MQConstants.TOPIC_COUNT_FOLLOWING,
          message,
          new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
              log.info("==> 【计数服务：粉丝数】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
              log.error("==> 【计数服务：粉丝数】MQ 发送异常: ", throwable);
            }
          });
    }
  }
}
