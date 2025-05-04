package com.slilio.xiaohashu.note.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-03 @Description: Lua笔记点赞枚举 @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum NoteLikeLuaResultEnum {
  // 布隆过滤器不存在或者zset不存在
  NOT_EXIST(-1L),
  // 笔记已经点赞
  NOTE_LIKED(1L),
  // 笔记点赞成功
  NOTE_LIKE_SUCCESS(0L),
  ;
  ;

  private final Long code;

  /**
   * 根据类型CODE获取对应的枚举
   *
   * @param code
   * @return
   */
  public static NoteLikeLuaResultEnum valueOf(Long code) {
    for (NoteLikeLuaResultEnum noteLikeLusResultEnum : NoteLikeLuaResultEnum.values()) {
      if (Objects.equals(code, noteLikeLusResultEnum.getCode())) {
        return noteLikeLusResultEnum;
      }
    }
    return null;
  }
}
