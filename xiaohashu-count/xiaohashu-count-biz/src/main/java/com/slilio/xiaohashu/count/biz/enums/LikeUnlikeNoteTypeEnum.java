package com.slilio.xiaohashu.count.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-08 @Description: 笔记点赞枚举类 @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum LikeUnlikeNoteTypeEnum {
  LIKE(1), // 点赞
  UNLIKE(0), // 取消点赞
  ;

  private final Integer code;

  public static LikeUnlikeNoteTypeEnum valueOf(Integer code) {
    for (LikeUnlikeNoteTypeEnum likeUnlikeNoteTypeEnum : LikeUnlikeNoteTypeEnum.values()) {
      if (Objects.equals(code, likeUnlikeNoteTypeEnum.getCode())) {
        return likeUnlikeNoteTypeEnum;
      }
    }
    return null;
  }
}
