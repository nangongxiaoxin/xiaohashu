package com.slilio.xiaohashu.note.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.note.biz.constant.MQConstants;
import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteCollectionDO;
import com.slilio.xiaohashu.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.slilio.xiaohashu.note.biz.model.dto.CollectUnCollectNoteMqDTO;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-11 @Description: 收藏、取消收藏消费者类 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_COLLECT_OR_UN_COLLECT, // group
    topic = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT, // 消费者的Topic
    consumeMode = ConsumeMode.ORDERLY // 设置为顺序消费模式
    )
@Slf4j
public class CollectUnCollectNoteConsumer implements RocketMQListener<Message> {
  private final NoteCollectionDOMapper noteCollectionDOMapper;
  // 每秒创建5000个令牌
  private RateLimiter rateLimiter = RateLimiter.create(5000);

  public CollectUnCollectNoteConsumer(NoteCollectionDOMapper noteCollectionDOMapper) {
    this.noteCollectionDOMapper = noteCollectionDOMapper;
  }

  @Override
  public void onMessage(Message message) {
    // 流量削峰 通过获取令牌，如果没有令牌可用，将阻塞，直到获得
    rateLimiter.acquire();

    // 幂等性：通过联合唯一索引保证

    // 消息体
    String bodyJsonStr = new String(message.getBody());
    // 标签
    String tags = message.getTags();

    log.info("==> CollectUnCollectNoteConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);

    // 根据MQ标签，判断操作类型
    if (Objects.equals(tags, MQConstants.TAG_COLLECT)) {
      // 收藏笔记
      handleCollectNoteTagMessage(bodyJsonStr);
    } else if (Objects.equals(tags, MQConstants.TAG_UN_COLLECT)) {
      // 取消收藏笔记
      handleUnCollectNoteTagMessage(bodyJsonStr);
    }
  }

  /**
   * 笔记收藏
   *
   * @param bodyJsonStr
   */
  private void handleCollectNoteTagMessage(String bodyJsonStr) {
    // 消息体Json字符串转DTO
    CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO =
        JsonUtils.parseObject(bodyJsonStr, CollectUnCollectNoteMqDTO.class);

    if (Objects.isNull(collectUnCollectNoteMqDTO)) {
      return;
    }

    // 用户ID
    Long userId = collectUnCollectNoteMqDTO.getUserId();
    // 收藏的笔记ID
    Long noteId = collectUnCollectNoteMqDTO.getNoteId();
    // 操作类型
    Integer type = collectUnCollectNoteMqDTO.getType();
    // 收藏时间
    LocalDateTime createTime = collectUnCollectNoteMqDTO.getCreateTime();

    // 构建DO对象
    NoteCollectionDO noteCollectionDO =
        NoteCollectionDO.builder()
            .userId(userId)
            .noteId(noteId)
            .createTime(createTime)
            .status(type)
            .build();

    // 添加或者更新笔记收藏记录
    int count = noteCollectionDOMapper.insertOrUpdate(noteCollectionDO);

    // todo：发送计数MQ
  }

  /**
   * 笔记取消收藏
   *
   * @param bodyJsonStr
   */
  private void handleUnCollectNoteTagMessage(String bodyJsonStr) {
    // 消息体JSON字符串转DTO
    CollectUnCollectNoteMqDTO unCollectNoteMqDTO =
        JsonUtils.parseObject(bodyJsonStr, CollectUnCollectNoteMqDTO.class);

    if (Objects.isNull(unCollectNoteMqDTO)) {
      return;
    }

    // 用户ID
    Long userId = unCollectNoteMqDTO.getUserId();
    // 收藏的笔记ID
    Long noteId = unCollectNoteMqDTO.getNoteId();
    // 操作的类型
    Integer type = unCollectNoteMqDTO.getType();
    // 收藏时间
    LocalDateTime createTime = unCollectNoteMqDTO.getCreateTime();

    // 构建DO对象
    NoteCollectionDO noteCollectionDO =
        NoteCollectionDO.builder()
            .userId(userId)
            .noteId(noteId)
            .createTime(createTime)
            .status(type)
            .build();
    int count = noteCollectionDOMapper.update2UnCollectByUserIdAndNoteId(noteCollectionDO);

    // todo: 发送计数MQ
  }
}
