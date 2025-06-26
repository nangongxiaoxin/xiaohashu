package com.slilio.xiaohashu.count.dto;

import jakarta.validation.constraints.NotNull;
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
public class FindUserCountsByIdReqDTO {
  @NotNull(message = "用户ID不能为空")
  private Long userId;
}
