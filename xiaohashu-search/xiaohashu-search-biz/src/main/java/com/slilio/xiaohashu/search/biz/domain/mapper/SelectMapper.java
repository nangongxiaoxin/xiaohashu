package com.slilio.xiaohashu.search.biz.domain.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * @Author: slilio @CreateTime: 2025-06-02 @Description: @Version: 1.0
 */
public interface SelectMapper {

  /**
   * 查询笔记文档所需的全字段数据
   *
   * @param noteId
   * @param userId
   * @return
   */
  List<Map<String, Object>> selectEsNoteIndexData(
      @Param("noteId") Long noteId, @Param("userId") Long userId);

  /**
   * 查询用户索引所需的全字段数据
   *
   * @param userId
   * @return
   */
  List<Map<String, Object>> selectEsUserIndexData(@Param("userId") Long userId);
}
