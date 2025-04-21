package com.slilio.xiaohashu.user.relation.biz;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.user.relation.biz.constant.MQConstants;
import com.slilio.xiaohashu.user.relation.biz.model.dto.FollowUserMqDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @Author: slilio @CreateTime: 2025-04-22 @Description: 测试 @Version: 1.0
 */
@SpringBootTest
@Slf4j
class MQTests {
  @Resource private RocketMQTemplate rocketMQTemplate;

  @Test
  void testBatchSendMQ() {
    for (long i = 0; i < 10000; i++) {
      // 构建消息体
      FollowUserMqDTO followUserMqDTO =
          FollowUserMqDTO.builder()
              .userId(i)
              .followUserId(i)
              .createTime(LocalDateTime.now())
              .build();

      // 构建消息对象，并将DTO转成Json字符串设置到消息体中
      Message<String> message =
          MessageBuilder.withPayload(JsonUtils.toJsonString(followUserMqDTO)).build();
      // 通过冒号连接，让发送Topic时携带tag
      String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;

      log.info("===》 开始发送关注操作MQ，消息体：{}", followUserMqDTO);

      // 异步发送MQ消息，提升接口响应速度
      rocketMQTemplate.asyncSend(
          destination,
          message,
          new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
              log.info("==> MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
              log.error("==> MQ 发送异常: ", throwable);
            }
          });
    }
  }
}
