package com.slilio.xiaohashu.user.relation.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-04-29 @Description: 计数关注取关服务枚举类 @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum FollowUnfollowTypeEnum {
  // 关注
  FOLLOW(1),
  // 取关
  UNFOLLOW(2),
  ;

  private final Integer code;
}
