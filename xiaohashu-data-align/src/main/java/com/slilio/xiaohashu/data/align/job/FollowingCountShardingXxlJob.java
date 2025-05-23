package com.slilio.xiaohashu.data.align.job;

import cn.hutool.core.collection.CollUtil;
import com.slilio.xiaohashu.data.align.constant.RedisKeyConstants;
import com.slilio.xiaohashu.data.align.constant.TableConstants;
import com.slilio.xiaohashu.data.align.domain.mapper.DeleteMapper;
import com.slilio.xiaohashu.data.align.domain.mapper.SelectMapper;
import com.slilio.xiaohashu.data.align.domain.mapper.UpdateMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-05-23 @Description: 分片广播任务 @Version: 1.0
 */
@Component
@Slf4j
public class FollowingCountShardingXxlJob {

  @Resource private SelectMapper selectMapper;
  @Resource private UpdateMapper updateMapper;
  @Resource private DeleteMapper deleteMapper;
  @Resource private RedisTemplate<String, Object> redisTemplate;

  // 分片广播任务
  @XxlJob("followingCountShardingJobHandler")
  public void followingCountShadingJobHandler() throws Exception {
    // 获取分片参数
    // 分片序号
    int shardIndex = XxlJobHelper.getShardIndex();
    // 分片总数
    int shardTotal = XxlJobHelper.getShardTotal();

    XxlJobHelper.log("分片参数：当前分片序号 = {}，总分片数 = {}", shardIndex, shardTotal);
    log.info("分片参数：当前分片序号 = {}，总分片数 = {}", shardIndex, shardTotal);

    // 业务逻辑

    // 表后缀
    String date =
        LocalDate.now()
            .minusDays(1) // 昨日的日期
            .format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 转字符串
    // 表名后缀
    String tableNameSuffix = TableConstants.buildTableNameSuffix(date, shardIndex);

    // 一批次1000条
    int batchSize = 1000;
    // 共对齐了多少条记录，默认为0
    int processedTotal = 0;
    // 死循环
    for (; ; ) {
      // 1.分批次查询 t_data_align_following_count_temp_日期_分片序号，如一批次查询1000条，直到全部查询完成
      List<Long> userIds =
          selectMapper.selectBatchFromDataAlignFollowingCountTempTable(tableNameSuffix, batchSize);

      // 若记录为空，终止循环
      if (CollUtil.isEmpty(userIds)) {
        break;
      }

      // 循环这一批次变更的用户ID
      userIds.forEach(
          userId -> {
            //  2.循环这一批发生变更的用户ID，对t_following关注表执行count(*) 操作，获取总数
            int followingTotal = selectMapper.selectCountFromFollowingTableByUserId(userId);

            //  3.更新t_user_count表，并更新对应Redis缓存
            int count = updateMapper.updateUserFollowingTotalByUserId(userId, followingTotal);
            // 更新对应Redis缓存
            if (count > 0) {
              String redisKey = RedisKeyConstants.buildCountUserKey(userId);
              // 判断Hash是否存在
              boolean hashKey = redisTemplate.hasKey(redisKey);
              // 若存在
              if (hashKey) {
                // 更新Hash中Field关注总数
                redisTemplate
                    .opsForHash()
                    .put(redisKey, RedisKeyConstants.FIELD_FOLLOWING_TOTAL, followingTotal);
              }
            }
          });
      //  4.批量物理删除这一批次记录
      deleteMapper.batchDeleteDataAlignFollowingCountTempTable(tableNameSuffix, userIds);

      // 当前已处理的记录数
      processedTotal += userIds.size();
    }
    XxlJobHelper.log(
        "======================================》结束定时分片广播任务：对当日发生变更的用户关注数进行对齐，共对齐记录数：{}",
        processedTotal);
  }
}
