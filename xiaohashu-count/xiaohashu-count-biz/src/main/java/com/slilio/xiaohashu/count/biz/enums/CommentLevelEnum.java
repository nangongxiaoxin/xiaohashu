package com.slilio.xiaohashu.count.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-06-08 @Description: @Version: 1.0
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
}
