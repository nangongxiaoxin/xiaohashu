package com.slilio.xiaohashu.note.biz.constant;

public interface MQConstants {
  /** Topic主题：删除笔记本地缓存 */
  String TOPIC_DELETE_NOTE_LOCAL_CACHE = "DeleteNoteLocalCacheTopic";

  /** Topic主题：延迟双删Redis笔记缓存 */
  String TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE = "DelayDeleteNoteRedisCacheTopic";

  /** Topic：点赞、取消点赞共用一个 */
  String TOPIC_LIKE_OR_UNLIKE = "LikeUnlikeTopic";

  /** 点赞标签 */
  String TAG_LIKE = "Like";

  /** Tag 标签：取消点赞 */
  String TAG_UNLIKE = "Unlike";

  /** Topic：计数 - 笔记点赞数 */
  String TOPIC_COUNT_NOTE_LIKE = "CountNoteLikeTopic";

  /** Topic：收藏、取消收藏共用一个 */
  String TOPIC_COLLECT_OR_UN_COLLECT = "CollectUnCollectTopic";

  /** Topic: 计数-笔记收藏数 */
  String TOPIC_COUNT_NOTE_COLLECT = "CountNoteCollectTopic";

  /** Tag：收藏 */
  String TAG_COLLECT = "Collect";

  /** Tag：取消收藏 */
  String TAG_UN_COLLECT = "UnCollect";
}
