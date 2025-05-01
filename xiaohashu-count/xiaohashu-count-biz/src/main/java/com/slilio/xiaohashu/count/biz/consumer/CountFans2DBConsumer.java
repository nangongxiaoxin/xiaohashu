package com.slilio.xiaohashu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.domain.mapper.UserCountDOMapper;
import jakarta.annotation.Resource;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-02 @Description: 粉丝数入库 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_FANS_2_DB, // group组
    topic = MQConstants.TOPIC_COUNT_FANS_2_DB // 主题 topic
    )
@Slf4j
public class CountFans2DBConsumer implements RocketMQListener<String> {
  @Resource private UserCountDOMapper userCountDOMapper;

  // 每秒创建5000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(5000);

  @Override
  public void onMessage(String body) {
    // 流量削峰：通过获取令牌，如果没有令牌可用，则阻塞，直到获取
    rateLimiter.acquire();

    log.info("## 消费到了 MQ 【计数：粉丝数入库】，{}...", body);

    Map<Long, Integer> countMap = null;
    try {
      countMap = JsonUtils.parseMap(body, Long.class, Integer.class);
    } catch (Exception e) {
      log.error("##解析JSON字符串异常", e);
    }

    if (CollUtil.isNotEmpty(countMap)) {
      // 判断数据库中，若目标用户的记录不存在，则插入;若记录存在，则直接更新
      countMap.forEach((k, v) -> userCountDOMapper.insertOrUpdateFansTotalByUserId(v, k));
    }
  }
}
