package com.slilio.xiaohashu.count.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-06-23 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum LikeUnlikeCommentTypeEnum {
  // 点赞
  LIKE(1),
  // 取消点赞
  UNLIKE(0),
  ;

  private final Integer code;

  public static LikeUnlikeCommentTypeEnum valueOf(Integer code) {
    for (LikeUnlikeCommentTypeEnum likeUnlikeCommentTypeEnum : LikeUnlikeCommentTypeEnum.values()) {
      if (Objects.equals(code, likeUnlikeCommentTypeEnum.getCode())) {
        return likeUnlikeCommentTypeEnum;
      }
    }
    return null;
  }
}
