package com.slilio.framework.common.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @Author: slilio @CreateTime: 2025-04-20 @Description: 获取时间戳工具类 @Version: 1.0
 */
public class DateUtils {
  public static long localDateTime2Timestamp(LocalDateTime localDateTime) {
    return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
  }
}
