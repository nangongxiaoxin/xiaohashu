package com.slilio.xiaohashu.note.biz.domain.mapper;

import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteCollectionDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface NoteCollectionDOMapper {
  int deleteByPrimaryKey(Long id);

  int insert(NoteCollectionDO record);

  int insertSelective(NoteCollectionDO record);

  NoteCollectionDO selectByPrimaryKey(Long id);

  int updateByPrimaryKeySelective(NoteCollectionDO record);

  int updateByPrimaryKey(NoteCollectionDO record);

  /**
   * 查询笔记是否被收藏
   *
   * @param userId
   * @param noteId
   * @return
   */
  int selectCountByUserIdAndNoteId(@Param("userId") Long userId, @Param("noteId") Long noteId);

  /**
   * 查寻用户已收藏的笔记
   *
   * @param userId
   * @return
   */
  List<NoteCollectionDO> selectByUserId(Long userId);

  /**
   * 查询笔记是否已经被收藏
   *
   * @param userId
   * @param noteId
   * @return
   */
  int selectNoteIsCollected(@Param("userId") Long userId, @Param("noteId") Long noteId);

  /**
   * 查询用户最近收藏的笔记
   *
   * @param userId
   * @param limit
   * @return
   */
  List<NoteCollectionDO> selectCollectedByUserIdAndLimit(
      @Param("userId") Long userId, @Param("limit") int limit);
}
