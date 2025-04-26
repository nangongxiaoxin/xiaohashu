package com.slilio.xiaohashu.user.relation.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-27 @Description: 查找粉丝列表入参 @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindFansListReqVO {
  @NotNull(message = "查询用户ID不能为空")
  private Long userId;

  @NotNull(message = "页码不能为空")
  private Integer pageNo = 1; // 默认值为第一页
}
