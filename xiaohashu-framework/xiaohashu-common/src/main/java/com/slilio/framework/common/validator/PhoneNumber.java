package com.slilio.framework.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 自定义手机号校验注解
@Target({
  ElementType.METHOD,
  ElementType.FIELD,
  ElementType.ANNOTATION_TYPE,
  ElementType.PARAMETER
}) // 可以应用的元素类型
@Retention(RetentionPolicy.RUNTIME) // 保留策略，表示该注解在运行时任然可用
@Constraint(validatedBy = PhoneNumberValidator.class) // 关联的验证器类
public @interface PhoneNumber {
  String message() default "手机号格式不正确，需要为11位数字";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
