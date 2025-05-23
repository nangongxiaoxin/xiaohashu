package com.slilio.xiaohashu.data.align.domain.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * @Author: slilio @CreateTime: 2025-05-24 @Description: @Version: 1.0
 */
public interface SelectMapper {

  /**
   * 日增量表：关注数计数变更-批量查询
   *
   * @param tableNameSuffix
   * @param batchSize
   * @return
   */
  List<Long> selectBatchFromDataAlignFollowingCountTempTable(
      @Param("tableNameSuffix") String tableNameSuffix, @Param("batchSize") int batchSize);

  /**
   * 查询 t_following 关注表，获取关注总数
   *
   * @param userId
   * @return
   */
  int selectCountFromFollowingTableByUserId(long userId);
}
