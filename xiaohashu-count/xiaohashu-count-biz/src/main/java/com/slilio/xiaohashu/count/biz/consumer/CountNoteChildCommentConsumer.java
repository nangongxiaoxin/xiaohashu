package com.slilio.xiaohashu.count.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.github.phantomthief.collection.BufferTrigger;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.count.biz.domain.mapper.CommentDOMapper;
import com.slilio.xiaohashu.count.biz.enums.CommentLevelEnum;
import com.slilio.xiaohashu.count.biz.model.dto.CountPublishCommentMqDTO;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.assertj.core.util.Lists;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-07 @Description: @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup =
        "xiaohashu_group_child_comment_total" + MQConstants.TOPIC_COUNT_NOTE_COMMENT, // group组
    topic = MQConstants.TOPIC_COUNT_NOTE_COMMENT // 主题topic
    )
@Slf4j
public class CountNoteChildCommentConsumer implements RocketMQListener<String> {

  @Resource private RocketMQTemplate rocketMQTemplate;
  @Resource private CommentDOMapper commentDOMapper;

  private BufferTrigger<String> bufferTrigger =
      BufferTrigger.<String>batchBlocking()
          .bufferSize(50000) // 缓存队列的最大容量
          .batchSize(1000) // 一批次最多聚合1000条
          .linger(Duration.ofSeconds(1)) // 多久聚合一次
          .setConsumerEx(this::consumeMessage) // 消费函数
          .build();
  private RedisTemplate<Object, Object> redisTemplate;

  public CountNoteChildCommentConsumer(CommentDOMapper commentDOMapper) {}

  @Override
  public void onMessage(String body) {
    // 往bufferTrigger中添加元素
    bufferTrigger.enqueue(body);
  }

  private void consumeMessage(List<String> bodys) {

    log.info("==> 【笔记二级评论数】聚合消息, size: {}", bodys.size());
    log.info("==> 【笔记二级评论数】聚合消息, {}", JsonUtils.toJsonString(bodys));

    // 将聚合后的消息体Json转List<CountPublishCommentMqDTO>
    List<CountPublishCommentMqDTO> countPublishCommentMqDTOList = Lists.newArrayList();
    bodys.forEach(
        body -> {
          try {
            List<CountPublishCommentMqDTO> list =
                JsonUtils.parseList(body, CountPublishCommentMqDTO.class);
            countPublishCommentMqDTOList.addAll(list);
          } catch (Exception e) {
            log.error("", e);
          }
        });

    // 过滤出二级评论，并按parent_id分组
    Map<Long, List<CountPublishCommentMqDTO>> groupMap =
        countPublishCommentMqDTOList.stream()
            .filter(
                commentMqDTO ->
                    Objects.equals(CommentLevelEnum.TWO.getCode(), commentMqDTO.getLevel()))
            .collect(
                Collectors.groupingBy(CountPublishCommentMqDTO::getParentId)); // 按 parent_id 分组

    // 若无二级评论。则直接return
    if (CollUtil.isEmpty(groupMap)) {
      return;
    }

    // 循环分组字典
    for (Map.Entry<Long, List<CountPublishCommentMqDTO>> entry : groupMap.entrySet()) {
      // 一级评论id
      Long parentId = entry.getKey();
      // 评论数
      int count = CollUtil.size(entry.getValue());

      // 更新Redis缓存中的评论计数数据
      // 构建Key
      String commentCountHashKey = RedisKeyConstants.buildCountCommentKey(parentId);
      // 判断Hash存在，则更新子评论总数
      boolean hasKey = redisTemplate.hasKey(commentCountHashKey);
      if (hasKey) {
        // 累加
        redisTemplate
            .opsForHash()
            .increment(commentCountHashKey, RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, count);
      }

      // 更新一级评论下的评论总数，进行累加操作
      commentDOMapper.updateChildCommentTotal(parentId, count);

      // 更新一级评论下的评论总数，进行累加操作
      commentDOMapper.updateChildCommentTotal(parentId, count);
    }

    // 获取字典中的评论ID
    Set<Long> commentIds = groupMap.keySet();

    // 异步发送计数MQ，更新评论热度值
    org.springframework.messaging.Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(commentIds)).build();

    // 异步发送MQ消息
    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_COMMENT_HEAT_UPDATE,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==> 【评论热度值更新】MQ 发送成功，SendResult: {}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==> 【评论热度值更新】MQ 发送异常：", throwable);
          }
        });
  }
}
