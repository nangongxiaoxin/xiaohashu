package com.slilio.xiaohashu.user.relation.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-27 @Description: 查找粉丝用户出参 @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindFansUserRspVO {
  private Long userId; // 用户ID
  private String avatar; // 用户头像
  private String nickname; // 用户昵称
  private Long fansTotal; // 粉丝总数
  private Long noteTotal; // 笔记总数
}
