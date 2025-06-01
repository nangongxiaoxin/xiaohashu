package com.slilio.xiaohashu.search.domain.mapper;

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
   * @return
   */
  List<Map<String, Object>> selectEsNoteIndexData(@Param("noteId") long noteId);
}
