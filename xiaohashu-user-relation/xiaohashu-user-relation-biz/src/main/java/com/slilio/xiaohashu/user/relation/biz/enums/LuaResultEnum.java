package com.slilio.xiaohashu.user.relation.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-04-20 @Description: Lua脚本异常枚举 @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum LuaResultEnum {
  // ZSET 不存在
  ZSET_NOT_EXIST(-1L),
  // 关注已经达到上限
  FOLLOW_LIMIT(-2L),
  // 已经关注了该用户
  ALREADY_FOLLOWED(-3L),
  // 关注成功
  FOLLOW_SUCCESS(0L),
  ;

  private final Long code;

  public static LuaResultEnum valueOf(Long code) {
    for (LuaResultEnum luaResultEnum : LuaResultEnum.values()) {
      if (Objects.equals(code, luaResultEnum.getCode())) {
        return luaResultEnum;
      }
    }
    return null;
  }
}
