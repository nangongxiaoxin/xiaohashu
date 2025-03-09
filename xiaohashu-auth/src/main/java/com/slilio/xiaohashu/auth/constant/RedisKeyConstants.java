package com.slilio.xiaohashu.auth.constant;

public class RedisKeyConstants {

  private static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:"; // 验证码前缀

  /**
   * 构建验证码
   *
   * @param phone
   * @return
   */
  public static String buildVerificationCodeKey(String phone) {
    return VERIFICATION_CODE_KEY_PREFIX + phone;
  }
}
