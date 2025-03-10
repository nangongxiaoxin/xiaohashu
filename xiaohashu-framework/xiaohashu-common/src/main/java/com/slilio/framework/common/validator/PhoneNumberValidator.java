package com.slilio.framework.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// 注解的验证器类
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
  @Override
  public void initialize(PhoneNumber constraintAnnotation) {
    // 在此进行初始化操作
  }

  @Override
  public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
    // 逻辑校验
    return phoneNumber != null && phoneNumber.matches("\\d{11}");
  }
}
