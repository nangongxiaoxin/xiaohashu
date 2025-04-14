package com.slilio.xiaohashu.note.biz.consumer;

import com.slilio.xiaohashu.note.biz.constant.MQConstants;
import com.slilio.xiaohashu.note.biz.constant.RedisKeyConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE, // chache
    topic = MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE // 消费的主题Topic
    )
public class DelayDeleteNoteRedisCacheConsumer implements RocketMQListener<String> {
  @Resource private RedisTemplate<String, Object> redisTemplate;

  @Override
  public void onMessage(String body) {
    Long noteId = Long.valueOf(body);
    log.info("## 延时消息消费成功，noteId：{}", noteId);

    // 删除Redis笔记缓存
    String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
    redisTemplate.delete(noteDetailRedisKey);
  }
}
