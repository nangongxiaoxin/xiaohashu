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
}
