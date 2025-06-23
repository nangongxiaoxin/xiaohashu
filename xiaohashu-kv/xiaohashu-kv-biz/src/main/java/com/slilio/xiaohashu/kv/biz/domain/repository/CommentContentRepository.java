package com.slilio.xiaohashu.kv.biz.domain.repository;

import com.slilio.xiaohashu.kv.biz.domain.dataobject.CommentContentDO;
import com.slilio.xiaohashu.kv.biz.domain.dataobject.CommentContentPrimaryKey;
import java.util.List;
import java.util.UUID;
import org.springframework.data.cassandra.repository.CassandraRepository;

/**
 * @Author: slilio @CreateTime: 2025-06-15 @Description: @Version: 1.0
 */
public interface CommentContentRepository
    extends CassandraRepository<CommentContentDO, CommentContentPrimaryKey> {

  /**
   * 根据noteId和yearMonth 批量查询评论内容
   *
   * @param noteId
   * @param yearMonths
   * @param contentIds
   * @return
   */
  List<CommentContentDO> findByPrimaryKeyNoteIdAndPrimaryKeyYearMonthInAndPrimaryKeyContentIdIn(
      Long noteId, List<String> yearMonths, List<UUID> contentIds);

  /**
   * 删除评论正文
   *
   * @param noteId
   * @param yearMonth
   * @param contentId
   */
  void deleteByPrimaryKeyNoteIdAndPrimaryKeyYearMonthAndPrimaryKeyContentId(
      Long noteId, String yearMonth, UUID contentId);
}
