package com.slilio.xiaohashu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-05 @Description: 点赞、取消点赞操作类型枚举类 @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum LikeUnlikeNoteTypeEnum {
  // 点赞
  LIKE(1),
  // 取消点赞
  UNLIKE(0),
  ;

  private final Integer code;
}
