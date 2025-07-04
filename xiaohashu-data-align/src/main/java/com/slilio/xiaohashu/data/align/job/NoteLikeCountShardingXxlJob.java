package com.slilio.xiaohashu.data.align.job;

import cn.hutool.core.collection.CollUtil;
import com.slilio.xiaohashu.data.align.constant.RedisKeyConstants;
import com.slilio.xiaohashu.data.align.constant.TableConstants;
import com.slilio.xiaohashu.data.align.domain.mapper.DeleteMapper;
import com.slilio.xiaohashu.data.align.domain.mapper.SelectMapper;
import com.slilio.xiaohashu.data.align.domain.mapper.UpdateMapper;
import com.slilio.xiaohashu.data.align.rpc.SearchRpcService;
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
 * @Author: slilio @CreateTime: 2025-05-25 @Description: 分片定时广播：对当日发送变更的笔记点赞数进行对齐 @Version: 1.0
 */
@Component
@Slf4j
public class NoteLikeCountShardingXxlJob {

  @Resource private SelectMapper selectMapper;
  @Resource private UpdateMapper updateMapper;
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private DeleteMapper deleteMapper;
  @Resource private SearchRpcService searchRpcService;

  /** 分片广播任务 */
  @XxlJob("noteLikeCountShardingJobHandler")
  public void noteLikeCountShardingJobHandler() throws Exception {
    // 获取分片参数

    // 分片序号
    int shardIndex = XxlJobHelper.getShardIndex();
    // 分片总数
    int shardTotal = XxlJobHelper.getShardTotal();

    XxlJobHelper.log("=================> 开始定时分片广播任务：对当日发生变更的笔记点赞数进行对齐");
    XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

    log.info("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

    // 表后缀
    String date =
        LocalDate.now()
            .minusDays(1) // 昨日的日期
            .format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 转字符串
    String tableNameSuffix = TableConstants.buildTableNameSuffix(date, shardIndex);

    // 一批次1000条
    int batchSize = 1000;
    // 共对齐了多少条记录，默认为0
    int processedTotal = 0;

    // 循环
    for (; ; ) {
      // 1. 分批次查询 t_data_align_note_like_count_temp_日期_分片序号，如一批次查询 1000 条，直到全部查询完成
      List<Long> noteIds =
          selectMapper.selectBatchFromDataAlignNoteLikeCountTempTable(tableNameSuffix, batchSize);

      // 若记录为空，终止循环
      if (CollUtil.isEmpty(noteIds)) {
        break;
      }

      // 循环发送变更的笔记ID
      noteIds.forEach(
          noteId -> {
            //  2: 循环这一批发生变更的笔记 ID， 对 t_note_like 关注表执行 count(*) 操作，获取总数 从点赞细表获取发生变更的最新点赞数据
            int likeTotal = selectMapper.selectCountFromNoteLikeTableByUserId(noteId);

            // TODO 3: 更新 t_note_count 表，并更新对应 Redis 缓存 写入到计数表
            int count = updateMapper.updateNoteLikeTotalByUserId(noteId, likeTotal);
            // 更新对应的Redis缓存
            if (count > 0) {
              String redisKey = RedisKeyConstants.buildCountNoteKey(noteId);
              // 判断hash是否存在
              boolean hashKey = redisTemplate.hasKey(redisKey);
              // 存在
              if (hashKey) {
                // 更新hash中field笔记点赞总数
                redisTemplate
                    .opsForHash()
                    .put(redisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, likeTotal);
              }
            }

            // 远程RPC，调用搜索服务
            searchRpcService.rebuildNoteDocument(noteId);
          });

      // 4. 批量物理删除这一批次记录
      deleteMapper.batchDeleteDataAlignNoteLikeCountTempTable(tableNameSuffix, noteIds);

      // 当前已处理的记录数
      processedTotal += noteIds.size();
    }
    XxlJobHelper.log("=================> 结束定时分片广播任务：对当日发生变更的笔记点赞数进行对齐，共对齐记录数：{}", processedTotal);
  }
}
