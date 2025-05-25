package com.slilio.xiaohashu.data.align.domain.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * @Author: slilio @CreateTime: 2025-05-24 @Description: @Version: 1.0
 */
public interface UpdateMapper {
  /**
   * 更新用户关注数
   *
   * @param userId
   * @param followingTotal
   * @return
   */
  int updateUserFollowingTotalByUserId(
      @Param("userId") Long userId, @Param("followingTotal") int followingTotal);

  /**
   * 更新t_note_total计数表笔记点赞数
   *
   * @param noteId
   * @param noteLikeTotal
   * @return
   */
  int updateNoteLikeTotalByUserId(
      @Param("noteId") long noteId, @Param("noteLikeTotal") int noteLikeTotal);

  // 更新t_user_count表
  int updateUserFansTotalByUserId(@Param("userId") Long userId, @Param("fansTotal") int fansTotal);

  // 更新t_note_count 表
  int updateNoteCollectTotalByUserId(
      @Param("noteId") Long noteId, @Param("noteCollectTotal") int noteCollectTotal);

  /** 更新 t_user_count 计数表获得的总笔记发布数 */
  int updateUserNoteTotalByUserId(@Param("userId") long userId, @Param("noteTotal") int noteTotal);

  /** 更新 t_user_count 计数表获得的总收藏数 */
  int updateUserCollectTotalByUserId(
      @Param("userId") long userId, @Param("collectTotal") int collectTotal);

  /** 更新 t_user_count 计数表获得的总点赞数 */
  int updateUserLikeTotalByUserId(@Param("userId") long userId, @Param("likeTotal") int likeTotal);
}
