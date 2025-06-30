package com.slilio.xiaohashu.note.biz.listener;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.note.biz.constant.MQConstants;
import com.slilio.xiaohashu.note.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.note.biz.convert.NoteConvert;
import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteDO;
import com.slilio.xiaohashu.note.biz.domain.mapper.NoteDOMapper;
import com.slilio.xiaohashu.note.biz.enums.NoteOperateEnum;
import com.slilio.xiaohashu.note.biz.model.dto.NoteOperateMqDTO;
import com.slilio.xiaohashu.note.biz.model.dto.PublishNoteDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @Author: slilio @CreateTime: 2025-06-30 @Description: @Version: 1.0
 */
@RocketMQTransactionListener
@Slf4j
public class PublishNote2DBLocalTransactionListener implements RocketMQLocalTransactionListener {
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private NoteDOMapper noteDOMapper;
  @Resource private RocketMQTemplate rocketMQTemplate;

  /**
   * 执行本地事务
   *
   * @param msg
   * @param arg
   * @return
   */
  @Override
  public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
    // 1.解析消息
    String payload = new String((byte[]) msg.getPayload());
    log.info("##事务消息：开始执行本地事务：{}", payload);

    // 消息体Json转DTO对象
    PublishNoteDTO publishNoteDTO = JsonUtils.parseObject(payload, PublishNoteDTO.class);
    Long noteId = publishNoteDTO.getId();
    Long creatorId = publishNoteDTO.getCreatorId();

    // 删除个人主页-已发布笔记列表缓存
    // todo 应采用灵活策略，大v应该直接更新缓存，而不是直接删除，普通用户则可以直接删除
    String publishedNoteListRedisKey = RedisKeyConstants.buildPublishedNoteListKey(creatorId);
    redisTemplate.delete(publishedNoteListRedisKey);

    // 2.执行本地事务（如数据库操作）
    try {
      // dto转Do
      NoteDO noteDO = NoteConvert.INSTANCE.convertDTO2DO(publishNoteDTO);
      // 笔记写数据库
      noteDOMapper.insert(noteDO);
    } catch (Exception e) {
      log.error("## 笔记元数据存储失败: ", e);
      return RocketMQLocalTransactionState.ROLLBACK; // 回滚事务消息
    }

    // 延迟双删：发送延迟消息
    sendDelayDeleteRedisPublishedNoteListCacheMQ(creatorId);

    // 发送MQ
    // 构建消息体DTO
    NoteOperateMqDTO noteOperateMqDTO =
        NoteOperateMqDTO.builder()
            .creatorId(creatorId)
            .noteId(noteId)
            .type(NoteOperateEnum.PUBLISH.getCode()) // 发布笔记
            .build();
    // 构建消息对象，并将DTO转成json字符串设置到消息体中
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(noteOperateMqDTO)).build();

    String destination = MQConstants.TOPIC_NOTE_OPERATE + ":" + MQConstants.TAG_NOTE_PUBLISH;
    // 发送MQ
    rocketMQTemplate.asyncSend(
        destination,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("===》【笔记发布】 MQ发送成功，SendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("===》【笔记发布】 MQ发送异常：", throwable);
          }
        });

    // 3.提交事务状态，half 消息转换为正式消息
    return RocketMQLocalTransactionState.COMMIT;
  }

  /**
   * 延迟双删：发送延迟消息
   *
   * @param userId
   */
  private void sendDelayDeleteRedisPublishedNoteListCacheMQ(Long userId) {
    Message<String> message = MessageBuilder.withPayload(String.valueOf(userId)).build();

    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_DELAY_DELETE_PUBLISHED_NOTE_LIST_REDIS_CACHE,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("## 延时删除 Redis 已发布笔记列表缓存消息发送成功...");
          }

          @Override
          public void onException(Throwable throwable) {
            log.error("## 延时删除 Redis 已发布笔记列表缓存消息发送失败...", throwable);
          }
        },
        3000, // 超时消息
        1 // 延迟级别，1表示1s
        );
  }

  /**
   * 事务状态回查（由Broker主动调用）
   *
   * @param msg
   * @return
   */
  @Override
  public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
    // 1.解析消息内容
    String payload = new String((byte[]) msg.getPayload());
    log.info("##事务消息：开始事务回查：{}", payload);

    // 消息体转换 json->dto
    PublishNoteDTO publishNoteDTO = JsonUtils.parseObject(payload, PublishNoteDTO.class);
    Long noteId = publishNoteDTO.getId();

    // 2.检查本地事务状态（若记录存在，说明本地事务执行成功了；否则执行失败）
    int count = noteDOMapper.selectCountByNoteId(noteId);

    // 3.返回最终状态（有记录则提交，否则失败）
    // 多次查询保证数据成功入库
    return count == 1
        ? RocketMQLocalTransactionState.COMMIT
        : RocketMQLocalTransactionState.ROLLBACK;
  }
}
