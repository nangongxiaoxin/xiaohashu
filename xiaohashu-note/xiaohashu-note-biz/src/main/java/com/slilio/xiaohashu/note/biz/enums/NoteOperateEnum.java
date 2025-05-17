package com.slilio.xiaohashu.note.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-18 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum NoteOperateEnum {
  // 笔记发布
  PUBLISH(1),
  // 笔记删除
  DELETE(0),
  ;

  private final Integer code;
}
