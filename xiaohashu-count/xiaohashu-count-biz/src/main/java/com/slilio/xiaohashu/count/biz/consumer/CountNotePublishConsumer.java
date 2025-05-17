package com.slilio.xiaohashu.count.biz.consumer;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.count.biz.constant.MQConstants;
import com.slilio.xiaohashu.count.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.count.biz.domain.mapper.UserCountDOMapper;
import com.slilio.xiaohashu.count.biz.model.dto.NoteOperateMqDTO;
import jakarta.annotation.Resource;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-18 @Description: 文章发布、删除消费 @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_NOTE_OPERATE,
    topic = MQConstants.TOPIC_NOTE_OPERATE)
@Slf4j
public class CountNotePublishConsumer implements RocketMQListener<Message> {
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private UserCountDOMapper userCountDOMapper;

  @Override
  public void onMessage(Message message) {
    // 消息体
    String bodyJsonStr = new String(message.getBody());
    // 标签
    String tags = message.getTags();

    log.info("==》 CountNotePublishConsumer 消费了消息 {}，tags: {}", bodyJsonStr, tags);

    // 根据MQ标签，判断操作类型
    if (Objects.equals(tags, MQConstants.TAG_NOTE_NOTE_PUBLISH)) { // 发布笔记
      handleTagMessage(bodyJsonStr, 1);
    } else if (Objects.equals(tags, MQConstants.TAG_NOTE_NOTE_DELETE)) { // 删除笔记
      handleTagMessage(bodyJsonStr, -1);
    }
  }

  /**
   * 删除笔记、发布笔记
   *
   * @param bodyJsonStr
   */
  private void handleTagMessage(String bodyJsonStr, long count) {
    // 消息体json转DTO
    NoteOperateMqDTO noteOperateMqDTO = JsonUtils.parseObject(bodyJsonStr, NoteOperateMqDTO.class);

    if (Objects.isNull(noteOperateMqDTO)) {
      return;
    }

    // 笔记发布者ID
    Long creatorId = noteOperateMqDTO.getCreatorId();

    // 更新redis中用户维度的计数hash
    String countUserRedisKey = RedisKeyConstants.buildCountUserKey(creatorId);
    // 判断Redis中hash是否存在
    boolean isCountUserExisted = redisTemplate.hasKey(countUserRedisKey);

    // 若存在，则更新
    // (因为缓存设有过期时间，考虑到过期后，缓存会被删除，这里需要判断一下，存在才会去更新，而初始化工作放在查询计数来做)
    if (isCountUserExisted) {
      // 更新目标用户的Hash的笔记发布总数
      redisTemplate
          .opsForHash()
          .increment(countUserRedisKey, RedisKeyConstants.FIELD_NOTE_TOTAL, count);
    }

    // 更新t_user_count表
    userCountDOMapper.insertOrUpdateNoteTotalByUserId(count, creatorId);
  }
}
