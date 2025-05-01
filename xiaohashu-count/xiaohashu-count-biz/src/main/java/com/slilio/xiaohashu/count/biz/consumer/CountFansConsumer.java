package com.slilio.xiaohashu.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.count.biz.enums.FollowUnfollowTypeEnum;
import com.slilio.xiaohashu.count.biz.model.dto.CountFollowUnfollowMqDTO;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
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

  @Resource private RedisTemplate<String, Object> redisTemplate;

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

    // List<String> 转List<CountFollowUnfollowMqDTO>
    List<CountFollowUnfollowMqDTO> countFollowUnfollowMqDTOS =
        bodys.stream()
            .map(body -> JsonUtils.parseObject(body, CountFollowUnfollowMqDTO.class))
            .toList();

    // 按目标用户进行分组
    Map<Long, List<CountFollowUnfollowMqDTO>> groupMap =
        countFollowUnfollowMqDTOS.stream()
            .collect(Collectors.groupingBy(CountFollowUnfollowMqDTO::getTargetUserId));

    // 按组汇总数据，统计出最终的计数
    // key为目标用户ID，value为最终操作的计数
    Map<Long, Integer> countMap = Maps.newHashMap();

    for (Map.Entry<Long, List<CountFollowUnfollowMqDTO>> entry : groupMap.entrySet()) {
      List<CountFollowUnfollowMqDTO> list = entry.getValue();
      // 最终的计数值，默认为0
      int finalCount = 0;
      for (CountFollowUnfollowMqDTO countFollowUnfollowMqDTO : list) {
        // 获取操作的类型
        Integer type = countFollowUnfollowMqDTO.getType();

        // 根据操作的类型，获取对应的枚举
        FollowUnfollowTypeEnum followUnfollowTypeEnum = FollowUnfollowTypeEnum.valueOf(type);

        // 若枚举为空，则跳到下一个循环
        if (Objects.isNull(followUnfollowTypeEnum)) continue;

        switch (followUnfollowTypeEnum) {
          case FOLLOW -> finalCount += 1; // 如果为关注操作，粉丝数+1
          case UNFOLLOW -> finalCount -= 1; // 如果为取关操作，粉丝数-1
        }
      }
      // 将分组后统计出的最终计数，存入countMap中
      countMap.put(entry.getKey(), finalCount);
    }

    log.info("## 聚合后的计数数据为：{}", JsonUtils.toJsonString(countMap));

    // 更新redis
    countMap.forEach(
        (k, v) -> {
          // RedisKey
          String redisKey = RedisKeyConstants.buildCountUserKey(k);
          // 判断Redis中Hash是否存在
          boolean isExisted = redisTemplate.hasKey(redisKey);
          // 若存在才会更新
          // 因为缓存设有过期时间，考虑到过期后，缓存会被删除，这里需要判断下，存在的才会去更新，而初始工作放在查询计数来做
          if (isExisted) {
            // 对目标用户Hash中的粉丝数字段进行计数操作
            redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_FANS_TOTAL, v);
          }
        });
    // todo：发送MQ，计数数据落库
  }
}
