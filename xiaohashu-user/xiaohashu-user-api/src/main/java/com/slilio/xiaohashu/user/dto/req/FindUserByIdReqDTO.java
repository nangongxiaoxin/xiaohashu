package com.slilio.xiaohashu.user.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindUserByIdReqDTO {
  /** 手机号 */
  @NotNull(message = "用户ID不能为空")
  private Long id;
}
