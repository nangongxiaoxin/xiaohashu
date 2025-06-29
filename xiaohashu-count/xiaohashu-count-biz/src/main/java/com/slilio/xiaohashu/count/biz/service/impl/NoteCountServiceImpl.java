package com.slilio.xiaohashu.count.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.google.common.collect.Maps;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.count.biz.domain.dataobject.NoteCountDO;
import com.slilio.xiaohashu.count.biz.domain.mapper.NoteCountDOMapper;
import com.slilio.xiaohashu.count.biz.service.NoteCountService;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdRspDTO;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdsReqDTO;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-29 @Description: @Version: 1.0
 */
@Component
@Slf4j
public class NoteCountServiceImpl implements NoteCountService {
  @Resource private NoteCountDOMapper noteCountDOMapper;
  @Resource private RedisTemplate<String, Object> redisTemplate;

  /**
   * 批量查询笔记计数
   *
   * @param findNoteCountsByIdsReqDTO
   * @return
   */
  @Override
  public Response<List<FindNoteCountsByIdRspDTO>> findNotesCountData(
      FindNoteCountsByIdsReqDTO findNoteCountsByIdsReqDTO) {
    List<Long> noteIds = findNoteCountsByIdsReqDTO.getNoteIds();

    // 1.先查询缓存
    List<String> hashKey = noteIds.stream().map(RedisKeyConstants::buildCountNoteKey).toList();

    // 使用pipeline通道，从redis中批量查询笔记hashkey计数
    List<Object> countHashes = getCountHashesByPipelineFromRedis(hashKey);
    // 返参DTO
    List<FindNoteCountsByIdRspDTO> findNoteCountsByIdRspDTOS = Lists.newArrayList();
    // 用与存储缓存中不存在，需要数据库查询的笔记ID
    List<Long> noteIdsNeedQuery = Lists.newArrayList();

    // 循环入参需要查询的笔记ID，构建对应的DTO，并设置缓存中已经存在的计数，以及过滤出需要查询数据库的笔记ID
    for (int i = 0; i < noteIds.size(); i++) {
      Long currNoteId = noteIds.get(i);
      List<Integer> currCountHash = (List<Integer>) countHashes.get(i);

      Integer likeTotal = currCountHash.get(0);
      Integer collectTotal = currCountHash.get(1);
      Integer commentTotal = currCountHash.get(2);

      // Hash中存在任意一个Field为null，都需要查询数据库
      if (Objects.isNull(likeTotal)
          || Objects.isNull(collectTotal)
          || Objects.isNull(commentTotal)) {
        noteIdsNeedQuery.add(currNoteId);
      }

      // 构建DTO
      FindNoteCountsByIdRspDTO findNoteCountsByIdRspDTO =
          FindNoteCountsByIdRspDTO.builder()
              .noteId(currNoteId)
              .likeTotal(Objects.nonNull(likeTotal) ? Long.valueOf(likeTotal) : null)
              .collectTotal(Objects.nonNull(collectTotal) ? Long.valueOf(collectTotal) : null)
              .commentTotal(Objects.nonNull(commentTotal) ? Long.valueOf(commentTotal) : null)
              .build();

      findNoteCountsByIdRspDTOS.add(findNoteCountsByIdRspDTO);
    }

    // 所有Hash计数都存在于Redis中，直接返参
    if (CollUtil.isEmpty(noteIdsNeedQuery)) {
      return Response.success(findNoteCountsByIdRspDTOS);
    }

    // 2.若缓存中无，则查询数据库
    // 从数据库查询缓存遗漏的
    List<NoteCountDO> noteCountDOS = noteCountDOMapper.selectByNoteIds(noteIdsNeedQuery);
    if (CollUtil.isNotEmpty(noteCountDOS)) {
      // DO集合转Map，方便后续查询对应笔记ID的计数
      Map<Long, NoteCountDO> noteIdAndDOMap =
          noteCountDOS.stream()
              .collect(Collectors.toMap(NoteCountDO::getNoteId, noteCountDO -> noteCountDO));

      // 将笔记hash计数存储到redis中
      syncNoteHash2Redis(findNoteCountsByIdRspDTOS, noteIdAndDOMap);

      // 针对DTO中为null的计数字段，循环设置从数据库中查询到的计数
      for (FindNoteCountsByIdRspDTO findNoteCountsByIdRspDTO : findNoteCountsByIdRspDTOS) {
        Long noteId = findNoteCountsByIdRspDTO.getNoteId();
        Long likeTotal = findNoteCountsByIdRspDTO.getLikeTotal();
        Long collectTotal = findNoteCountsByIdRspDTO.getCollectTotal();
        Long commentTotal = findNoteCountsByIdRspDTO.getCommentTotal();

        NoteCountDO noteCountDO = noteIdAndDOMap.get(noteId);

        if (Objects.isNull(likeTotal)) {
          findNoteCountsByIdRspDTO.setLikeTotal(
              Objects.nonNull(noteCountDO) ? noteCountDO.getLikeTotal() : 0);
        }
        if (Objects.isNull(collectTotal)) {
          findNoteCountsByIdRspDTO.setCollectTotal(
              Objects.nonNull(noteCountDO) ? noteCountDO.getCollectTotal() : 0);
        }
        if (Objects.isNull(commentTotal)) {
          findNoteCountsByIdRspDTO.setCommentTotal(
              Objects.nonNull(noteCountDO) ? noteCountDO.getCommentTotal() : 0);
        }
      }
    }

    return Response.success(findNoteCountsByIdRspDTOS);
  }

  /**
   * 将笔记Hash计数同步到redis中
   *
   * @param findNoteCountsByIdRspDTOS
   * @param noteIdAndDOMap
   */
  private void syncNoteHash2Redis(
      List<FindNoteCountsByIdRspDTO> findNoteCountsByIdRspDTOS,
      Map<Long, NoteCountDO> noteIdAndDOMap) {
    // 将笔记id计数同步到redis中
    redisTemplate.executePipelined(
        new SessionCallback<>() {
          @Override
          public Object execute(RedisOperations operations) {
            // 循环已经构建好的返参DTO集合
            for (FindNoteCountsByIdRspDTO findNoteCountsByIdRspDTO : findNoteCountsByIdRspDTOS) {
              Long likeTotal = findNoteCountsByIdRspDTO.getLikeTotal();
              Long collectTotal = findNoteCountsByIdRspDTO.getCollectTotal();
              Long commentTotal = findNoteCountsByIdRspDTO.getCommentTotal();

              // 若当前DTO的所有计数都不为空，则无需同步
              if (Objects.nonNull(likeTotal)
                  && Objects.nonNull(collectTotal)
                  && Objects.nonNull(commentTotal)) {
                continue;
              }

              // 如果但凡有一个值为null，都需要同步field
              Long noteId = findNoteCountsByIdRspDTO.getNoteId();
              String noteCountHashKey = RedisKeyConstants.buildCountNoteKey(noteId);

              // 设置field计数
              Map<String, Long> countMap = Maps.newHashMap();
              NoteCountDO noteCountDO = noteIdAndDOMap.get(noteId);

              if (Objects.isNull(likeTotal)) {
                countMap.put(
                    RedisKeyConstants.FIELD_LIKE_TOTAL,
                    Objects.nonNull(noteCountDO) ? noteCountDO.getLikeTotal() : 0);
              }
              if (Objects.isNull(collectTotal)) {
                countMap.put(
                    RedisKeyConstants.FIELD_COLLECT_TOTAL,
                    Objects.nonNull(noteCountDO) ? noteCountDO.getCollectTotal() : 0);
              }
              if (Objects.isNull(commentTotal)) {
                countMap.put(
                    RedisKeyConstants.FIELD_COMMENT_TOTAL,
                    Objects.nonNull(noteCountDO) ? noteCountDO.getCommentTotal() : 0);
              }

              // 批量设置hash计数的field
              operations.opsForHash().putAll(noteCountHashKey, countMap);

              // 设置随机过期时间 (1小时以内)
              long expireTime = 60 * 30 + RandomUtil.randomInt(60 * 30);
              operations.expire(noteCountHashKey, expireTime, TimeUnit.SECONDS);
            }

            return null;
          }
        });
  }

  /**
   * 从redis中查询笔记hash计数
   *
   * @param hashKey
   * @return
   */
  private List<Object> getCountHashesByPipelineFromRedis(List<String> hashKeys) {
    return redisTemplate.executePipelined(
        new SessionCallback<>() {
          @Override
          public Object execute(RedisOperations operations) {
            for (String hashKey : hashKeys) {
              // 批量获取多个
              operations
                  .opsForHash()
                  .multiGet(
                      hashKey,
                      List.of(
                          RedisKeyConstants.FIELD_LIKE_TOTAL,
                          RedisKeyConstants.FIELD_COLLECT_TOTAL,
                          RedisKeyConstants.FIELD_COMMENT_TOTAL));
            }
            return null;
          }
        });
  }
}
