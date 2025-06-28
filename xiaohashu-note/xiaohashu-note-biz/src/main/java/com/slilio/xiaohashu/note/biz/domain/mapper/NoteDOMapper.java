package com.slilio.xiaohashu.note.biz.domain.mapper;

import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface NoteDOMapper {
  int deleteByPrimaryKey(Long id);

  int insert(NoteDO record);

  int insertSelective(NoteDO record);

  NoteDO selectByPrimaryKey(Long id);

  int updateByPrimaryKeySelective(NoteDO record);

  int updateByPrimaryKey(NoteDO record);

  int updateVisibleOnlyMe(NoteDO noteDO);

  int updateIsTop(NoteDO noteDO);

  int selectCountByNoteId(Long noteId);

  /**
   * 查询笔记的发布者用户ID
   *
   * @param noteId
   * @return
   */
  Long selectCreatorIdByNoteId(Long noteId);

  /**
   * 查询个人主页已经发布笔记列表
   *
   * @param creatorId
   * @param cursor
   * @return
   */
  List<NoteDO> selectPublishedNoteListByUserIdAndCursor(
      @Param("creatorId") Long creatorId, @Param("cursor") Long cursor);
}
