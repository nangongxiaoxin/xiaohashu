package com.slilio.xiaohashu.note.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-06 @Description: 笔记取消点赞Lua脚本枚举类 @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum NoteUnlikeLuaResultEnum {
  // 布隆过滤器不存在
  NOT_EXISTS(-1L),
  // 笔记已经点赞
  NOTE_LIKED(1L),
  // 笔记未点赞
  NOTE_NOT_LIKED(0L),
  ;

  private final Long code;

  /**
   * 根据类型code获取对应的枚举
   *
   * @param code
   * @return
   */
  public static NoteUnlikeLuaResultEnum valueOf(Long code) {
    for (NoteUnlikeLuaResultEnum noteUnlikeLuaResultEnum : NoteUnlikeLuaResultEnum.values()) {
      if (Objects.equals(code, noteUnlikeLuaResultEnum.getCode())) {
        return noteUnlikeLuaResultEnum;
      }
    }
    return null;
  }
}
