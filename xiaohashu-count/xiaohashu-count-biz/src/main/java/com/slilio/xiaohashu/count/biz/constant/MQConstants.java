package com.slilio.xiaohashu.count.biz.constant;

/**
 * @Author: slilio @CreateTime: 2025-04-29 @Description: MQ常量类 @Version: 1.0
 */
public interface MQConstants {
  String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic"; // topic：关注计数

  String TOPIC_COUNT_FANS = "CountFansTopic"; // topic:粉丝计数

  String TOPIC_COUNT_FANS_2_DB = "CountFans2DBTopic"; // topic：粉丝数计数入库

  String TOPIC_COUNT_FOLLOWING_2_DB = "CountFollowing2DBTopic"; // topic：关注数计数入库

  String TOPIC_COUNT_NOTE_LIKE = "CountNoteLikeTopic"; // topic：计数 - 笔记点赞数

  String TOPIC_COUNT_NOTE_LIKE_2_DB = "CountNoteLike2DBTopic"; // Topic：计数 - 笔记点赞数落库

  String TOPIC_COUNT_NOTE_COLLECT = "CountNoteCollectTopic"; // topic:计数-收藏服务

  String TOPIC_COUNT_NOTE_COLLECT_2_DB = "CountNoteCollect2DBTTopic"; //  Topic: 计数 - 笔记收藏数落库

  String TOPIC_NOTE_OPERATE = "NoteOperateTopic"; // 笔记删除、发布

  String TAG_NOTE_NOTE_PUBLISH = "publishNote"; // tag：笔记发布

  String TAG_NOTE_NOTE_DELETE = "deleteNote"; // tag：笔记删除

  String TOPIC_LIKE_OR_UNLIKE = "LikeUnlikeTopic"; // topic:计数-笔记点赞数

  String TOPIC_COUNT_NOTE_COMMENT = "CountNoteCommentTopic"; // topic: 笔记评论总数计数
}
