package com.slilio.xiaohashu.comment.biz.constant;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: @Version: 1.0
 */
public interface MQConstants {

  String TOPIC_PUBLISH_COMMENT = "PublishCommentTopic"; // 发布评论的MQ Topic

  String TOPIC_COUNT_NOTE_COMMENT = "CountNoteCommentTopic"; // 笔记评论总数计数 Topic

  String TOPIC_COMMENT_HEAT_UPDATE = "CommentHeatUpdateTopic"; // 评论热度值更新 Topic

  String TOPIC_COMMENT_LIKE_OR_UNLIKE = "CommentLikeUnlikeTopic"; // 评论点赞、取消点赞共用一个topic

  String TOPIC_DELETE_COMMENT_LOCAL_CACHE = "DeleteCommentDetailLocalCacheTopic"; // 删除本地缓存-评论详情

  String TOPIC_DELETE_COMMENT = "DeleteCommentTopic"; // 删除评论

  String TAG_LIKE = "like"; // tag标签：点赞

  String TAG_UNLIKE = "UnLike"; // Tag标签：取消点赞
}
