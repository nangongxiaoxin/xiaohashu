package com.slilio.xiaohashu.user.relation.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.PageResponse;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.DateUtils;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import com.slilio.xiaohashu.user.relation.biz.constant.MQConstants;
import com.slilio.xiaohashu.user.relation.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.user.relation.biz.domain.dataobject.FansDO;
import com.slilio.xiaohashu.user.relation.biz.domain.dataobject.FollowingDO;
import com.slilio.xiaohashu.user.relation.biz.domain.mapper.FansDOMapper;
import com.slilio.xiaohashu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.slilio.xiaohashu.user.relation.biz.enums.LuaResultEnum;
import com.slilio.xiaohashu.user.relation.biz.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.user.relation.biz.model.dto.FollowUserMqDTO;
import com.slilio.xiaohashu.user.relation.biz.model.dto.UnfollowUserMqDTO;
import com.slilio.xiaohashu.user.relation.biz.model.vo.*;
import com.slilio.xiaohashu.user.relation.biz.rpc.UserRpcService;
import com.slilio.xiaohashu.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

/**
 * @Author: slilio @CreateTime: 2025-04-19 @Description: 关注接口 @Version: 1.0
 */
@Service
@Slf4j
public class RelationServiceImpl implements RelationService {
  @Resource private UserRpcService userRpcService;
  @Resource private RedisTemplate<String, Object> redisTemplate;
  @Resource private FollowingDOMapper followingDOMapper;
  @Resource private FansDOMapper fansDOMapper;
  @Resource private RocketMQTemplate rocketMQTemplate;

  @Resource(name = "taskExecutor")
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  /**
   * 关注用户
   *
   * @param followUserReqVO
   * @return
   */
  @Override
  public Response<?> follow(FollowUserReqVO followUserReqVO) {
    // 关注的用户ID
    Long followUserId = followUserReqVO.getFollowUserId();

    // 当前登录的用户ID
    Long userId = LoginUserContextHolder.getUserId();

    // 校验：无法关注自己
    if (Objects.equals(userId, followUserId)) {
      throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOUR_SELF);
    }

    // 检验关注的用户是否存在
    FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(followUserId);

    if (Objects.isNull(findUserByIdRspDTO)) {
      throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
    }
    // 构建当前用户关注列表的Redis Key
    String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);

    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    // Lua 脚本路径
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_add.lua")));
    // 返回值类型
    script.setResultType(Long.class);

    // 当前时间
    LocalDateTime now = LocalDateTime.now();
    // 当前时间的时间戳
    Long timestamp = DateUtils.localDateTime2Timestamp(now);

    // 执行Lua脚本
    Long result =
        redisTemplate.execute(
            script, Collections.singletonList(followingRedisKey), followUserId, timestamp);
    // 校验 Lua 脚本执行结果
    checkLuaScriptResult(result);

    // ZSET 不存在
    if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
      // 从数据库查询当前用户的关注关系记录
      List<FollowingDO> followingDOS = followingDOMapper.selectByUserId(userId);

      // 随机过期时间
      // 保底1天+随机秒数
      long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

      // 若记录为空，直接 ZADD 对象, 并设置过期时间
      if (CollUtil.isEmpty(followingDOS)) {
        DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
        script2.setScriptSource(
            new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua")));
        script2.setResultType(Long.class);

        // TODO: 可以根据用户类型，设置不同的过期时间，若当前用户为大V, 则可以过期时间设置的长些或者不设置过期时间；如不是，则设置的短些
        // 如何判断呢？可以从计数服务获取用户的粉丝数，目前计数服务还没创建，则暂时采用统一的过期策
        redisTemplate.execute(
            script2,
            Collections.singletonList(followingRedisKey),
            followUserId,
            timestamp,
            expireSeconds);

      } else {
        // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
        // 构建 Lua 参数
        Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

        // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
        DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
        script3.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
        script3.setResultType(Long.class);
        redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);

        // 再次调用上面的 Lua 脚本：follow_check_and_add.lua , 将最新的关注关系添加进去
        result =
            redisTemplate.execute(
                script, Collections.singletonList(followingRedisKey), followUserId, timestamp);
        checkLuaScriptResult(result);
      }
    }

    // 发送 MQ
    // 构建消息体DTO
    FollowUserMqDTO followUserMqDTO =
        FollowUserMqDTO.builder().userId(userId).followUserId(followUserId).createTime(now).build();
    // 构建消息对象，并将DTO转成Json字符串设置到消息体中
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(followUserMqDTO)).build();

    // 通过冒号链接，可让MQ发送主题Topic时，携带上标签Tag
    String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;
    log.info("===》 开始发送关注操作MQ，消息体：{}", followUserMqDTO);

    String hashKey = String.valueOf(userId);
    // 异步发送MQ消息，提升接口响应速度
    rocketMQTemplate.asyncSendOrderly(
        destination,
        message,
        hashKey,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("===》 MQ发送成功，SendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.error("===》 MQ发送异常：", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 取关用户
   *
   * @param unfollowUserReqVO
   * @return
   */
  @Override
  public Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO) {
    // 想要取关的用户
    Long unfollowUserId = unfollowUserReqVO.getUnfollowUserId();
    // 当前登录的用户id
    Long userId = LoginUserContextHolder.getUserId();

    // 无法取关自己
    if (Objects.equals(userId, unfollowUserId)) {
      throw new BizException(ResponseCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
    }

    // 校验关注用户是否存在
    FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(unfollowUserId);
    if (Objects.isNull(findUserByIdRspDTO)) {
      throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXISTED);
    }

    // 当前关注用户的Redis key
    String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);

    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    // Lua脚本路径
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/unfollow_check_and_delete.lua")));
    // 返回值类型
    script.setResultType(Long.class);
    // 执行Lua脚本
    Long result =
        redisTemplate.execute(script, Collections.singletonList(followingRedisKey), unfollowUserId);

    // 校验lua脚本执行结果
    // 取关的用户不在关注列表中
    if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
      throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
    }

    // ZSET关注列表不存在
    if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
      // 从数据库查询当前用户的关注关系记录
      List<FollowingDO> followingDOS = followingDOMapper.selectByUserId(userId);

      // 随机过期时间
      // 保底1条+随机秒数
      long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
      // 若记录为空，则表示还未关注任何人，提示还未关注对方
      if (CollUtil.isEmpty(followingDOS)) {
        throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
      } else {
        // 若记录不为空，则将关注关系数据全量同步到redis中，并设置过期时间
        // 构建lua参数
        Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

        // 执行lua脚本，批量同步关注关系到redis中
        DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
        script3.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
        script3.setResultType(Long.class);
        redisTemplate.execute(script3, Collections.singletonList(followingRedisKey), luaArgs);

        // 再次调用上面的lua脚本：unfollow_check_and_delete.lua，将取关的用户删除
        result =
            redisTemplate.execute(
                script, Collections.singletonList(followingRedisKey), unfollowUserId);
        // 再次校验结果
        if (Objects.equals(result, LuaResultEnum.NOT_FOLLOWED.getCode())) {
          throw new BizException(ResponseCodeEnum.NOT_FOLLOWED);
        }
      }
    }

    // 发送MQ-----------------------
    // 构建消息体DTO
    UnfollowUserMqDTO unfollowUserMqDTO =
        UnfollowUserMqDTO.builder()
            .userId(userId)
            .unfollowUserId(unfollowUserId)
            .createTime(LocalDateTime.now())
            .build();

    // 构建消息对象，并将DTO转换成Json字符串设置到消息体中
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(unfollowUserMqDTO)).build();

    // 通过冒号连接,让topic携带tag
    String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_UNFOLLOW;

    log.info("===》 开始发送取关操作MQ，消息体：{}", unfollowUserMqDTO);

    String hashKey = String.valueOf(userId);
    // 异步发送MQ消息，提升响应速度
    rocketMQTemplate.asyncSendOrderly(
        destination,
        message,
        hashKey,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("===》 MQ发送成功，SendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("===》 MQ发送失败，throwable：{}", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 查询关注列表
   *
   * @param findFollowingListReqVO
   * @return
   */
  @Override
  public PageResponse<FindFollowingUserRspVO> findFollowingList(
      FindFollowingListReqVO findFollowingListReqVO) {
    // 想要查询的用户ID
    Long userId = findFollowingListReqVO.getUserId();
    // 页码
    Integer pageNo = findFollowingListReqVO.getPageNo();

    // 先从redis查
    String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);

    // 查询目标用户关注列表ZSET的总大小
    long total = redisTemplate.opsForZSet().zCard(followingRedisKey);

    // 返参
    List<FindFollowingUserRspVO> findFollowingUserRspVOS = null;

    // 每页展示10条数据
    long limit = 10;

    if (total > 0) {
      // 缓存中有数据

      // 计算一共多少页
      long totalPage = PageResponse.getTotalPage(total, limit);

      // 请求的页码超出了总页数
      if (pageNo > totalPage) {
        PageResponse.success(null, pageNo, total);
      }

      // 准备从redis中查询ZSET分页数据
      // 每页10个元素，计算偏移量
      long offset = PageResponse.getOffset(pageNo, limit);

      // ZREVRANGEBYSCORE  命令按 score 降序获取元素，同时使用limit字句实现分页
      // 注意：这里使用Double.POSUTIVE_INFINITY 和 double.NEGATIVE_INFINITY 作为分数范围
      // 因为关注列表最多有1000个元素 ，这样可以确保到所有的元素
      Set<Object> followingUserIdsSet =
          redisTemplate
              .opsForZSet()
              .reverseRangeByScore(
                  followingRedisKey,
                  Double.NEGATIVE_INFINITY,
                  Double.POSITIVE_INFINITY,
                  offset,
                  limit);

      if (CollUtil.isNotEmpty(followingUserIdsSet)) {
        // 提取所有用户ID到集合中
        List<Long> userIds =
            followingUserIdsSet.stream().map(object -> Long.valueOf(object.toString())).toList();

        // RPC：调用用户服务，并将DTO转换位VO
        findFollowingUserRspVOS = rpcUserServiceAndDTO2VO(userIds, findFollowingUserRspVOS);
      }
    } else {
      // 若redis中没有数据，则从数据库查询
      // 先查询记录总量
      long count = followingDOMapper.selectCountByUserId(userId);

      // 计算一共多少页
      long totalPage = PageResponse.getTotalPage(count, limit);

      // 请求的页码超过了总页数
      if (pageNo > totalPage) {
        PageResponse.success(null, pageNo, total);
      }

      // 偏移量
      long offset = PageResponse.getOffset(pageNo, limit);

      // 分页查询
      List<FollowingDO> followingDOS =
          followingDOMapper.selectPageListByUserId(userId, offset, limit);
      // 赋值真实的记录总数
      total = count;

      // 若记录不为空
      if (CollUtil.isNotEmpty(followingDOS)) {
        // 提取所有关注用户id到集合中
        List<Long> userIds = followingDOS.stream().map(FollowingDO::getFollowingUserId).toList();

        // RPC：调用用户服务，并将DTO转换为VO
        findFollowingUserRspVOS = rpcUserServiceAndDTO2VO(userIds, findFollowingUserRspVOS);

        // todo ： 异步将关注列表全量同步到redis中
        threadPoolTaskExecutor.submit(() -> syncFollowingList2Redis(userId));
      }
    }

    return PageResponse.success(findFollowingUserRspVOS, pageNo, total);
  }

  /**
   * 查询粉丝列表
   *
   * @param findFansListReqVO
   * @return
   */
  @Override
  public PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO findFansListReqVO) {
    // 想要查询的用户ID
    Long userId = findFansListReqVO.getUserId();
    // 页码
    Integer pageNo = findFansListReqVO.getPageNo();

    // 先从redis中查询
    String fansRedisKey = RedisKeyConstants.buildUserFansKey(userId);

    // 查询目标用户粉丝列表ZSet的总大小
    long total = redisTemplate.opsForZSet().zCard(fansRedisKey);

    // 返参
    List<FindFansUserRspVO> findFansUserRspVOS = null;

    // 每页展示10条数据
    long limit = 10;

    if (total > 0) {
      // 缓存中有数据

      // 计算一共多少页
      long totalPage = PageResponse.getTotalPage(total, limit);

      // 请求的页码超过了总页数
      if (pageNo > totalPage) {
        return PageResponse.success(null, pageNo, total);
      }

      // 准备从redis中获取数据
      // 每页10个元素，计算偏移量
      long offset = PageResponse.getOffset(pageNo, limit);

      // 使用 ZREVRANGEBYSCORE 命令按 score 降序获取元素，同时使用 LIMIT 子句实现分页
      Set<Object> followingUserIdsSet =
          redisTemplate
              .opsForZSet()
              .reverseRangeByScore(
                  fansRedisKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, limit);

      if (CollUtil.isNotEmpty(followingUserIdsSet)) {
        // 提取所有的用户ID到集合中
        List<Long> userIds =
            followingUserIdsSet.stream().map(object -> Long.valueOf(object.toString())).toList();

        // RPC： 批量查询用户信息
        findFansUserRspVOS = rpcUserServiceAndCountServiceAndDTO2VO(userIds, findFansUserRspVOS);
      }
    } else { // 若redis缓存中无数据，则查询数据库

      // 先查询记录总数
      total = fansDOMapper.selectCountByUserId(userId);

      // 计算一共多少页
      long totalPage = PageResponse.getTotalPage(total, limit);

      // 请求的页码超过了总页数（只允许查询前500页）
      if (pageNo > 500 || pageNo > totalPage) {
        return PageResponse.success(null, pageNo, total);
      }

      // 偏移量
      long offset = PageResponse.getOffset(pageNo, limit);

      // 分页查询
      List<FansDO> fansDOS = fansDOMapper.selectPageListByUserId(userId, offset, limit);

      // 若记录不为空
      if (CollUtil.isNotEmpty(fansDOS)) {
        // 提取所有粉丝用户ID到集合中
        List<Long> userIds = fansDOS.stream().map(FansDO::getFansUserId).toList();

        // RPC：调用用户服务、计数服务、并将DTO转换为VO
        findFansUserRspVOS = rpcUserServiceAndCountServiceAndDTO2VO(userIds, findFansUserRspVOS);

        // 异步将粉丝列表同步到redis中（最多5000条）
        threadPoolTaskExecutor.submit(() -> syncFansList2Redis(userId));
      }
    }

    return PageResponse.success(findFansUserRspVOS, pageNo, total);
  }

  /**
   * 粉丝列表同步到redis（最多5000条）
   *
   * @param userId
   * @return
   */
  private void syncFansList2Redis(Long userId) {
    // 查询粉丝列表（最多5000位用户）
    List<FansDO> fansDOS = fansDOMapper.select5000FansByUserId(userId);
    if (CollUtil.isNotEmpty(fansDOS)) {
      // 用户粉丝列表Redis Key
      String fansRedisKey = RedisKeyConstants.buildUserFansKey(userId);
      // 随机过期时间
      // 保底1天+随机秒数
      long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
      // 构建Lua参数
      Object[] luaArgs = buildFansZSetLuaArgs(fansDOS, expireSeconds);

      // 执行Lua脚本，批量同步关注关系到redis中
      DefaultRedisScript<Long> script = new DefaultRedisScript<>();
      script.setScriptSource(
          new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
      script.setResultType(Long.class);
      redisTemplate.execute(script, Collections.singletonList(fansRedisKey), luaArgs);
    }
  }

  /**
   * 构建Lua脚本参数：粉丝列表
   *
   * @param fansDOS
   * @param expireSeconds
   * @return
   */
  private Object[] buildFansZSetLuaArgs(List<FansDO> fansDOS, long expireSeconds) {
    int argsLength = fansDOS.size() * 2 + 1; // 每个粉丝有两个参数（score和value），再加一个过期时间
    Object[] luaArgs = new Object[argsLength];

    int i = 0;
    for (FansDO fansDO : fansDOS) {
      luaArgs[i] = DateUtils.localDateTime2Timestamp(fansDO.getCreateTime()); // 粉丝的关注时间为score
      luaArgs[i + 1] = fansDO.getFansUserId();
      i += 2;
    }
    luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数为ZSet过期时间
    return luaArgs;
  }

  /**
   * RPC：调用用户服务、计数服务、并将DTO转换为VO粉丝列表
   *
   * @param userIds
   * @param findFansUserRspVOS
   * @return
   */
  private List<FindFansUserRspVO> rpcUserServiceAndCountServiceAndDTO2VO(
      List<Long> userIds, List<FindFansUserRspVO> findFansUserRspVOS) {
    // RPC：批量查询用户信息
    List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);

    // todo：rpc：批量查询用户的计数数据（笔记总数，分数总数）

    // 若不为空，DTO转VO
    if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
      findFansUserRspVOS =
          findUserByIdRspDTOS.stream()
              .map(
                  dto ->
                      FindFansUserRspVO.builder()
                          .userId(dto.getId())
                          .avatar(dto.getAvatar())
                          .nickname(dto.getNickName())
                          .noteTotal(999L) // todo ： 数据待后续补充
                          .fansTotal(888L) // todo ： 数据待后续补充
                          .build())
              .toList();
    }
    return findFansUserRspVOS;
  }

  /**
   * 全量同步关注列表到redis中
   *
   * @param userId
   */
  private void syncFollowingList2Redis(Long userId) {
    // 查询全量关注用户列表（1000位用户）
    List<FollowingDO> followingDOS = followingDOMapper.selectAllByUserId(userId);
    if (CollUtil.isNotEmpty(followingDOS)) {
      // 用户关注列表RedisKey
      String followingRedisKey = RedisKeyConstants.buildUserFollowingKey(userId);
      // 随机过期时间
      long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
      // 构建lua参数
      Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

      // 执行lua脚本，批量同步到redis中
      DefaultRedisScript<Long> script = new DefaultRedisScript<>();
      script.setScriptSource(
          new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
      script.setResultType(Long.class);
      redisTemplate.execute(script, Collections.singletonList(followingRedisKey), luaArgs);
    }
  }

  /**
   * RPC：调用用户服务，并将DTO转为VO
   *
   * @param userIds
   * @param findFollowingUserRspVOS
   * @return
   */
  private List<FindFollowingUserRspVO> rpcUserServiceAndDTO2VO(
      List<Long> userIds, List<FindFollowingUserRspVO> findFollowingUserRspVOS) {
    // RPC：批量查询用户信息
    List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);

    // 若不为空 DTO转VO
    if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
      findFollowingUserRspVOS =
          findUserByIdRspDTOS.stream()
              .map(
                  dto ->
                      FindFollowingUserRspVO.builder()
                          .userId(dto.getId())
                          .avatar(dto.getAvatar())
                          .nickname(dto.getNickName())
                          .introduction(dto.getIntroduction())
                          .build())
              .toList();
    }
    return findFollowingUserRspVOS;
  }

  /**
   * 校验 Lua 脚本结果，根据状态码抛出对应的业务异常
   *
   * @param result
   */
  private static void checkLuaScriptResult(Long result) {
    LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);

    if (Objects.isNull(luaResultEnum)) throw new RuntimeException("Lua 返回结果错误");
    // 校验 Lua 脚本执行结果
    switch (luaResultEnum) {
      // 关注数已达到上限
      case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOWING_COUNT_LIMIT);
      // 已经关注了该用户
      case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
    }
  }

  /**
   * 构建 Lua 脚本参数
   *
   * @param followingDOS
   * @param expireSeconds
   * @return
   */
  private static Object[] buildLuaArgs(List<FollowingDO> followingDOS, long expireSeconds) {
    int argsLength = followingDOS.size() * 2 + 1; // 每个关注关系有 2 个参数（score 和 value），再加一个过期时间
    Object[] luaArgs = new Object[argsLength];

    int i = 0;
    for (FollowingDO following : followingDOS) {
      luaArgs[i] = DateUtils.localDateTime2Timestamp(following.getCreateTime()); // 关注时间作为 score
      luaArgs[i + 1] = following.getFollowingUserId(); // 关注的用户 ID 作为 ZSet value
      i += 2;
    }

    luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
    return luaArgs;
  }
}
