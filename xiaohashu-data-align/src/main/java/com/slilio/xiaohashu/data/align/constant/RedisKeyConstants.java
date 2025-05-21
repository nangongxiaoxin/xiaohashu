package com.slilio.xiaohashu.data.align.constant;

/**
 * @Author: slilio @CreateTime: 2025-05-21 @Description: @Version: 1.0
 */
public class RedisKeyConstants {
  // 布隆过滤器：日增量变更数据，用户笔记点赞、取消点赞 前缀
  public static final String BLOOM_TODAY_NOTE_LIKE_LIST_KEY = "bloom:dataAlign:note:likes:";

  // 布隆过滤器：日增量变更数据，用户笔记收藏、取消收藏 前缀
  public static final String BLOOM_TODAY_NOTE_COLLECT_LIST_KEY = "bloom:dataAlign:note:collects:";

  // 构建笔记点赞、取消点赞的key
  public static String buildBloomUserNoteLikeListKey(String date) {
    return BLOOM_TODAY_NOTE_LIKE_LIST_KEY + date;
  }

  // 构建笔记收藏、取消收藏的key
  public static String buildBloomUserNoteCollectListKey(String date) {
    return BLOOM_TODAY_NOTE_COLLECT_LIST_KEY + date;
  }
}
