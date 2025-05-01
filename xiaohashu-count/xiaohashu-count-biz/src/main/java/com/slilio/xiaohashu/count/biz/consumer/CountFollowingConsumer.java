package com.slilio.xiaohashu.count.biz.consumer;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.count.biz.enums.FollowUnfollowTypeEnum;
import com.slilio.xiaohashu.count.biz.model.dto.CountFollowUnfollowMqDTO;
import jakarta.annotation.Resource;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-04-29 @Description: 关注计数消费 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_FOLLOWING, // group组
    topic = MQConstants.TOPIC_COUNT_FOLLOWING // 主题topic
    )
@Slf4j
public class CountFollowingConsumer implements RocketMQListener<String> {
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private RocketMQTemplate rocketMQTemplate;

  @Override
  public void onMessage(String body) {
    log.info("### 消费了MQ【计数：关注数】，{}...", body);
    if (StringUtils.isBlank(body)) {
      return;
    }

    // 关注数和粉丝数场景不同，单个用户无法短时间内关注大量用户，所以无需聚合
    // 直接对Redis中的Hash进行+1或者-1

    CountFollowUnfollowMqDTO countFollowUnfollowMqDTO =
        JsonUtils.parseObject(body, CountFollowUnfollowMqDTO.class);

    // 操作类型：关注 或者 取关
    Integer type = countFollowUnfollowMqDTO.getType();
    // 原用户ID
    Long userId = countFollowUnfollowMqDTO.getUserId();

    // 更新redis
    String redisKey = RedisKeyConstants.buildCountUserKey(userId);
    // 判断hash是否存在
    boolean isExisted = redisTemplate.hasKey(redisKey);
    // 若存在
    if (isExisted) {
      // 关注数：关注+1、取关-1
      long count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
      // 对Hash中的followingTotal字段进行加减操作
      redisTemplate
          .opsForHash()
          .increment(redisKey, RedisKeyConstants.FIELD_FOLLOWING_TOTAL, count);
    }

    // 发送MQ，数据落数据库
    // 构建消息对象
    Message<String> message = MessageBuilder.withPayload(body).build();
    // 异步发送MQ消息
    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_COUNT_FOLLOWING_2_DB,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==》 【计数服务：关注数入库】MQ发送成功，sendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==》 【计数服务：关注数入库】MQ发送异常：", throwable);
          }
        });
  }
}
