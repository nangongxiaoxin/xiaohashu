package com.slilio.xiaohashu.note.biz.consumer;

import com.slilio.xiaohashu.note.biz.constant.MQConstants;
import com.slilio.xiaohashu.note.biz.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group" + MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, // 组 group
    topic = MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, // 消费的主题 Topic
    messageModel = MessageModel.BROADCASTING // 广播模式
    )
public class DeleteNoteLocalCacheConsumer implements RocketMQListener<String> {
  @Resource NoteService noteService;

  @Override
  public void onMessage(String body) {
    Long noteId = Long.valueOf(body);
    log.info("## 消费者消费成功, noteId: {}", noteId);

    noteService.deleteNoteLocalCache(noteId);
  }
}
