package com.slilio.xiaohashu.data.align.constant;

/**
 * @Author: slilio @CreateTime: 2025-05-21 @Description: @Version: 1.0
 */
public interface MQConstants {
  // topic: 计数-笔记点赞数
  String TOPIC_COUNT_NOTE_LIKE = "CountNoteLikeTopic";

  // topic：计数-笔记收藏数
  String TOPIC_COUNT_NOTE_COLLECT = "CountNoteCollectTopic";

  // topic：关注数计数
  String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic";

  // topic：笔记操作（发布、删除）
  String TOPIC_NOTE_OPERATE = "NoteOperateTopic";
}
