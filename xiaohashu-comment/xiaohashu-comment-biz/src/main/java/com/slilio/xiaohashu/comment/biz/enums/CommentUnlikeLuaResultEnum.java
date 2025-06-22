package com.slilio.xiaohashu.comment.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-06-22 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum CommentUnlikeLuaResultEnum {
  // 布隆过滤器不存在
  NOT_EXIST(-1L),
  // 评论已经点赞
  COMMENT_LIKED(1L),
  // 评论未点赞
  COMMENT_NOT_LIKED(0L),
  ;

  private final Long code;

  /**
   * 根据不同类型的Code获取对应的枚举
   *
   * @param code
   * @return
   */
  public static CommentUnlikeLuaResultEnum valueOf(Long code) {
    for (CommentUnlikeLuaResultEnum commentUnlikeLuaResultEnum :
        CommentUnlikeLuaResultEnum.values()) {
      if (Objects.equals(code, commentUnlikeLuaResultEnum.getCode())) {
        return commentUnlikeLuaResultEnum;
      }
    }
    return null;
  }
}
