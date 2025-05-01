package com.slilio.xiaohashu.count.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-01 @Description: 关注、取关 @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum FollowUnfollowTypeEnum {
  // 关注
  FOLLOW(1),
  // 取关
  UNFOLLOW(0),
  ;

  private final Integer code;

  public static FollowUnfollowTypeEnum valueOf(Integer code) {
    for (FollowUnfollowTypeEnum followUnfollowTypeEnum : FollowUnfollowTypeEnum.values()) {
      if (Objects.equals(code, followUnfollowTypeEnum.getCode())) {
        return followUnfollowTypeEnum;
      }
    }
    return null;
  }
}
