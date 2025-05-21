package com.slilio.xiaohashu.data.align.job;

import com.slilio.xiaohashu.data.align.constant.TableConstants;
import com.slilio.xiaohashu.data.align.domain.mapper.DeleteTableMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-22 @Description: 删除计数日增量变更表任务 @Version: 1.0
 */
@Component
public class DeleteTableXxlJob {
  // 表总分片数
  @Value("${table.shards}")
  private int tableShards;

  @Resource private DeleteTableMapper deleteTableMapper;

  /** 1.简单任务示例（Bean模式） */
  @XxlJob("deleteTableJobHandler")
  public void deleteTableJobHandler() throws Exception {
    XxlJobHelper.log("## 开始删除最近一个月的日增量临时表");

    // 今日
    LocalDate today = LocalDate.now();
    // 日期格式
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    LocalDate startDate = today;
    // 从昨天往前推一个月
    LocalDate endDate = today.minusMonths(1);

    // 循环最近一个月的日期，不包含今天
    while (startDate.isAfter(endDate)) {
      // 往前推一天
      startDate = startDate.minusDays(1);
      // 日期字符串
      String date = startDate.format(formatter);

      for (int hashKey = 0; hashKey < tableShards; hashKey++) {
        // 表名后缀
        String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hashKey);

        // 删除表
        deleteTableMapper.deleteDataAlignFollowingCountTempTable(tableNameSuffix);
        deleteTableMapper.deleteDataAlignFansCountTempTable(tableNameSuffix);
        deleteTableMapper.deleteDataAlignNoteCollectCountTempTable(tableNameSuffix);
        deleteTableMapper.deleteDataAlignUserCollectCountTempTable(tableNameSuffix);
        deleteTableMapper.deleteDataAlignUserLikeCountTempTable(tableNameSuffix);
        deleteTableMapper.deleteDataAlignNoteLikeCountTempTable(tableNameSuffix);
        deleteTableMapper.deleteDataAlignNotePublishCountTempTable(tableNameSuffix);
      }
    }
    XxlJobHelper.log("## 结束删除最近一个月的日增量临时表");
  }
}
