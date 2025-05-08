package com.slilio.xiaohashu.note.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-09 @Description: @Version: 1.0
 */
@AllArgsConstructor
@Getter
public enum NoteCollectLuaResultEnum {
  // 布隆过滤器不存在或者Zset不存在
  NOT_EXIST(-1L),
  // 笔记已收藏
  NOTE_COLLECTED(1L),
  // 笔记收藏成功
  NOTE_COLLECTED_SUCCESS(0L),
  ;

  private final Long code;

  /**
   * 根据code获取对应的枚举类型
   *
   * @param code
   * @return
   */
  public static NoteCollectLuaResultEnum valueOf(Long code) {
    for (NoteCollectLuaResultEnum noteCollectLuaResultEnum : NoteCollectLuaResultEnum.values()) {
      if (Objects.equals(code, noteCollectLuaResultEnum.getCode())) {
        return noteCollectLuaResultEnum;
      }
    }
    return null;
  }
}
