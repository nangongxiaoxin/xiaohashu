package com.slilio.xiaohashu.comment.biz.constant;

/**
 * @Author: slilio @CreateTime: 2025-06-13 @Description: @Version: 1.0
 */
public class RedisKeyConstants {
  // key前缀：一级评论的first_reply_commit_id字段值是否更新标识
  public static final String HAVA_FIRST_REPLY_COMMENT_KEY_PREFIX =
      "comment:haveFirstReplyCommentId:";

  // key前缀：笔记评论总数
  private static final String COUNT_COMMENT_TOTAL_KEY_PREFIX = "count:note:";

  // Hash field键：评论总数
  public static final String FIELD_COMMENT_TOTAL = "commentTotal";

  /** key前缀：评论分页ZSET */
  private static final String COMMENT_LIST_KEY_PREFIX = "comment:list:";

  /** Key前缀：评论详情Json */
  private static final String COMMENT_DETAIL_KEY_PREFIX = "comment:detail:";

  /** 评论维度计数key前缀 */
  private static final String COUNT_COMMENT_KEY_PREFIX = "count:comment:";

  /** Hash Field：子评论总数 */
  public static final String FIELD_CHILD_COMMENT_TOTAL = "childCommentTotal";

  /** Hash Field： 点赞总数 */
  public static final String FIELD_LIKE_TOTAL = "likeTotal";

  /** Key 前缀：二级评论分页ZSET */
  private static final String CHILD_COMMENT_LIST_KEY_PREFIX = "comment:childList:";

  /**
   * 构建子评论分页ZSET完整KEY
   *
   * @param commentId
   * @return
   */
  public static String buildChildCommentListKey(Long commentId) {
    return CHILD_COMMENT_LIST_KEY_PREFIX + commentId;
  }

  /**
   * 构建评论维度计数Key
   *
   * @param commentId
   * @return
   */
  public static String buildCountCommentKey(Long commentId) {
    return COUNT_COMMENT_KEY_PREFIX + commentId;
  }

  /**
   * 构建一级评论ID完整Key
   *
   * @param commentId
   * @return
   */
  public static String buildHaveFirstReplyCommentKey(Long commentId) {
    return HAVA_FIRST_REPLY_COMMENT_KEY_PREFIX + commentId;
  }

  /**
   * 构建笔记评论总数完整KEY
   *
   * @param noteId
   * @return
   */
  public static String buildNoteCommentTotalKey(Long noteId) {
    return COUNT_COMMENT_TOTAL_KEY_PREFIX + noteId;
  }

  /**
   * 构建评论分页ZSET完整KEY
   *
   * @param noteId
   * @return
   */
  public static String buildCommentListKey(Long noteId) {
    return COMMENT_LIST_KEY_PREFIX + noteId;
  }

  /**
   * 构建评论详情完整KEY
   *
   * @param commentId
   * @return
   */
  public static String buildCommentDetailKey(Object commentId) {
    return COMMENT_DETAIL_KEY_PREFIX + commentId;
  }
}
