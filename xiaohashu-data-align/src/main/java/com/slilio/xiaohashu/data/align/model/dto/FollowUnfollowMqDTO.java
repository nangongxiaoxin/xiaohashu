package com.slilio.xiaohashu.data.align.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-23 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUnfollowMqDTO {
  private Long userId; // 原用户
  private Long targetUserId; // 目标用户ID
  private Integer type; // 1：关注；0：取关
}
