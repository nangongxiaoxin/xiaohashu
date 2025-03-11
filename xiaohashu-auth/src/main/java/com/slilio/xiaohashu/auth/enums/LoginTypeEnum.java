package com.slilio.xiaohashu.auth.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginTypeEnum {
  // 验证码
  VERIFICATION_CODE(1),
  // 密码
  PASSWORD(2),
  ;

  private final Integer value;

  public static LoginTypeEnum valeOf(Integer code) {
    for (LoginTypeEnum loginTypeEnum : LoginTypeEnum.values()) {
      if (Objects.equals(code, loginTypeEnum.getValue())) {
        return loginTypeEnum;
      }
    }
    return null;
  }
}
