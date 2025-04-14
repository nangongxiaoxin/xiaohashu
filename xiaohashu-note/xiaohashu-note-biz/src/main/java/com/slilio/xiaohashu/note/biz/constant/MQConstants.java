package com.slilio.xiaohashu.note.biz.constant;

public interface MQConstants {
  /** Topic主题：删除笔记本地缓存 */
  String TOPIC_DELETE_NOTE_LOCAL_CACHE = "DeleteNoteLocalCacheTopic";

  /** Topic主题：延迟双删Redis笔记缓存 */
  String TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE = "DelayDeleteNoteRedisCacheTopic";
}
