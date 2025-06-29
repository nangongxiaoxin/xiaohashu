package com.slilio.xiaohashu.note.biz.consumer;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.note.biz.constant.MQConstants;
import com.slilio.xiaohashu.note.biz.constant.RedisKeyConstants;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE, // group
    topic = MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE // 消费的主题Topic
    )
public class DelayDeleteNoteRedisCacheConsumer implements RocketMQListener<String> {
  @Resource private RedisTemplate<String, Object> redisTemplate;

  @Override
  public void onMessage(String body) {
    try {
      List<Long> noteIdAndUserId = JsonUtils.parseList(body, Long.class);
      Long noteId = noteIdAndUserId.get(0);
      Long userId = noteIdAndUserId.get(1);
      log.info("## 延迟消息消费成功, noteId: {}, userId: {}", noteId, userId);

      // 删除Redis笔记缓存
      String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
      String publishedNoteListRedisKey = RedisKeyConstants.buildPublishedNoteListKey(userId);
      redisTemplate.delete(Arrays.asList(noteDetailRedisKey, publishedNoteListRedisKey));
    } catch (Exception e) {
      log.error("", e);
    }
  }
}
