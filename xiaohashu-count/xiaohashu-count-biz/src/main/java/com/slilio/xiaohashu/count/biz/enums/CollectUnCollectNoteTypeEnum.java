package com.slilio.xiaohashu.count.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-13 @Description: 收藏、取消收藏枚举 @Version: 1.0
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

  public static CollectUnCollectNoteTypeEnum valueOf(Integer code) {
    for (CollectUnCollectNoteTypeEnum collectUnCollectNoteTypeEnum :
        CollectUnCollectNoteTypeEnum.values()) {
      if (Objects.equals(code, collectUnCollectNoteTypeEnum.getCode())) {
        return collectUnCollectNoteTypeEnum;
      }
    }
    return null;
  }
}
