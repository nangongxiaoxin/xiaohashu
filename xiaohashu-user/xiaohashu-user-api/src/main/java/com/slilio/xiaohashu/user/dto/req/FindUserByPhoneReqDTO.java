package com.slilio.xiaohashu.user.dto.req;

import com.slilio.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindUserByPhoneReqDTO {
  @NotBlank(message = "手机号不能为空")
  @PhoneNumber
  private String phone; // 手机号
}
