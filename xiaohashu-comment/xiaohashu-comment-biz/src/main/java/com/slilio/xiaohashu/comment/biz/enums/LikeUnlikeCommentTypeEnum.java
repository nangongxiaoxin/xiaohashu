package com.slilio.xiaohashu.comment.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-06-21 @Description: @Version: 1.0
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
}
