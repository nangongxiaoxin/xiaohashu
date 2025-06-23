package com.slilio.xiaohashu.comment.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-06-06 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum CommentLevelEnum {
  // 一级评论
  ONE(1),
  // 二级评论
  TWO(2),
  ;

  private final Integer code;

  /**
   * 获取对应的枚举
   *
   * @param code
   * @return
   */
  public static CommentLevelEnum valueOf(Integer code) {
    for (CommentLevelEnum commentLevelEnum : CommentLevelEnum.values()) {
      if (Objects.equals(code, commentLevelEnum.getCode())) {
        return commentLevelEnum;
      }
    }
    return null;
  }
}
