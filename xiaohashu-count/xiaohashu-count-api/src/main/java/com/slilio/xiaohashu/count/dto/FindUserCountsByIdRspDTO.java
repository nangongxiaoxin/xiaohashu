package com.slilio.xiaohashu.count.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-26 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindUserCountsByIdRspDTO {
  private Long userId; // 用户ID
  private Long fansTotal; // 粉丝数
  private Long followingTotal; // 关注数
  private Long noteTotal; // 当前发布笔记数
  private Long likeTotal; // 当前获得点赞数
  private Long collectTotal; // 当前获得收藏数
}
