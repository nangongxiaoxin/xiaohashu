package com.slilio.xiaohashu.user.relation.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-24 @Description: 取关接口入参实体 @Version: 1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UnfollowUserReqVO {
  @NotNull(message = "被取关用户ID不能为空")
  private Long unfollowUserId;
}
