package com.slilio.xiaohashu.note.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.note.biz.constant.MQConstants;
import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteLikeDO;
import com.slilio.xiaohashu.note.biz.domain.mapper.NoteLikeDOMapper;
import com.slilio.xiaohashu.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-05 @Description: 点赞、取消点赞消费者 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_LIKE_OR_UNLIKE, // group组
    topic = MQConstants.TOPIC_LIKE_OR_UNLIKE, // 消费者的主题Topic
    consumeMode = ConsumeMode.ORDERLY // 设置为顺序消费模式
    )
@Slf4j
public class LikeUnlikeNoteConsumer implements RocketMQListener<Message> {
  private final NoteLikeDOMapper noteLikeDOMapper;
  // 每秒创建5000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(5000);

  public LikeUnlikeNoteConsumer(NoteLikeDOMapper noteLikeDOMapper) {
    this.noteLikeDOMapper = noteLikeDOMapper;
  }

  @Override
  public void onMessage(Message message) {
    // 流量削峰
    rateLimiter.acquire();

    // 幂等性：通过联合唯一索引保证

    // 消息体
    String bodyJsonStr = new String(message.getBody());
    // 标签
    String tags = message.getTags();

    log.info("==》 LikeUnlikeNoteConsumer 消费了消息 {} ,tags: {}", bodyJsonStr, tags);

    // 根据MQ标签，判断操作类型
    if (Objects.equals(tags, MQConstants.TAG_LIKE)) {
      // 点赞笔记
      handleLikeNoteTagMessage(bodyJsonStr);
    } else if (Objects.equals(tags, MQConstants.TAG_UNLIKE)) {
      handleUnlikeNoteTagMessage(bodyJsonStr);
    }
  }

  /**
   * 笔记点赞
   *
   * @param bodyJsonStr
   */
  private void handleLikeNoteTagMessage(String bodyJsonStr) {
    // 消息体json字符串转DTO
    LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO =
        JsonUtils.parseObject(bodyJsonStr, LikeUnlikeNoteMqDTO.class);

    if (Objects.isNull(likeUnlikeNoteMqDTO)) {
      return;
    }

    // 用户ID
    Long userId = likeUnlikeNoteMqDTO.getUserId();
    // 点赞的笔记ID
    Long noteId = likeUnlikeNoteMqDTO.getNoteId();
    // 操作类型
    Integer type = likeUnlikeNoteMqDTO.getType();
    // 操作时间
    LocalDateTime createTime = likeUnlikeNoteMqDTO.getCreateTime();

    // 构建DO对象
    NoteLikeDO noteLikeDO =
        NoteLikeDO.builder()
            .userId(userId)
            .noteId(noteId)
            .createTime(createTime)
            .status(type)
            .build();

    // 添加点赞或更新笔记点赞记录
    int count = noteLikeDOMapper.insertOrUpdate(noteLikeDO);

    // todo：发送计数MQ
  }

  /**
   * 笔记取消点赞
   *
   * @param bodyJsonStr
   */
  private void handleUnlikeNoteTagMessage(String bodyJsonStr) {}
}
