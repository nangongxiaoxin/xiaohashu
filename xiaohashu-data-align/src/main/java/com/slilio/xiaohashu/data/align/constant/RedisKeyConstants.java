package com.slilio.xiaohashu.data.align.constant;

/**
 * @Author: slilio @CreateTime: 2025-05-21 @Description: @Version: 1.0
 */
public class RedisKeyConstants {
  public static final String BLOOM_TODAY_NOTE_LIKE_LIST_KEY = "bloom:dataAlign:note:likes:";

  public static String buildBloomUserNoteLikeListKey(String date) {
    return BLOOM_TODAY_NOTE_LIKE_LIST_KEY + date;
  }
}
