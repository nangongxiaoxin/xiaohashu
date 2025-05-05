package com.slilio.xiaohashu.note.biz.domain.mapper;

import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteLikeDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface NoteLikeDOMapper {
  int deleteByPrimaryKey(Long id);

  int insert(NoteLikeDO record);

  int insertSelective(NoteLikeDO record);

  NoteLikeDO selectByPrimaryKey(Long id);

  int updateByPrimaryKeySelective(NoteLikeDO record);

  int updateByPrimaryKey(NoteLikeDO record);

  int selectCountByUserIdAndNoteId(@Param("userId") Long userId, @Param("noteId") Long noteId);

  List<NoteLikeDO> selectByUserId(@Param("userId") Long userId);

  int selectNoteIsLiked(@Param("userId") Long userId, @Param("noteId") Long noteId);

  List<NoteLikeDO> selectLikedByUserIdAndLimit(
      @Param("userId") Long userId, @Param("limit") int limit);

  /**
   * 新增笔记点赞，若已存在，则更新笔记点赞记录
   *
   * @param noteLikeDO
   * @return
   */
  int insertOrUpdate(NoteLikeDO noteLikeDO);

  /**
   * 取消点赞
   *
   * @param noteLikeDO
   * @return
   */
  int update2UnlikeByUserIdAndNoteId(NoteLikeDO noteLikeDO);
}
