package com.slilio.xiaohashu.kv.biz.consumer;

import com.alibaba.fastjson.JSON;
import com.slilio.xiaohashu.kv.biz.constants.MQConstants;
import com.slilio.xiaohashu.kv.biz.model.dto.PublishNoteDTO;
import com.slilio.xiaohashu.kv.biz.service.NoteContentService;
import com.slilio.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-07-01 @Description: @Version: 1.0
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_PUBLISH_NOTE_TRANSACTION,
    topic = MQConstants.TOPIC_PUBLISH_NOTE_TRANSACTION)
@Slf4j
public class SaveNoteContentConsumer implements RocketMQListener<Message> {
  @Resource private NoteContentService noteContentService;

  @Override
  public void onMessage(Message body) {
    String bodyJsonStr = new String(body.getBody());
    log.info("## SaveNoteContentConsumer 消费了事务消息 {}", bodyJsonStr);

    // 笔记保存到Cassandra
    if (StringUtils.isNotBlank(bodyJsonStr)) {
      PublishNoteDTO publishNoteDTO = JSON.parseObject(bodyJsonStr, PublishNoteDTO.class);
      String content = publishNoteDTO.getContent();
      String uuid = publishNoteDTO.getContentUuid();

      // 写入
      noteContentService.addNoteContent(
          AddNoteContentReqDTO.builder().uuid(uuid).content(content).build());
    }
  }
}
