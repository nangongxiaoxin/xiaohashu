package com.slilio.xiaohashu.user.biz.constant;

public class RedisKeyConstants {

  /** 小哈书全局 ID 生成器 KEY */
  public static final String XIAOHASHU_ID_GENERATOR_KEY = "xiaohashu.id.generator";

  /** 用户角色数据 KEY 前缀 */
  private static final String USER_ROLES_KEY_PREFIX = "user:roles:";

  /** 角色对应的权限集合 KEY 前缀 */
  private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";

  /** 用户信息数据Key前缀 */
  private static final String USER_INFO_KEY_PREFIX = "user:info:";

  /** 用户主页信息数据Key前缀 */
  private static final String USER_PROFILE_KEY_PREFIX = "user:profile:";

  /**
   * 构建角色主要信息对应的KEY
   *
   * @param userId
   * @return
   */
  public static String buildUserProfileKey(Long userId) {
    return USER_PROFILE_KEY_PREFIX + userId;
  }

  /**
   * 用户对应的角色集合 KEY
   *
   * @param userId
   * @return
   */
  public static String buildUserRoleKey(Long userId) {
    return USER_ROLES_KEY_PREFIX + userId;
  }

  /**
   * 构建角色对应的权限集合 KEY
   *
   * @param roleKey
   * @return
   */
  public static String buildRolePermissionsKey(String roleKey) {
    return ROLE_PERMISSIONS_KEY_PREFIX + roleKey;
  }

  /**
   * 构建角色对应的权限集合KEY
   *
   * @param userId
   * @return
   */
  public static String buildUserInfoKey(Long userId) {
    return USER_INFO_KEY_PREFIX + userId;
  }
}
