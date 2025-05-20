package com.slilio.xiaohashu.data.align.domain.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * @Author: slilio @CreateTime: 2025-05-21 @Description: @Version: 1.0
 */
public interface InsertRecordMapper {
  // 笔记点赞数：计数变更
  void insert2DataAlignNoteLikeCountTempTable(
      @Param("tableNameSuffix") String tableNameSuffix, @Param("noteId") Long noteId);

  //    用户获得的点赞数：计数变更
  void insert2DataAlignUserLikeCountTempTable(
      @Param("tableNameSuffix") String tableNameSuffix, @Param("userId") Long userId);
}
