package com.slilio.xiaohashu.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.count.biz.enums.LikeUnlikeNoteTypeEnum;
import com.slilio.xiaohashu.count.biz.model.dto.AggregationCountLikeUnlikeNoteMqDTO;
import com.slilio.xiaohashu.count.biz.model.dto.CountLikeUnLikeNoteMqDTO;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
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
 * @Author: slilio @CreateTime: 2025-05-08 @Description: 笔记点赞计数消费者 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COUNT_NOTE_LIKE, // 组
    topic = MQConstants.TOPIC_COUNT_NOTE_LIKE // 主题
    )
@Slf4j
public class CountNoteLikeConsumer implements RocketMQListener<String> {

  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private RocketMQTemplate rocketMQTemplate;

  private BufferTrigger<String> bufferTrigger =
      BufferTrigger.<String>batchBlocking()
          .bufferSize(50000) // 缓存队列最大容量
          .batchSize(1000) // 一批次最多聚合1000条
          .linger(Duration.ofSeconds(1)) // 多久聚和一次
          .setConsumerEx(this::consumeMessage) // 设置消费者方法
          .build();

  @Override
  public void onMessage(String body) {
    // 往bufferTrigger中添加元素
    bufferTrigger.enqueue(body);
  }

  private void consumeMessage(List<String> bodys) {
    log.info("==》 【笔记点赞数】聚合消息，size:{}", bodys.size());
    log.info("==》 【笔记点赞数】聚合消息，{}", JsonUtils.toJsonString(bodys));

    // List<String> 转 List<CountLikeUnlikeNoteMqDTO>
    List<CountLikeUnLikeNoteMqDTO> countLikeUnLikeNoteMqDTOS =
        bodys.stream()
            .map(body -> JsonUtils.parseObject(body, CountLikeUnLikeNoteMqDTO.class))
            .toList();

    // 按笔记ID进行分组
    Map<Long, List<CountLikeUnLikeNoteMqDTO>> groupMap =
        countLikeUnLikeNoteMqDTOS.stream()
            .collect(Collectors.groupingBy(CountLikeUnLikeNoteMqDTO::getNoteId));

    // 按分组汇总数据，统计出最终的计数
    // key为笔记ID，value为最终操作的计数
    List<AggregationCountLikeUnlikeNoteMqDTO> countList = Lists.newArrayList();

    for (Map.Entry<Long, List<CountLikeUnLikeNoteMqDTO>> entry : groupMap.entrySet()) {
      // 笔记ID
      Long noteId = entry.getKey();
      // 笔记发布者ID
      Long creatorId = null;
      List<CountLikeUnLikeNoteMqDTO> list = entry.getValue();
      // 最终的计数值，默认为0
      int finalCount = 0;
      for (CountLikeUnLikeNoteMqDTO countLikeUnLikeNoteMqDTO : list) {
        // 设置笔记发布者用户ID
        creatorId = countLikeUnLikeNoteMqDTO.getNoteCreatorId();
        // 获取操作类型
        Integer type = countLikeUnLikeNoteMqDTO.getType();
        // 根据操作类型获取对应的枚举类
        LikeUnlikeNoteTypeEnum likeUnlikeNoteTypeEnum = LikeUnlikeNoteTypeEnum.valueOf(type);
        // 若枚举为空，跳到下一次循环
        if (Objects.isNull(likeUnlikeNoteTypeEnum)) {
          continue;
        }

        switch (likeUnlikeNoteTypeEnum) {
          case LIKE -> finalCount += 1; // 如果为点赞操作，点赞数+1
          case UNLIKE -> finalCount -= 1; // 如果为取消点赞，点赞数-1
        }
      }
      // 将分组后统计出的最终计数，存入countMap中
      countList.add(
          AggregationCountLikeUnlikeNoteMqDTO.builder()
              .noteId(noteId)
              .creatorId(creatorId)
              .count(finalCount)
              .build());
    }
    log.info("==》【笔记点赞数】聚合后的计数数据：{}", JsonUtils.toJsonString(countList));

    // 更新Redis
    countList.forEach(
        item -> {
          // 笔记发布者ID
          Long creatorId = item.getCreatorId();
          Long noteId = item.getNoteId();
          Integer count = item.getCount();

          // 笔记维度计数RedisKey
          String countNoteRedisKey = RedisKeyConstants.buildCountNoteKey(noteId);
          // 判断Redis中Hash是否存在
          boolean isCountNoteExisted = redisTemplate.hasKey(countNoteRedisKey);

          // 若存在才会更新
          // 因为缓存设置的有过期时间，考虑到过期后，缓存才会被删除，这里需要判断，存在才去更新，初始化工作在查询计数来做
          if (isCountNoteExisted) {
            // redis用户点赞计数
            redisTemplate
                .opsForHash()
                .increment(countNoteRedisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
          }

          // 更新redis用户维度计数
          String countUserRedisKey = RedisKeyConstants.buildCountUserKey(creatorId);
          boolean isCountUserExisted = redisTemplate.hasKey(countUserRedisKey);
          if (isCountUserExisted) {
            redisTemplate
                .opsForHash()
                .increment(countUserRedisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
          }
        });

    // 发送MQ，笔记点赞数落库
    Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countList)).build();
    // 异步发送MQ消息
    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==> 【计数服务：笔记点赞数入库】MQ 发送成功，SendResult: {}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==> 【计数服务：笔记点赞数入库】MQ 发送异常：", throwable);
          }
        });
  }
}
