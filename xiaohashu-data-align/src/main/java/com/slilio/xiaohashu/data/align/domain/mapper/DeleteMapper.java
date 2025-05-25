package com.slilio.xiaohashu.data.align.domain.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * @Author: slilio @CreateTime: 2025-05-24 @Description: @Version: 1.0
 */
public interface DeleteMapper {

  /**
   * 日增量表：关注数计数变更-批量删除
   *
   * @param tableNameSuffix
   * @param userIds
   */
  void batchDeleteDataAlignFollowingCountTempTable(
      @Param("tableNameSuffix") String tableNameSuffix, @Param("userIds") List<Long> userIds);

  /**
   * 日增量表：笔记点赞数计数变更-批量删除
   *
   * @param tableNameSuffix
   * @param noteIds
   */
  void batchDeleteDataAlignNoteLikeCountTempTable(
      @Param("tableNameSuffix") String tableNameSuffix, @Param("noteIds") List<Long> noteIds);

  /**
   * 粉丝计数表变更-批量删除
   *
   * @param tableNameSuffix
   * @param userIds
   */
  void batchDeleteDataAlignFansCountTempTable(
      @Param("tableNameSuffix") String tableNameSuffix, @Param("userIds") List<Long> userIds);

  /**
   * 日增量表：笔记收藏计数变更 - 批量删除
   *
   * @param tableNameSuffix
   * @param noteIds
   */
  void batchDeleteDataAlignNoteCollectCountTempTable(
      @Param("tableNameSuffix") String tableNameSuffix, @Param("noteIds") List<Long> noteIds);

  /** 日增量表：用户发布笔记数变更 - 批量删除 */
  void batchDeleteDataAlignNotePublishCountTempTable(
      @Param("tableNameSuffix") String tableNameSuffix, @Param("userIds") List<Long> userIds);

  /**
   * 日增量表：用户获得的收藏数变更 - 批量删除
   *
   * @param userIds
   */
  void batchDeleteDataAlignUserCollectCountTempTable(
      @Param("tableNameSuffix") String tableNameSuffix, @Param("userIds") List<Long> userIds);

  /**
   * 日增量表：用户获得的点赞数变更 - 批量删除
   *
   * @param userIds
   */
  void batchDeleteDataAlignUserLikeCountTempTable(
      @Param("tableNameSuffix") String tableNameSuffix, @Param("userIds") List<Long> userIds);
}
