package com.slilio.xiaohashu.search.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-29 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum NoteSortTypeEnum {
  LATEST(0), // 最新
  MOST_LIKE(1), // 最新点赞
  MOST_COMMENT(2), // 最多评论
  MOST_COLLECT(3), // 最多收藏
  ;

  private final Integer code;

  public static NoteSortTypeEnum valueOf(Integer code) {
    for (NoteSortTypeEnum noteSortTypeEnum : NoteSortTypeEnum.values()) {
      if (Objects.equals(code, noteSortTypeEnum.getCode())) {
        return noteSortTypeEnum;
      }
    }
    return null;
  }
}
