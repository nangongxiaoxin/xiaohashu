package com.slilio.xiaohashu.count.biz.consumer;

import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
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
public class CountFollowingConsumer implements RocketMQListener<String> { // TODO :Message

  @Override
  public void onMessage(String body) {
    log.info("### 消费了MQ【计数：关注数】，{}...", body);
  }
}
