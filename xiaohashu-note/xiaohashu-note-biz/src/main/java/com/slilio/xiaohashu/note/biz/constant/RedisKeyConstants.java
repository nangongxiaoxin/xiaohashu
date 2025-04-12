package com.slilio.xiaohashu.note.biz.constant;

public class RedisKeyConstants {
  // 笔记详情Key前缀
  public static final String NOTE_DETAIL_KEY = "note:detail:";

  // 构建完整的笔记详情key
  public static String buildNoteDetailKey(Long noteId) {
    return NOTE_DETAIL_KEY + noteId;
  }
}
