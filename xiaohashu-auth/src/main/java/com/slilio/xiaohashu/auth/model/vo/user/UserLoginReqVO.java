package com.slilio.xiaohashu.auth.model.vo.user;

import com.slilio.framework.common.validator.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginReqVO {
  @NotBlank(message = "手机号不能为空")
  @PhoneNumber
  private String phone; // 手机号

  private String code; // 验证码

  private String password; // 密码

  @NotNull(message = "登录类型不能为空")
  private Integer type; // 登录类型：手机号验证码、账号密码
}
