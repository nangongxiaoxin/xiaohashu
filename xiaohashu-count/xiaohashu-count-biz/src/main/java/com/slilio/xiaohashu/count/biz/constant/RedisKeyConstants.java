package com.slilio.xiaohashu.count.biz.constant;

/**
 * @Author: slilio @CreateTime: 2025-05-01 @Description: redis枚举类 @Version: 1.0
 */
public class RedisKeyConstants {
  // 用户维度计数枚举类
  private static final String COUNT_USER_KEY_PREFIX = "count:user:";

  // Hash field：粉丝总数
  public static final String FIELD_FANS_TOTAL = "fansTotal";

  // Hash field：关注总数
  public static final String FIELD_FOLLOWING_TOTAL = "followingTotal";

  // 笔记维度计数Key前缀
  private static final String COUNT_NOTE_KEY_PREFIX = "count:note:";

  // Hash field：笔记点赞总数
  public static final String FIELD_LIKE_TOTAL = "likeTotal";

  // Hash Field: 笔记收藏总数
  public static final String FIELD_COLLECT_TOTAL = "collectTotal";

  // Hash Field：笔记发布总数
  public static final String FIELD_NOTE_TOTAL = "noteTotal";

  /** Key前缀：评论维度计数Key */
  private static final String COUNT_COMMENT_KEY_PREFIX = "count:comment:";

  /** Hash field: 子评论总数 */
  public static final String FIELD_CHILD_COMMENT_TOTAL = "childCommentTotal";

  /** Hash Field:笔记评论总数 */
  public static final String FIELD_COMMENT_TOTAL = "commentTotal";

  public static String buildCountUserKey(Long userId) {
    return COUNT_USER_KEY_PREFIX + userId;
  }

  /**
   * 构建笔记维度计数Key
   *
   * @param noteId
   * @return
   */
  public static String buildCountNoteKey(Long noteId) {
    return COUNT_NOTE_KEY_PREFIX + noteId;
  }

  /**
   * 构建评论维度计数
   *
   * @param commentId
   * @return
   */
  public static String buildCountCommentKey(Long commentId) {
    return COUNT_COMMENT_KEY_PREFIX + commentId;
  }
}
