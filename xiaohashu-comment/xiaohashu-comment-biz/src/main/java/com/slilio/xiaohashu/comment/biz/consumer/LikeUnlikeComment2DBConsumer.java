package com.slilio.xiaohashu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.google.common.collect.Lists;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.comment.biz.constant.MQConstants;
import com.slilio.xiaohashu.comment.biz.domain.mapper.CommentLikeDOMapper;
import com.slilio.xiaohashu.comment.biz.enums.LikeUnlikeCommentTypeEnum;
import com.slilio.xiaohashu.comment.biz.model.dto.LikeUnlikeCommentMqDTO;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-22 @Description: 批量MQ消费 @Version: 1.0
 */
@Component
@Slf4j
public class LikeUnlikeComment2DBConsumer {
  private final CommentLikeDOMapper commentLikeDOMapper;

  @Value("${rocketmq.name-server}")
  private String namesrvAddr;

  private DefaultMQPushConsumer consumer;

  // 每秒创建5000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(5000);

  public LikeUnlikeComment2DBConsumer(CommentLikeDOMapper commentLikeDOMapper) {
    this.commentLikeDOMapper = commentLikeDOMapper;
  }

  @Bean(name = "LikeUnlikeComment2DBConsumer")
  public DefaultMQPushConsumer mqPushConsumer() throws MQClientException {
    // group组
    String group = "xiaohashu_group_" + MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE;
    // 创建一个新的DefaultMQPublishConsumer实例，并指定消费者的消费组名
    consumer = new DefaultMQPushConsumer(group);
    // 设置Rocket的nameServer地址
    consumer.setNamesrvAddr(namesrvAddr);
    // 订阅指定的主题，并设置主题的订阅规则（*表示订阅所有标签的信息）
    consumer.subscribe(MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE, "*");
    // 设置消费者消费的起始位置，如果队列里没有消息，则从最新的消息开始消费
    consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
    // 设置消费模式，此处使用集群模式
    consumer.setMessageModel(MessageModel.CLUSTERING);
    // 最大重试次数，以防止消息重试次数过多此仍然没有成功，避免消息卡在消费队列中
    consumer.setMaxReconsumeTimes(3);
    // 设置每批次消费的最大消息数量，这里设置为30，表示每次拉取最多消费30条消息
    consumer.setConsumeMessageBatchMaxSize(30);

    // 注册消息监听器
    consumer.registerMessageListener(
        (MessageListenerOrderly)
            (msgs, context) -> {
              log.info("==》【评论点赞、取消点赞】本批次消息大小：{}", msgs.size());
              try {
                // 令牌桶流量控制，以控制数据库能够承受的QPS
                rateLimiter.acquire();

                // 将批次Json转换为DTO集合
                List<LikeUnlikeCommentMqDTO> likeUnlikeCommentMqDTOS = Lists.newArrayList();

                msgs.forEach(
                    msg -> {
                      String tag = msg.getTags();
                      String msgJson = new String(msg.getBody());
                      log.info("==》【评论点赞、取消点赞】Consumer-Tag：{},Received message:{}", tag, msgJson);

                      // json转DTO
                      likeUnlikeCommentMqDTOS.add(
                          JsonUtils.parseObject(msgJson, LikeUnlikeCommentMqDTO.class));
                    });

                // 按照评论ID分组
                Map<Long, List<LikeUnlikeCommentMqDTO>> commentIdAndListMap =
                    likeUnlikeCommentMqDTOS.stream()
                        .collect(Collectors.groupingBy(LikeUnlikeCommentMqDTO::getCommentId));

                List<LikeUnlikeCommentMqDTO> finalLikeUnlikeCommentMqDTOS = Lists.newArrayList();
                commentIdAndListMap.forEach(
                    (commentId, ops) -> {
                      // 优化：多次操作归并
                      Map<Long, LikeUnlikeCommentMqDTO> userLastOp =
                          ops.stream()
                              .collect(
                                  Collectors.toMap(
                                      LikeUnlikeCommentMqDTO::getUserId, // 以发布评论用户的ID作为Map键
                                      Function.identity(), // 直接使用DTO对象本身作为Map值
                                      // 合并策略：当出现重复键（同一个用户多次操作）时，保留时间更晚的记录
                                      (oldValue, newValue) ->
                                          oldValue.getCreateTime().isAfter(newValue.getCreateTime())
                                              ? oldValue
                                              : newValue));

                      finalLikeUnlikeCommentMqDTOS.addAll(userLastOp.values());
                    });

                // 批量操作数据库
                executeBatchSQL(finalLikeUnlikeCommentMqDTOS);

                // 手动ACK，告诉RocketMQ这批次消息消费成功
                return ConsumeOrderlyStatus.SUCCESS;
              } catch (Exception e) {
                log.error("", e);
                // 这样RocketMq会暂停当前队列的消费一段时间，再进行重试
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
              }
            });

    // 启动消费者
    consumer.start();
    return consumer;
  }

  /**
   * 批量操作数据库
   *
   * @param values
   */
  private void executeBatchSQL(List<LikeUnlikeCommentMqDTO> values) {
    // 过滤出点赞操作
    List<LikeUnlikeCommentMqDTO> likes =
        values.stream()
            .filter(op -> Objects.equals(op.getType(), LikeUnlikeCommentTypeEnum.LIKE.getCode()))
            .toList();

    // 过滤出取消点赞的操作
    List<LikeUnlikeCommentMqDTO> unlikes =
        values.stream()
            .filter(op -> Objects.equals(op.getType(), LikeUnlikeCommentTypeEnum.UNLIKE.getCode()))
            .toList();

    // 取消点赞：批量删除
    if (CollUtil.isNotEmpty(unlikes)) {
      commentLikeDOMapper.batchDelete(unlikes);
    }

    // 点赞：批量新增
    if (CollUtil.isNotEmpty(likes)) {
      commentLikeDOMapper.batchInsert(likes);
    }
  }

  @PreDestroy
  public void destroy() {
    if (Objects.nonNull(consumer)) {
      try {
        consumer.shutdown(); // 关闭消费者
      } catch (Exception e) {
        log.error("", e);
      }
    }
  }
}
