package com.slilio.xiaohashu.data.align.domain.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * @Author: slilio @CreateTime: 2025-05-24 @Description: @Version: 1.0
 */
public interface UpdateMapper {
  int updateUserFollowingTotalByUserId(
      @Param("userId") Long userId, @Param("followingTotal") int followingTotal);
}
