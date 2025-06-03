package com.slilio.xiaohashu.search.biz.enums;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: slilio @CreateTime: 2025-05-31 @Description: @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum NotePublishTimeRangeEnum {
  // 一天内
  DAY(0),
  // 一周内
  WEEK(1),
  // 半年内
  HALF_YEAR(2),
  ;

  private final Integer code;

  /**
   * 根据类型code获取对应的枚举
   *
   * @param code
   * @return
   */
  public static NotePublishTimeRangeEnum valueOf(Integer code) {
    for (NotePublishTimeRangeEnum notePublishTimeRangeEnum : NotePublishTimeRangeEnum.values()) {
      if (Objects.equals(code, notePublishTimeRangeEnum.getCode())) {
        return notePublishTimeRangeEnum;
      }
    }
    return null;
  }
}
