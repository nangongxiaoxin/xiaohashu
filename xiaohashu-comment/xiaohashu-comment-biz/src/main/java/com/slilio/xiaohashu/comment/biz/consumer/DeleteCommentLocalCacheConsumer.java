package com.slilio.xiaohashu.comment.biz.consumer;

import com.slilio.xiaohashu.comment.biz.constant.MQConstants;
import com.slilio.xiaohashu.comment.biz.service.CommentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-23 @Description: @Version: 1.0
 */
@Component
@Slf4j
@RocketMQMessageListener(
    consumerGroup = "xiaohashu_group_" + MQConstants.TOPIC_DELETE_COMMENT_LOCAL_CACHE, // Group
    topic = MQConstants.TOPIC_DELETE_COMMENT_LOCAL_CACHE, // 主题topic
    messageModel = MessageModel.BROADCASTING // 广播模式
    )
public class DeleteCommentLocalCacheConsumer implements RocketMQListener<String> {
  @Resource CommentService commentService;

  @Override
  public void onMessage(String body) {
    Long commentId = Long.valueOf(body);
    log.info("## 消费者消费成功，commentId：{}", commentId);

    commentService.deleteCommentLocalCache(commentId);
  }
}
