package com.slilio.xiaohashu.comment.biz;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.comment.biz.model.dto.LikeUnlikeCommentMqDTO;
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
 * @Author: slilio @CreateTime: 2025-06-05 @Description: 测试 @Version: 1.0
 */
@SpringBootTest
@Slf4j
class MQTest {

  @Resource private RocketMQTemplate rocketMQTemplate;

  /** 测试：模拟发送评论发布消息 */
  @Test
  void testBatchSendMQ() {
    for (long i = 0; i < 1620; i++) {

      // 构建消息对象
      Message<String> message = MessageBuilder.withPayload("消息体数据").build();

      // 异步发送 MQ 消息
      rocketMQTemplate.asyncSend(
          "PublishCommentTopic",
          message,
          new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
              log.info("==> 【评论发布】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
              log.error("==> 【评论发布】MQ 发送异常: ", throwable);
            }
          });
    }
  }

  @Test
  void testBatchSendLikeUnlikeCommentMQ() {
    Long userId = 1L;
    Long commentId = 4001L;

    for (long i = 0; i < 32; i++) {
      // 消息体
      LikeUnlikeCommentMqDTO likeUnlikeCommentMqDTO =
          LikeUnlikeCommentMqDTO.builder()
              .userId(userId)
              .commentId(commentId)
              .createTime(LocalDateTime.now())
              .build();

      // topic和tag
      String destination = "CommentLikeUnlikeTopic:";

      if (i % 2 == 0) {
        // 偶数
        likeUnlikeCommentMqDTO.setType(0); // 取消点赞
        destination += "Unlike";
      } else {
        // 偶数
        likeUnlikeCommentMqDTO.setType(1); // 点赞
        destination += "Like";
      }
      // MQ分区键
      String hashKey = String.valueOf(userId);

      // 构建消息
      Message<String> message =
          MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeCommentMqDTO)).build();

      // 同步消息发送
      rocketMQTemplate.syncSendOrderly(destination, message, hashKey);
    }
  }
}
