package com.slilio.xiaohashu.note.biz.constant;

public class RedisKeyConstants {
  // Roaring Bitmap：用户笔记点赞前缀
  public static final String R_BITMAP_USER_NOTE_LIKE_KEY = "rbitmap:note:likes:";

  // Roaring Bitmap：用户笔记收藏前缀
  public static final String R_BITMAP_USER_NOTE_COLLECT_LIST_KEY = "rbitmap:note:collects:";

  public static String buildRBitmapUserNoteLikeListKey(Long userId) {
    return R_BITMAP_USER_NOTE_LIKE_KEY + userId;
  }

  public static String buildRBitmapUserNoteCollectListKey(Long userId) {
    return R_BITMAP_USER_NOTE_COLLECT_LIST_KEY + userId;
  }

  // 笔记详情Key前缀
  public static final String NOTE_DETAIL_KEY = "note:detail:";

  // 布隆过滤器：用户笔记点赞
  public static final String BLOOM_USER_NOTE_LIKE_LIST_KEY = "bloom:note:likes:";

  // 用户笔记点赞列表Zset前缀
  public static final String USER_NOTE_LIKE_ZSET_KEY = "user:note:likes:";

  // 布隆过滤器：用户笔记收藏 前缀
  public static final String BLOOM_USER_NOTE_COLLECT_LIST_KEY = "bloom:note:collects:";

  // 用户笔记收藏Zset前缀
  public static final String USER_NOTE_COLLECT_ZSET_KEY = "user:note:collects:";

  // 构建完整的笔记详情key
  public static String buildNoteDetailKey(Long noteId) {
    return NOTE_DETAIL_KEY + noteId;
  }

  // 构建完整的布隆过滤器：用户笔记点赞 Key
  public static String buildBloomUserNoteLikeListKey(Long userId) {
    return BLOOM_USER_NOTE_LIKE_LIST_KEY + userId;
  }

  // 构建完整的用户笔记点赞列表ZsetKey
  public static String buildUserNoteLikeZSetKey(Long userId) {
    return USER_NOTE_LIKE_ZSET_KEY + userId;
  }

  // 构建完整的布隆过滤器：用户笔记收藏Key
  public static String buildBloomUserNoteCollectListKey(Long userId) {
    return BLOOM_USER_NOTE_COLLECT_LIST_KEY + userId;
  }

  // 构建完整的用户笔记收藏列表ZSET
  public static String buildUserNoteCollectZSetKey(Long userId) {
    return USER_NOTE_COLLECT_ZSET_KEY + userId;
  }
}
