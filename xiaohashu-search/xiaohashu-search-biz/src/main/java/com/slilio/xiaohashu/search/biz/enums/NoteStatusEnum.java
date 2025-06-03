package com.slilio.xiaohashu.search.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-06-02 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum NoteStatusEnum {
  BE_EXAMINE(0), // 待审核
  NORMAL(1), // 正常展示
  DELETED(2), // 被删除
  DOWNED(3), // 被下架
  ;

  private final Integer code;
}
