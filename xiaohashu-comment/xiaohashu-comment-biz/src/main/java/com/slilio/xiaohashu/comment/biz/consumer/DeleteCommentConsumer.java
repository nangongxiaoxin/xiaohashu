package com.slilio.xiaohashu.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.comment.biz.constant.MQConstants;
import com.slilio.xiaohashu.comment.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.comment.biz.domain.dataobject.CommentDO;
import com.slilio.xiaohashu.comment.biz.domain.mapper.CommentDOMapper;
import com.slilio.xiaohashu.comment.biz.domain.mapper.NoteCountDOMapper;
import com.slilio.xiaohashu.comment.biz.enums.CommentLevelEnum;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-23 @Description: @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_DELETE_COMMENT, // 组
    topic = MQConstants.TOPIC_DELETE_COMMENT // topic
    )
@Slf4j
public class DeleteCommentConsumer implements RocketMQListener<String> {
  @Resource private CommentDOMapper commentDOMapper;
  @Resource private NoteCountDOMapper noteCountDOMapper;
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private RocketMQTemplate rocketMQTemplate;

  // 每秒创建1000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(1000);

  @Override
  public void onMessage(String body) {
    // 令牌流量控制
    rateLimiter.acquire();

    log.info("## 【删除评论 - 后续业务处理】消费者消费成功, body: {}", body);

    CommentDO commentDO = JsonUtils.parseObject(body, CommentDO.class);

    // 评论级别
    Integer level = commentDO.getLevel();
    CommentLevelEnum commentLevelEnum = CommentLevelEnum.valueOf(level);

    switch (commentLevelEnum) {
      // 一级评论
      case ONE -> handleOneLevelComment(commentDO);
      // 二级评论
      case TWO -> handleTwoLevelComment(commentDO);
    }
  }

  /**
   * 二级评论处理
   *
   * @param commentDO
   */
  private void handleTwoLevelComment(CommentDO commentDO) {
    Long commentId = commentDO.getId();

    // 1. 批量删除关联评论（递归查询回复评论，并批量删除）

    List<Long> replyCommentIds = Lists.newArrayList();
    recurrentGetReplyCommentId(replyCommentIds, commentId);
    // 被删除的行数
    int count = 0;
    if (CollUtil.isNotEmpty(replyCommentIds)) {
      count = commentDOMapper.deleteByIds(replyCommentIds);
    }

    // 2. 更新一级评论的计数
    Long parentCommentId = commentDO.getParentId();
    String redisKey = RedisKeyConstants.buildCountCommentKey(parentCommentId);

    boolean hasKey = redisTemplate.hasKey(redisKey);
    if (hasKey) {
      redisTemplate
          .opsForHash()
          .increment(redisKey, RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, -(count + 1));
    }

    // 3. 若是最早的发布的二级评论被删除，需要更新一级评论的 first_reply_comment_id

    // 查询一级评论
    CommentDO oneLevelCommentDO = commentDOMapper.selectByPrimaryKey(parentCommentId);
    Long firstReplyCommentId = oneLevelCommentDO.getFirstReplyCommentId();

    // 若删除的是最早回复的二级评论
    if (Objects.equals(firstReplyCommentId, commentId)) {
      // 查询数据库，重新获取一级评论最早回复的评论
      CommentDO earliestCommentDO =
          commentDOMapper.selectEarliestCommentByParentId(parentCommentId);

      // 最早回复的那条评论ID。若查询结果为null，则最早回复的评论ID为null
      Long earliestCommentId =
          Objects.nonNull(earliestCommentDO) ? earliestCommentDO.getId() : null;
      // 更新其一级评论的first_reply_comment_id
      commentDOMapper.updateFirstReplyCommentIdByPrimaryKey(earliestCommentId, parentCommentId);
    }

    // 4. 重新计算一级评论的热度值
    Set<Long> commentIds = Sets.newHashSetWithExpectedSize(1);
    commentIds.add(parentCommentId);

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

  /**
   * 递归获取全部回复的评论ID
   *
   * @param replyCommentIds
   * @param commentId
   */
  private void recurrentGetReplyCommentId(List<Long> commentIds, Long commentId) {
    CommentDO replyCommentDO = commentDOMapper.selectByReplyCommentId(commentId);

    if (Objects.isNull(replyCommentDO)) {
      return;
    }

    commentIds.add(replyCommentDO.getId());
    Long replyCommentId = replyCommentDO.getId();
    // 递归调用
    recurrentGetReplyCommentId(commentIds, replyCommentId);
  }

  /**
   * 一级评论处理
   *
   * @param commentDO
   */
  private void handleOneLevelComment(CommentDO commentDO) {
    Long commentId = commentDO.getId();
    Long noteId = commentDO.getNoteId();

    // 1. 关联评论删除（一级评论下所有子评论，都需要删除）
    int count = commentDOMapper.deleteByParentId(commentId);

    // 2. 计数更新（笔记下总评论数）

    // 更新Redis
    String redisKey = RedisKeyConstants.buildNoteCommentTotalKey(noteId);
    boolean hasKey = redisTemplate.hasKey(redisKey);

    if (hasKey) {
      // 笔记评论总数-1
      redisTemplate
          .opsForHash()
          .increment(redisKey, RedisKeyConstants.FIELD_COMMENT_TOTAL, -(count + 1));
    }

    // 更新t_note_count计数表
    noteCountDOMapper.updateCommentTotalByNoteId(noteId, -(count + 1));
  }
}
