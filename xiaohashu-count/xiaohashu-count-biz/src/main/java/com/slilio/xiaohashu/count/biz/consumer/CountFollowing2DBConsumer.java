package com.slilio.xiaohashu.count.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.domain.mapper.UserCountDOMapper;
import com.slilio.xiaohashu.count.biz.enums.FollowUnfollowTypeEnum;
import com.slilio.xiaohashu.count.biz.model.dto.CountFollowUnfollowMqDTO;
import jakarta.annotation.Resource;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-02 @Description: 关注数写数据库消费者 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_FOLLOWING_2_DB, // group组
    topic = MQConstants.TOPIC_COUNT_FOLLOWING_2_DB // 主题topic
    )
@Slf4j
public class CountFollowing2DBConsumer implements RocketMQListener<String> {
  @Resource private UserCountDOMapper userCountDOMapper;

  // 每秒创建5000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(5000);

  @Override
  public void onMessage(String body) {
    // 流量削峰：通过获取令牌，如果没有令牌可用，则阻塞，直到获取
    rateLimiter.acquire();

    log.info("## 消费到了 MQ 【计数：关注数入库】，{}...", body);

    if (StringUtils.isBlank(body)) {
      return;
    }

    CountFollowUnfollowMqDTO countFollowUnfollowMqDTO =
        JsonUtils.parseObject(body, CountFollowUnfollowMqDTO.class);

    // 操作类型：关注、取关
    Integer type = countFollowUnfollowMqDTO.getType();
    // 原用户ID
    Long userId = countFollowUnfollowMqDTO.getUserId();

    // 关注数：+1，取关-1
    int count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
    // 判断数据库中，若原用户的记录不存在，则插入；否则直接更新
    userCountDOMapper.insertOrUpdateFollowingTotalByUserId(count, userId);
  }
}
