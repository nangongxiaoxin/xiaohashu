package com.slilio.xiaohashu.user.relation.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-18 @Description: 关注 入参 @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowUserReqVO {
  @NotNull(message = "被关注的用户ID不能为空")
  private Long followUserId;
}
