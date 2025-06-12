package com.slilio.xiaohashu.comment.biz.constant;

/**
 * @Author: slilio @CreateTime: 2025-06-13 @Description: @Version: 1.0
 */
public interface RedisKeyConstants {
  // key前缀：一级评论的first_reply_commit_id字段值是否更新标识
  public static final String HAVA_FIRST_REPLY_COMMENT_KEY_PREFIX =
      "comment:haveFirstReplyCommentId:";

  public static String buildHaveFirstReplyCommentKey(Long commentId) {
    return HAVA_FIRST_REPLY_COMMENT_KEY_PREFIX + commentId;
  }
}
