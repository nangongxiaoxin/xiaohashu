package com.slilio.xiaohashu.user.relation.biz.constant;

/**
 * @Author: slilio @CreateTime: 2025-04-20 @Description: Redis Key 常亮类 @Version: 1.0
 */
public class RedisKeyConstants {
  /** 关注列表Key签注 */
  private static final String USER_FOLLOWING_KEY_PREFIX = "following:";

  /**
   * 构建关注列表完整的Key
   *
   * @param userId
   * @return
   */
  public static String buildUserFollowingKey(Long userId) {
    return USER_FOLLOWING_KEY_PREFIX + userId;
  }
}
