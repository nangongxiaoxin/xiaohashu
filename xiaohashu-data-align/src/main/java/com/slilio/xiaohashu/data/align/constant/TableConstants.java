package com.slilio.xiaohashu.data.align.constant;

/**
 * @Author: slilio @CreateTime: 2025-05-20 @Description: @Version: 1.0
 */
public class TableConstants {

  /** 表名的分隔符 */
  private static final String TABLE_NAME_SEPARATE = "_";

  /**
   * 拼接表名后缀
   *
   * @param date
   * @param hashKey
   * @return
   */
  public static String buildTableNameSuffix(String date, long hashKey) {
    // 拼接表名
    return date + TABLE_NAME_SEPARATE + hashKey;
  }
}
