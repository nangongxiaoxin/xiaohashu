package com.slilio.xiaohashu.note.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-11 @Description: 未收藏Lua脚本返回值枚举 @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum NoteUnCollectLuaResultEnum {
  // 布隆过滤器不存在
  NOT_EXISTS(-1L),
  // 笔记已收藏
  NOTE_COLLECTED(1L),
  // 笔记未收藏
  NOTE_NOT_COLLECTED(0L),
  ;

  private final Long code;

  /**
   * 根据提供的Code获取对应的枚举
   *
   * @param code
   * @return
   */
  public static NoteUnCollectLuaResultEnum valueOf(Long code) {
    for (NoteUnCollectLuaResultEnum noteUnCollectLuaResultEnum :
        NoteUnCollectLuaResultEnum.values()) {
      if (Objects.equals(code, noteUnCollectLuaResultEnum.getCode())) {
        return noteUnCollectLuaResultEnum;
      }
    }
    return null;
  }
}
