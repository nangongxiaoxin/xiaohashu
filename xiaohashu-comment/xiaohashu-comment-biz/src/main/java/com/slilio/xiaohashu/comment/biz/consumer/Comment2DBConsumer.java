package com.slilio.xiaohashu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.comment.biz.constant.MQConstants;
import com.slilio.xiaohashu.comment.biz.domain.dataobject.CommentDO;
import com.slilio.xiaohashu.comment.biz.domain.mapper.CommentDOMapper;
import com.slilio.xiaohashu.comment.biz.enums.CommentLevelEnum;
import com.slilio.xiaohashu.comment.biz.model.bo.CommentBO;
import com.slilio.xiaohashu.comment.biz.model.dto.CountPublishCommentMqDTO;
import com.slilio.xiaohashu.comment.biz.model.dto.PublishCommentMqDTO;
import com.slilio.xiaohashu.comment.biz.rpc.KeyValueRpcService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: 评论入库消费者 @Version: 1.0
 */
@Component
@Slf4j
public class Comment2DBConsumer {
  @Value("${rocketmq.name-server}")
  private String namesrvAddr;

  @Resource private TransactionTemplate transactionTemplate;
  @Resource private KeyValueRpcService keyValueRpcService;

  private DefaultMQPushConsumer consumer;

  // 每秒创建1000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(1000);

  @Bean
  public DefaultMQPushConsumer mqPushConsumer(
      CommentDOMapper commentDOMapper, RocketMQTemplate rocketMQTemplate) throws MQClientException {
    // group组
    String group = "xiaohashu_group_" + MQConstants.TOPIC_PUBLISH_COMMENT;

    // 创建一个新的DefaultMQPublishConsumer实例，并指定消费者的消费组名
    consumer = new DefaultMQPushConsumer(group);

    // 设置rocketMQ的NameServer地址
    consumer.setNamesrvAddr(namesrvAddr);

    // 订阅指定的主题，并指定主题的订阅规则（“*”表示订阅所有标签的消息）
    consumer.subscribe(MQConstants.TOPIC_PUBLISH_COMMENT, "*");

    // 设置消费者消费消息的起始位置，如果队列中没有消息，则从最新的消息开始消费
    consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

    // 设置消息的消费模式，这里使用集群模式（clustering）
    consumer.setMessageModel(MessageModel.CLUSTERING);

    // 设置每批次的最大消费数量消息，这里设置为30，表示每次拉取时最多30条消息
    consumer.setConsumeMessageBatchMaxSize(30);

    // 注册消息监听器
    consumer.registerMessageListener(
        (MessageListenerConcurrently)
            (msgs, context) -> {
              log.info("==> 本批次消息大小：{}", msgs.size());
              try {
                // 令牌桶流量控制
                rateLimiter.acquire();

                // 消息体Json字符串转DTO
                List<PublishCommentMqDTO> publishCommentMqDTOS = Lists.newArrayList();
                msgs.forEach(
                    msg -> {
                      String msgJson = new String(msg.getBody());
                      log.info("==> Consumer - Received message: {}", msgJson);
                      publishCommentMqDTOS.add(
                          JsonUtils.parseObject(msgJson, PublishCommentMqDTO.class));
                    });
                // 提取所有不为空的回复评论ID 二级评论
                List<Long> replyCommentIds =
                    publishCommentMqDTOS.stream()
                        .filter(
                            publishCommentMqDTO ->
                                Objects.nonNull(publishCommentMqDTO.getReplyCommentId()))
                        .map(PublishCommentMqDTO::getReplyCommentId)
                        .toList();

                // 批量查询相关回复评论记录
                List<CommentDO> replyCommentDOS = null;
                if (CollUtil.isNotEmpty(replyCommentIds)) {
                  // 查询数据库
                  replyCommentDOS = commentDOMapper.selectByCommentIds(replyCommentIds);
                }

                // DO集合转<评论ID，评论DO>字典，方便后续查找
                Map<Long, CommentDO> commentIdAndCommentDOMap = Maps.newHashMap();
                if (CollUtil.isNotEmpty(replyCommentDOS)) {
                  commentIdAndCommentDOMap =
                      replyCommentDOS.stream()
                          .collect(Collectors.toMap(CommentDO::getId, commentDO -> commentDO));
                }

                // DTO转BO
                List<CommentBO> commentBOS = Lists.newArrayList();
                for (PublishCommentMqDTO publishCommentMqDTO : publishCommentMqDTOS) {
                  String imageUrl = publishCommentMqDTO.getImageUrl();
                  CommentBO commentBO =
                      CommentBO.builder()
                          .id(publishCommentMqDTO.getCommentId())
                          .noteId(publishCommentMqDTO.getNoteId())
                          .userId(publishCommentMqDTO.getCreatorId())
                          .isContentEmpty(true) // 默认评论内容为空
                          .imageUrl(StringUtils.isBlank(imageUrl) ? "" : imageUrl)
                          .level(CommentLevelEnum.ONE.getCode()) // 默认为一级评论
                          .parentId(publishCommentMqDTO.getNoteId()) // 默认设置为所属笔记 ID
                          .createTime(publishCommentMqDTO.getCreateTime())
                          .updateTime(publishCommentMqDTO.getCreateTime())
                          .isTop(false)
                          .replyTotal(0L)
                          .likeTotal(0L)
                          .replyCommentId(0L)
                          .replyUserId(0L)
                          .build();

                  // 评论内容如果不为空
                  String content = publishCommentMqDTO.getContent();
                  if (StringUtils.isNotBlank(content)) {
                    commentBO.setContentUuid(UUID.randomUUID().toString());
                    commentBO.setIsContentEmpty(false);
                    commentBO.setContent(content);
                  }

                  // 设置评论级别，回复用户Id（reply_user_id）父评论ID（parent_id）
                  Long replyCommentId = publishCommentMqDTO.getReplyCommentId();
                  if (Objects.nonNull(replyCommentId)) {
                    CommentDO replyCommentDO =
                        commentIdAndCommentDOMap.get(replyCommentId); // 从上文Map中获取 父级评论

                    if (Objects.nonNull(replyCommentDO)) {
                      // 若回复的评论ID不为空，那说明是二级评论
                      commentBO.setLevel(CommentLevelEnum.TWO.getCode());
                      commentBO.setReplyCommentId(publishCommentMqDTO.getReplyCommentId());

                      // 父级评论ID
                      commentBO.setParentId(replyCommentDO.getId());
                      // 如果回复的评论属于二级评论
                      if (Objects.equals(
                          replyCommentDO.getLevel(), CommentLevelEnum.TWO.getCode())) {
                        commentBO.setParentId(replyCommentDO.getParentId());
                      }
                      // 回复用户ID
                      commentBO.setReplyUserId(replyCommentDO.getUserId());
                    }
                  }
                  commentBOS.add(commentBO);
                }
                log.info("## 清洗后的 CommentBOS: {}", JsonUtils.toJsonString(commentBOS));

                // 编程事务，保证整体操作的原子性
                Integer insertedRows =
                    transactionTemplate.execute(
                        status -> {
                          try {
                            // 先批量存入评论元数据到mysql数据库
                            int count = commentDOMapper.batchInsert(commentBOS);

                            // 过滤出评论内容不为空的BO
                            List<CommentBO> commentContentNotEmptyBOS =
                                commentBOS.stream()
                                    .filter(
                                        commentBO ->
                                            Boolean.FALSE.equals(commentBO.getIsContentEmpty()))
                                    .toList();

                            if (CollUtil.isNotEmpty(commentContentNotEmptyBOS)) {
                              // 批量存储评论到cassandra中
                              keyValueRpcService.batchSaveCommentContent(commentContentNotEmptyBOS);
                            }

                            return count;
                          } catch (Exception ex) {
                            status.setRollbackOnly();
                            log.error("", ex);
                            throw ex;
                          }
                        });

                // 如果批量插入的行数大于0
                if (Objects.nonNull(insertedRows) && insertedRows > 0) {
                  // 构建发送给计数服务的DTO集合
                  List<CountPublishCommentMqDTO> countPublishCommentMqDTOS =
                      commentBOS.stream()
                          .map(
                              commentBO ->
                                  CountPublishCommentMqDTO.builder()
                                      .noteId(commentBO.getNoteId())
                                      .commentId(commentBO.getId())
                                      .level(commentBO.getLevel())
                                      .parentId(commentBO.getParentId())
                                      .build())
                          .toList();

                  // 异步发送计数MQ
                  org.springframework.messaging.Message<String> message =
                      MessageBuilder.withPayload(JsonUtils.toJsonString(countPublishCommentMqDTOS))
                          .build();
                  // 异步发送MQ消息
                  rocketMQTemplate.asyncSend(
                      MQConstants.TOPIC_COUNT_NOTE_COMMENT,
                      message,
                      new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                          log.info("==》 【计数：评论发布】MQ发送成功，SendResult：{}", sendResult);
                        }

                        @Override
                        public void onException(Throwable throwable) {
                          log.info("==》 【计数：评论发布】MQ发送异常：", throwable);
                        }
                      });
                }

                // 手动ACK，告诉RocketMQ这批消息已被成功消费
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
              } catch (Exception e) {
                log.error("", e);
                // 手动ACK，告诉RocketMQ这批消息消费失败，稍后进行重试
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
              }
            });

    // 启动消费者
    consumer.start();
    return consumer;
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
