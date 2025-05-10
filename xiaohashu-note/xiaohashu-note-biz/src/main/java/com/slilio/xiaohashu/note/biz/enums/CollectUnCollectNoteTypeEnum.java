package com.slilio.xiaohashu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-11 @Description: 收藏、取消收藏操作枚举类 @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum CollectUnCollectNoteTypeEnum {
  // 收藏
  COLLECT(1),
  // 取消收藏
  UN_COLLECT(0),
  ;

  private final Integer code;
}
