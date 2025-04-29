package com.slilio.xiaohashu.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-04-29 @Description: 粉丝计数消费 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_FANS, // group组
    topic = MQConstants.TOPIC_COUNT_FANS // 主题topic
    )
@Slf4j
public class CountFansConsumer implements RocketMQListener<String> { // TODO :Message

  private BufferTrigger<String> bufferTrigger =
      BufferTrigger.<String>batchBlocking()
          .bufferSize(50000) // 缓存队列的最大容量
          .batchSize(1000) // 一批次最多聚合1000条
          .linger(Duration.ofSeconds(1)) // 多久聚合一次
          .setConsumerEx(this::consumeMessage)
          .build();

  @Override
  public void onMessage(String body) {
    //    log.info("### 消费了MQ【计数：粉丝数】，{}...", body);

    // 往bufferTrigger 中提那几元素
    bufferTrigger.enqueue(body);
  }

  private void consumeMessage(List<String> bodys) {
    log.info("===》 聚合消息，size：{}", bodys.size());
    log.info("===》 聚合消息，{}", JsonUtils.toJsonString(bodys));
  }
}
