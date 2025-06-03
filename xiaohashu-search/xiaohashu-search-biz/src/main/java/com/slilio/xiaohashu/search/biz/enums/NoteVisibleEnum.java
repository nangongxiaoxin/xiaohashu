package com.slilio.xiaohashu.search.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-06-02 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum NoteVisibleEnum {
  PUBLIC(0), // 公开，所有人可见
  PRIVATE(1), // 私有，仅自己可见
  ;

  private final Integer code;
}
