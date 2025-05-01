package com.slilio.xiaohashu.count.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-01 @Description: DTO实体类 @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountFollowUnfollowMqDTO {
  private Long userId; // 原用户

  private Long targetUserId; // 目标用户

  private Integer type; // 1：关注，0：取关
}
