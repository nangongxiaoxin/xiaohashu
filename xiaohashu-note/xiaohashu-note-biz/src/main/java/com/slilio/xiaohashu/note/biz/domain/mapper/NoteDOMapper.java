package com.slilio.xiaohashu.note.biz.domain.mapper;

import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteDO;

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
}
