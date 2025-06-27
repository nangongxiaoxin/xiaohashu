package com.slilio.xiaohashu.user.biz.consumer;

import com.slilio.xiaohashu.user.biz.constant.MQConstants;
import com.slilio.xiaohashu.user.biz.constant.RedisKeyConstants;
import jakarta.annotation.Resource;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-28 @Description: 延迟双删
 * 解决分布式情况下，多服务直接的数据不一致，即此应用信息变更了，但是别的还没有变更 @Version: 1.0
 */
@Component
@Slf4j
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_DELAY_DELETE_USER_REDIS_CACHE,
    topic = MQConstants.TOPIC_DELAY_DELETE_USER_REDIS_CACHE)
public class DelayDeleteUserRedisCacheConsumer implements RocketMQListener<String> {
  @Resource RedisTemplate<String, Object> redisTemplate;

  @Override
  public void onMessage(String body) {
    Long userId = Long.valueOf(body);
    log.info("## 延迟消息消费成功, userId: {}", userId);

    // 删除Redis用户缓存

    // 构建redis key
    String userProfileRedisKey = RedisKeyConstants.buildUserProfileKey(userId);
    String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);

    // 批量删除
    redisTemplate.delete(Arrays.asList(userProfileRedisKey, userInfoRedisKey));
  }
}
