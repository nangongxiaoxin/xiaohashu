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

  public static String buildCountUserKey(Long userId) {
    return COUNT_USER_KEY_PREFIX + userId;
  }
}
