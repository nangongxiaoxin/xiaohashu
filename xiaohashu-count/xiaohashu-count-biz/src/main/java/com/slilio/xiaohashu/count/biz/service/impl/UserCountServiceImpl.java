package com.slilio.xiaohashu.count.biz.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.google.common.collect.Maps;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.count.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.count.biz.domain.dataobject.UserCountDO;
import com.slilio.xiaohashu.count.biz.domain.mapper.UserCountDOMapper;
import com.slilio.xiaohashu.count.biz.service.UserCountService;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdReqDTO;
import com.slilio.xiaohashu.count.dto.FindUserCountsByIdRspDTO;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @Author: slilio @CreateTime: 2025-06-26 @Description: @Version: 1.0
 */
@Service
@Slf4j
public class UserCountServiceImpl implements UserCountService {

  @Resource private UserCountDOMapper userCountDOMapper;
  @Resource private RedisTemplate<String, Object> redisTemplate;

  @Resource(name = "taskExecutor")
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  /**
   * 查询用户计数
   *
   * @param findUserCountsByIdReqDTO
   * @return
   */
  @Override
  public Response<FindUserCountsByIdRspDTO> findUserCountData(
      FindUserCountsByIdReqDTO findUserCountsByIdReqDTO) {
    // 目标用户ID
    Long userId = findUserCountsByIdReqDTO.getUserId();

    FindUserCountsByIdRspDTO findUserCountsByIdRspDTO =
        FindUserCountsByIdRspDTO.builder().userId(userId).build();

    // redis中查询
    String userCountHashKey = RedisKeyConstants.buildCountUserKey(userId);

    List<Object> counts =
        redisTemplate
            .opsForHash()
            .multiGet(
                userCountHashKey,
                List.of(
                    RedisKeyConstants.FIELD_COLLECT_TOTAL,
                    RedisKeyConstants.FIELD_FANS_TOTAL,
                    RedisKeyConstants.FIELD_NOTE_TOTAL,
                    RedisKeyConstants.FIELD_FOLLOWING_TOTAL,
                    RedisKeyConstants.FIELD_LIKE_TOTAL));

    Object collectTotal = counts.get(0);
    Object fansTotal = counts.get(1);
    Object noteTotal = counts.get(2);
    Object followingTotal = counts.get(3);
    Object likeTotal = counts.get(4);

    findUserCountsByIdRspDTO.setCollectTotal(
        Objects.isNull(collectTotal) ? 0 : Long.parseLong(String.valueOf(collectTotal)));
    findUserCountsByIdRspDTO.setFansTotal(
        Objects.isNull(fansTotal) ? 0 : Long.parseLong(String.valueOf(fansTotal)));
    findUserCountsByIdRspDTO.setNoteTotal(
        Objects.isNull(noteTotal) ? 0 : Long.parseLong(String.valueOf(noteTotal)));
    findUserCountsByIdRspDTO.setFollowingTotal(
        Objects.isNull(followingTotal) ? 0 : Long.parseLong(String.valueOf(followingTotal)));
    findUserCountsByIdRspDTO.setLikeTotal(
        Objects.isNull(likeTotal) ? 0 : Long.parseLong(String.valueOf(likeTotal)));

    // 若其中一个为空
    boolean isAnyNull = counts.stream().anyMatch(Objects::isNull);

    if (isAnyNull) {
      // 从数据库查询该用户的计数
      UserCountDO userCountDO = userCountDOMapper.selectByUserId(userId);

      // 判断 Redis 中对应计数，若为空，则使用 DO 中的计数
      if (Objects.nonNull(userCountDO) && Objects.isNull(collectTotal)) {
        findUserCountsByIdRspDTO.setCollectTotal(userCountDO.getCollectTotal());
      }
      if (Objects.nonNull(userCountDO) && Objects.isNull(fansTotal)) {
        findUserCountsByIdRspDTO.setFansTotal(userCountDO.getFansTotal());
      }
      if (Objects.nonNull(userCountDO) && Objects.isNull(noteTotal)) {
        findUserCountsByIdRspDTO.setNoteTotal(userCountDO.getNoteTotal());
      }
      if (Objects.nonNull(userCountDO) && Objects.isNull(followingTotal)) {
        findUserCountsByIdRspDTO.setFollowingTotal(userCountDO.getFollowingTotal());
      }
      if (Objects.nonNull(userCountDO) && Objects.isNull(likeTotal)) {
        findUserCountsByIdRspDTO.setLikeTotal(userCountDO.getLikeTotal());
      }

      // 异步同步到 Redis 缓存中, 以便下次查询能够命中缓存
      syncHashCount2Redis(
          userCountHashKey,
          userCountDO,
          collectTotal,
          fansTotal,
          noteTotal,
          followingTotal,
          likeTotal);
    }

    return Response.success(findUserCountsByIdRspDTO);
  }

  // 异步同步到 Redis 缓存中, 以便下次查询能够命中缓存
  private void syncHashCount2Redis(
      String userCountHashKey,
      UserCountDO userCountDO,
      Object collectTotal,
      Object fansTotal,
      Object noteTotal,
      Object followingTotal,
      Object likeTotal) {
    if (Objects.nonNull(userCountDO)) {
      threadPoolTaskExecutor.execute(
          () -> {
            // 存放计数
            Map<String, Long> userCountMap = Maps.newHashMap();
            if (Objects.isNull(collectTotal)) {
              userCountMap.put(
                  RedisKeyConstants.FIELD_COLLECT_TOTAL,
                  Objects.isNull(userCountDO.getCollectTotal())
                      ? 0
                      : userCountDO.getCollectTotal());
            }

            if (Objects.isNull(fansTotal)) {
              userCountMap.put(
                  RedisKeyConstants.FIELD_FANS_TOTAL,
                  Objects.isNull(userCountDO.getFansTotal()) ? 0 : userCountDO.getFansTotal());
            }

            if (Objects.isNull(noteTotal)) {
              userCountMap.put(
                  RedisKeyConstants.FIELD_NOTE_TOTAL,
                  Objects.isNull(userCountDO.getNoteTotal()) ? 0 : userCountDO.getNoteTotal());
            }

            if (Objects.isNull(followingTotal)) {
              userCountMap.put(
                  RedisKeyConstants.FIELD_FOLLOWING_TOTAL,
                  Objects.isNull(userCountDO.getFollowingTotal())
                      ? 0
                      : userCountDO.getFollowingTotal());
            }

            if (Objects.isNull(likeTotal)) {
              userCountMap.put(
                  RedisKeyConstants.FIELD_LIKE_TOTAL,
                  Objects.isNull(userCountDO.getLikeTotal()) ? 0 : userCountDO.getLikeTotal());
            }

            redisTemplate.executePipelined(
                new SessionCallback<>() {
                  @Override
                  public Object execute(RedisOperations operations) {
                    // 批量添加hash的计数field
                    operations.opsForHash().putAll(userCountHashKey, userCountMap);
                    // 设置随机过期时间 (2小时以内)
                    long expireTime = 60 * 60 + RandomUtil.randomInt(60 * 60);

                    operations.expire(userCountHashKey, expireTime, TimeUnit.SECONDS);

                    return null;
                  }
                });
          });
    }
  }
}
