package com.slilio.xiaohashu.auth.constant;

public class RedisKeyConstants {

  private static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:"; // 验证码前缀

  /** 小哈书全局ID生成器KEY */
  public static final String XIAOHASHU_ID_GENERATOR_KEY = "xiaohashu.id.generator";

  /**
   * 构建验证码 key
   *
   * @param phone
   * @return
   */
  public static String buildVerificationCodeKey(String phone) {
    return VERIFICATION_CODE_KEY_PREFIX + phone;
  }

  /** 用户角色数据key前缀 */
  private static final String USER_ROLES_KEY_PREFIX = "user:roles:";

  /**
   * 构建角色 key
   *
   * @param phone
   * @return
   */
  public static String buildUserRolesKey(String phone) {
    return USER_ROLES_KEY_PREFIX + phone;
  }

  /** 角色对应的权限集合key前缀 */
  private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";

  /**
   * 构建角色对应的权限集合KEY
   *
   * @param roleId
   * @return
   */
  public static String buildRolePermissionsKey(Long roleId) {
    return ROLE_PERMISSIONS_KEY_PREFIX + roleId;
  }
}
