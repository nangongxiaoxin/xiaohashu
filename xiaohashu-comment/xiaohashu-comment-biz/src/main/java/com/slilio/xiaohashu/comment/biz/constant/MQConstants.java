package com.slilio.xiaohashu.comment.biz.constant;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: @Version: 1.0
 */
public interface MQConstants {

  String TOPIC_PUBLISH_COMMENT = "PublishCommentTopic"; // 发布评论的MQ Topic

  String TOPIC_COUNT_NOTE_COMMENT = "CountNoteCommentTopic"; // 笔记评论总数计数 Topic
}
