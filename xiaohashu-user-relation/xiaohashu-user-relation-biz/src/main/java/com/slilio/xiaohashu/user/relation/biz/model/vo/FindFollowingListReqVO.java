package com.slilio.xiaohashu.user.relation.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-26 @Description: 查询关注列表 @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindFollowingListReqVO {
  @NotNull(message = "查询用户ID不能为空")
  private Long userId;

  @NotNull(message = "页码不能为空")
  private Integer pageNo = 1; // 默认值为第一页
}
