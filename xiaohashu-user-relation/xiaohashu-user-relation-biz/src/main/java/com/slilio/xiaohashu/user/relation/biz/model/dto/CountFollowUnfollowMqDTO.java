package com.slilio.xiaohashu.user.relation.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-29 @Description: 计数关注、取关服务实体类 @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountFollowUnfollowMqDTO {
  private Long userId; // 原用户
  private Long targetUserId; // 目标用户
  private Integer type; // 1：关注； 2：取关
}
