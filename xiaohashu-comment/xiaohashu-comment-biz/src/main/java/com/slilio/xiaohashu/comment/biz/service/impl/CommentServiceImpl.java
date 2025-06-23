package com.slilio.xiaohashu.comment.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.constant.DateConstants;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.PageResponse;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.DateUtils;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.comment.biz.constant.MQConstants;
import com.slilio.xiaohashu.comment.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.comment.biz.domain.dataobject.CommentDO;
import com.slilio.xiaohashu.comment.biz.domain.dataobject.CommentLikeDO;
import com.slilio.xiaohashu.comment.biz.domain.mapper.CommentDOMapper;
import com.slilio.xiaohashu.comment.biz.domain.mapper.CommentLikeDOMapper;
import com.slilio.xiaohashu.comment.biz.domain.mapper.NoteCountDOMapper;
import com.slilio.xiaohashu.comment.biz.enums.*;
import com.slilio.xiaohashu.comment.biz.model.dto.LikeUnlikeCommentMqDTO;
import com.slilio.xiaohashu.comment.biz.model.dto.PublishCommentMqDTO;
import com.slilio.xiaohashu.comment.biz.model.vo.*;
import com.slilio.xiaohashu.comment.biz.retry.SendMqRetryHelper;
import com.slilio.xiaohashu.comment.biz.rpc.DistributedIdGeneratorRpcService;
import com.slilio.xiaohashu.comment.biz.rpc.KeyValueRpcService;
import com.slilio.xiaohashu.comment.biz.rpc.UserRpcService;
import com.slilio.xiaohashu.comment.biz.service.CommentService;
import com.slilio.xiaohashu.kv.dto.req.FindCommentContentReqDTO;
import com.slilio.xiaohashu.kv.dto.rsp.FindCommentContentRspDTO;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: @Version: 1.0
 */
@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

  @Resource private RocketMQTemplate rocketMQTemplate;
  @Resource private SendMqRetryHelper sendMqRetryHelper;
  @Resource private CommentDOMapper commentDOMapper;
  @Resource private NoteCountDOMapper noteCountDOMapper;
  @Resource private CommentLikeDOMapper commentLikeDOMapper;
  @Resource private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
  @Resource private KeyValueRpcService keyValueRpcService;
  @Resource private UserRpcService userRpcService;
  @Resource private RedisTemplate<String, Object> redisTemplate;

  @Resource(name = "taskExecutor")
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  /** 评论详情本地缓存 实例化 */
  private static final Cache<Long, String> LOCAL_CACHE =
      Caffeine.newBuilder()
          .initialCapacity(10000) // 设置初始容量为10000个条目
          .maximumSize(10000) // 设置缓存的最大容量为10000个条目
          .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后1小时过期
          .build();

  @Autowired private TransactionTemplate transactionTemplate;

  /**
   * 发布评论
   *
   * @param publishCommentReqVO
   * @return
   */
  @Override
  public Response<?> publishComment(PublishCommentReqVO publishCommentReqVO) {
    // 评论正文
    String content = publishCommentReqVO.getContent();
    // 附近图片
    String imageUrl = publishCommentReqVO.getImgUrl();

    // 评论内容和图片不能同时为空
    Preconditions.checkArgument(
        StringUtils.isNotBlank(content) || StringUtils.isBlank(imageUrl), "评论内容和图片不能同时为空");

    // 发布者ID
    Long creatorId = LoginUserContextHolder.getUserId();

    // RPC：调用分布式ID生成服务，生成评论ID
    String commentId = distributedIdGeneratorRpcService.generateCommentId();

    // 发送MQ
    // 构建消息体DTO
    PublishCommentMqDTO publishCommentMqDTO =
        PublishCommentMqDTO.builder()
            .noteId(publishCommentReqVO.getNoteId())
            .content(content)
            .imageUrl(imageUrl)
            .replyCommentId(publishCommentReqVO.getReplyCommentId())
            .createTime(LocalDateTime.now())
            .creatorId(creatorId)
            .commentId(Long.valueOf(commentId))
            .build();

    // 发送MQ（包含重试策略）
    sendMqRetryHelper.asyncSend(
        MQConstants.TOPIC_PUBLISH_COMMENT, JsonUtils.toJsonString(publishCommentMqDTO));

    return Response.success();
  }

  /**
   * 评论列表分页查询
   *
   * @param findCommentPageListReqVO
   * @return
   */
  @Override
  public PageResponse<FindCommentItemRspVO> findCommentPageList(
      FindCommentPageListReqVO findCommentPageListReqVO) {

    // 笔记ID
    Long noteId = findCommentPageListReqVO.getNoteId();
    // 当前页码
    Integer pageNo = findCommentPageListReqVO.getPageNo();
    // 每页展示一级评论数
    long pageSize = 10;

    // redis缓存中查询数据
    String noteCommentTotalKey = RedisKeyConstants.buildNoteCommentTotalKey(noteId);
    // 先从redis中查询该笔记的评论总数
    Number commentTotal =
        (Number)
            redisTemplate
                .opsForHash()
                .get(noteCommentTotalKey, RedisKeyConstants.FIELD_COMMENT_TOTAL);
    long count = Objects.isNull(commentTotal) ? 0L : commentTotal.longValue();

    // 若缓存不存在，则查询数据库
    if (Objects.isNull(commentTotal)) {
      // 查询评论总数（从t_note_count笔记计数表查询，提升查询性能，避免count(*)）
      Long dbCount = noteCountDOMapper.selectCommentTotalByNoteId(noteId);
      // 若数据库也不存在，抛出异常
      if (Objects.isNull(dbCount)) {
        throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
      }

      count = dbCount;
      // 异步将评论结果同步到redis中
      threadPoolTaskExecutor.execute(
          () -> syncNoteCommentTotal2Redis(noteCommentTotalKey, dbCount));
    }

    // 若评论总数为0，直接返回响应
    if (count == 0) {
      return PageResponse.success(null, pageNo, 0);
    }

    // 分页返参
    List<FindCommentItemRspVO> commentRspVOS = Lists.newArrayList();

    // 计算分页查询的偏移量offset
    long offset = PageResponse.getOffset(pageNo, pageSize);

    // 评论分页缓存用ZSET+String 实现
    // 构建评论ZSET key
    String commentZSetKey = RedisKeyConstants.buildCommentListKey(noteId);
    // 先判断ZSET是否存在
    boolean hasKey = redisTemplate.hasKey(commentZSetKey);

    // 若ZSET不存在
    if (!hasKey) {
      // 异步将热点评论同步到redis中（最多同步500条）
      threadPoolTaskExecutor.execute(() -> syncHeatComments2Redis(commentZSetKey, noteId));
    }

    // 若ZSET缓存存在，并且查询的是前50页的评论
    if (hasKey && offset < 500) {
      // 使用ZRevRange获取某篇笔记下，按照热度降序排列的一级评论ID
      Set<Object> commentIds =
          redisTemplate
              .opsForZSet()
              .reverseRangeByScore(
                  commentZSetKey, -Double.MAX_VALUE, Double.MAX_VALUE, offset, pageSize);

      // 若结果不为空
      if (CollUtil.isNotEmpty(commentIds)) {
        // Set转List
        List<Object> commentIdList = Lists.newArrayList(commentIds);

        // 先查询本地缓存
        // 新建一个集合，用于存储本地缓存中不存在的评论ID
        List<Long> localCacheExpiredCommentIds = Lists.newArrayList();
        // 构建查询本地缓存的Key集合
        List<Long> localCacheKeys =
            commentIdList.stream().map(commentId -> Long.valueOf(commentId.toString())).toList();

        // 批量查询本地缓存
        Map<Long, String> commentIdAndDetailJsonMap =
            LOCAL_CACHE.getAll(
                localCacheKeys,
                missingKeys -> {
                  // 对于本地缓存中缺失的 key，返回空字符串
                  Map<Long, String> missingData = Maps.newHashMap();
                  missingKeys.forEach(
                      key -> {
                        // 记录缓存中不存在的评论 ID
                        localCacheExpiredCommentIds.add(key);
                        // 不存在的评论详情, 对其 Value 值设置为空字符串
                        missingData.put(key, Strings.EMPTY);
                      });
                  return missingData;
                });

        // 若 localCacheExpiredCommentIds 的大小不等于 commentIdList 的大小，说明本地缓存中有数据
        if (CollUtil.size(localCacheExpiredCommentIds) != commentIdList.size()) {
          // 将本地缓存中的评论详情 Json, 转换为实体类，添加到 VO 返参集合中
          for (String value : commentIdAndDetailJsonMap.values()) {
            if (StringUtils.isBlank(value)) continue;
            FindCommentItemRspVO commentRspVO =
                JsonUtils.parseObject(value, FindCommentItemRspVO.class);
            commentRspVOS.add(commentRspVO);
          }
        }

        // 若localCacheExpiredCommentIds 大小等于0，说明评论详情数据都在本地缓存中，直接响应返参
        if (CollUtil.size(localCacheExpiredCommentIds) == 0) {
          // 计数数据需要从Redis中查询
          if (CollUtil.isNotEmpty(commentRspVOS)) {
            setCommentCountData(commentRspVOS, localCacheExpiredCommentIds);
          }

          return PageResponse.success(commentRspVOS, pageNo, count, pageSize);
        }

        // 构建MGET批量查询评论详情的Key集合
        List<String> commentIdKeys =
            localCacheExpiredCommentIds.stream()
                .map(RedisKeyConstants::buildCommentDetailKey)
                .toList();

        // MGET批量获取评论数据
        List<Object> commentsJsonList = redisTemplate.opsForValue().multiGet(commentIdKeys);

        // 可能存在部分评论不在缓存中，已经过期被删除，这些评论ID需要提取出来，用来查询数据库
        List<Long> expiredCommentIds = Lists.newArrayList();
        for (int i = 0; i < commentsJsonList.size(); i++) {
          String commentJson = (String) commentsJsonList.get(i);
          Long commentId = Long.valueOf(localCacheExpiredCommentIds.get(i).toString());
          if (Objects.nonNull(commentJson)) {
            // 缓存中存在的评论Json，直接转换为VO添加到返参集合中
            FindCommentItemRspVO commentRspVO =
                JsonUtils.parseObject(commentJson, FindCommentItemRspVO.class);
            commentRspVOS.add(commentRspVO);
          } else {
            // 评论失效，添加到失效评论列表
            expiredCommentIds.add(commentId);
          }
        }

        // 对应缓存存在的评论详情，需要再次查询其计数数据
        if (CollUtil.isNotEmpty(commentRspVOS)) {
          setCommentCountData(commentRspVOS, expiredCommentIds);
        }

        // 对于不存在的一级评论，需要批量从数据库中查询，并添加到commentRspVOS中
        if (CollUtil.isNotEmpty(expiredCommentIds)) {
          List<CommentDO> commentDOS = commentDOMapper.selectByCommentIds(expiredCommentIds);
          getCommentDataAndSync2Redis(commentDOS, noteId, commentRspVOS);
        }
      }

      // 按照热度值进行降序排列
      commentRspVOS =
          commentRspVOS.stream()
              .sorted(Comparator.comparing(FindCommentItemRspVO::getHeat).reversed())
              .collect(Collectors.toList());

      // 异步将评论详情，同步到本地缓存
      syncCommentDetail2LocalCache(commentRspVOS);

      return PageResponse.success(commentRspVOS, pageNo, count, pageSize);
    }

    // 缓存中没有，则查询数据库
    // 查询一级评论
    List<CommentDO> oneLevelCommentDOS = commentDOMapper.selectPageList(noteId, offset, pageSize);
    getCommentDataAndSync2Redis(oneLevelCommentDOS, noteId, commentRspVOS);

    // 异步将评论详情，同步到本地缓存
    syncCommentDetail2LocalCache(commentRspVOS);

    return PageResponse.success(commentRspVOS, pageNo, count, pageSize);
  }

  /**
   * 二级评论分页查询
   *
   * @param findChildCommentPageListReqVO
   * @return
   */
  @Override
  public PageResponse<FindChildCommentItemRspVO> findChildCommentPageList(
      FindChildCommentPageListReqVO findChildCommentPageListReqVO) {

    // 父评论
    Long parentCommentId = findChildCommentPageListReqVO.getParentCommentId();
    // 当前页码
    Integer pageNo = findChildCommentPageListReqVO.getPageNo();
    // 每页展示的二级评论数（小红书为1次6条）
    long pageSize = 6;

    // 先从缓存查询
    String countCommentKey = RedisKeyConstants.buildCountCommentKey(parentCommentId);
    // 子评论总数
    Number redisCount =
        (Number)
            redisTemplate
                .opsForHash()
                .get(countCommentKey, RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL);
    long count = Objects.isNull(redisCount) ? 0 : redisCount.longValue();

    // 若缓存不存在，走数据库查询
    if (Objects.isNull(redisCount)) {
      // 查询一级评论下子评论的总数（直接查询t_comment表的child_comment_total字段，提升查询性能，避免count(*)）
      Long dbCount = commentDOMapper.selectChildCommentTotalById(parentCommentId);
      // 若数据库中也没有，抛出异常
      if (Objects.isNull(dbCount)) {
        throw new BizException(ResponseCodeEnum.PARENT_COMMENT_NOT_FOUNT);
      }

      count = dbCount;
      // 异步将子评论总数同步到Redis中
      threadPoolTaskExecutor.execute(
          () -> {
            syncCommentCount2Redis(countCommentKey, dbCount);
          });
    }

    // 若子评论总数为0
    if (count == 0) {
      return PageResponse.success(null, pageNo, 0);
    }

    // 遍历这个父级评论ID下的子评论数据，封装返回    遍历childCommentDOS，因为内部包含了子评论的信息

    // 分页返参
    List<FindChildCommentItemRspVO> childCommentRspVOS = Lists.newArrayList();

    // 计算分页查询的偏移量offset（需要+1，因为最早回复的二级评论已经被展示了）
    long offset = PageResponse.getOffset(pageNo, pageSize) + 1;

    // 子评论分页缓存使用ZSET+STRING实现
    // 构建子评论ZSET key
    String childCommentZSetKey = RedisKeyConstants.buildChildCommentListKey(parentCommentId);
    // 先判断zset是否存在
    boolean hasKey = redisTemplate.hasKey(childCommentZSetKey);

    // 若不存在
    if (!hasKey) {
      // 异步将子评论同步到Redis中（最多同步6*10条）
      threadPoolTaskExecutor.execute(
          () -> {
            syncChildComment2Redis(parentCommentId, childCommentZSetKey);
          });
    }

    // 若子评论ZSET缓存存在，并且查询的是前10页的子评论
    if (hasKey && offset < 6 * 10) {
      // 使用ZRevRange 获取某个一级评论下的子评论下的子评论，按照回复时间升序排列
      Set<Object> childCommentIds =
          redisTemplate
              .opsForZSet()
              .rangeByScore(childCommentZSetKey, 0, Double.MAX_VALUE, offset, pageSize);

      // 若结果不为空
      if (CollUtil.isNotEmpty(childCommentIds)) {
        // set转list
        List<Object> childCommentIdsList = Lists.newArrayList(childCommentIds);

        // 构建MGET批量查询子评论详情的key集合
        List<String> commentIdKeys =
            childCommentIdsList.stream().map(RedisKeyConstants::buildCommentDetailKey).toList();

        // MGET批量获取评论数据
        List<Object> commentsJsonList = redisTemplate.opsForValue().multiGet(commentIdKeys);

        // 可能存在部分评论不在缓存中，已经过期，这些评论ID需要提取出来，等会查数据库
        List<Long> expiredChildCommentIds = Lists.newArrayList();

        for (int i = 0; i < commentsJsonList.size(); i++) {
          String commentJson = (String) commentsJsonList.get(i);
          Long commentId = Long.valueOf(childCommentIdsList.get(i).toString());
          if (Objects.isNull(commentJson)) {
            // 缓存中存在的评论Json，直接转换为VO添加到返参集合中
            FindChildCommentItemRspVO childCommentRspVO =
                JsonUtils.parseObject(commentJson, FindChildCommentItemRspVO.class);
            childCommentRspVOS.add(childCommentRspVO);
          } else {
            // 评论失效，添加到失效评论列表
            expiredChildCommentIds.add(commentId);
          }
        }

        // 对于缓存中存在的子评论，需要再次查询Hash，获取其计数数据
        if (CollUtil.isNotEmpty(childCommentRspVOS)) {
          setChildCommentCountData(childCommentRspVOS, expiredChildCommentIds);
        }

        // 对于不存在的子评论，需要批量从数据库中查询，并添加到commentRspVOS中
        if (CollUtil.isNotEmpty(expiredChildCommentIds)) {
          List<CommentDO> commentDOS = commentDOMapper.selectByCommentIds(expiredChildCommentIds);
          getChildCommentDataAndSync2Redis(commentDOS, childCommentRspVOS);
        }

        // 按照评论ID升序排列（等同于按照时间升序）
        childCommentRspVOS =
            childCommentRspVOS.stream()
                .sorted(Comparator.comparing(FindChildCommentItemRspVO::getCommentId))
                .collect(Collectors.toList());

        return PageResponse.success(childCommentRspVOS, pageNo, count, pageSize);
      }
    }

    // 分页查询子评论
    List<CommentDO> childCommentDOS =
        commentDOMapper.selectChildPageList(parentCommentId, offset, pageSize);

    // 调用kv服务的入参
    List<FindCommentContentReqDTO> findCommentContentReqDTOS = Lists.newArrayList();
    // 调用以用户服务的入参
    Set<Long> userIds = Sets.newHashSet();
    // 归属的笔记ID
    Long noteId = null;

    // 循环提取RPC调用需要的入参数据
    for (CommentDO childCommentDO : childCommentDOS) {
      noteId = childCommentDO.getNoteId();
      // 构建KV服务批量查询评论内容的入参
      boolean isContentEmpty = childCommentDO.getIsContentEmpty();

      if (!isContentEmpty) {
        FindCommentContentReqDTO findCommentContentReqDTO =
            FindCommentContentReqDTO.builder()
                .contentId(childCommentDO.getContentUuid())
                .yearMonth(DateConstants.DATE_FORMAT_Y_M.format(childCommentDO.getCreateTime()))
                .build();

        findCommentContentReqDTOS.add(findCommentContentReqDTO);
      }

      // 构建用户服务批量查询以用户信息的入参（包含评论、回复的目标用户）
      userIds.add(childCommentDO.getUserId());

      Long parentId = childCommentDO.getParentId();
      Long replyCommentId = childCommentDO.getReplyCommentId();
      // 检查当前子评论replyCommentId 回复的父级评论是谁。如果回复的不是1级评论，需要展示用户
      if (!Objects.equals(parentId, replyCommentId)) {
        userIds.add(childCommentDO.getReplyUserId());
      }
    }

    // RPC:调用KV服务，评论获取评论内容
    List<FindCommentContentRspDTO> findCommentContentRspDTOS =
        keyValueRpcService.batchFindCommentContent(noteId, findCommentContentReqDTOS);

    // DTO集合转Map，方便后续拼装数据
    Map<String, String> commentUuidAndContentMap = null;
    if (CollUtil.isNotEmpty(findCommentContentRspDTOS)) {
      commentUuidAndContentMap =
          findCommentContentRspDTOS.stream()
              .collect(
                  Collectors.toMap(
                      FindCommentContentRspDTO::getContentId,
                      FindCommentContentRspDTO::getContent));
    }

    // RPC：调用用户服务
    List<FindUserByIdRspDTO> findUserByIdRspDTOS =
        userRpcService.findByIds(userIds.stream().toList());

    // DTO集合转Map，方便后续拼接数据
    Map<Long, FindUserByIdRspDTO> userIdAndDTOMap = null;
    if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
      userIdAndDTOMap =
          findUserByIdRspDTOS.stream()
              .collect(
                  Collectors.toMap(
                      FindUserByIdRspDTO::getId, findUserByIdRspDTO -> findUserByIdRspDTO));
    }

    // DO转VO
    for (CommentDO childCommentDO : childCommentDOS) {
      // 构建VO实体类
      Long userId = childCommentDO.getUserId();
      FindChildCommentItemRspVO childCommentRspVO =
          FindChildCommentItemRspVO.builder()
              .userId(userId)
              .commentId(childCommentDO.getId())
              .imageUrl(childCommentDO.getImageUrl())
              .crateTime(DateUtils.formatRelativeTime(childCommentDO.getCreateTime()))
              .likeTotal(childCommentDO.getLikeTotal())
              .build();

      // 填充用户信息（包含发布者评论，回复的用户）
      if (CollUtil.isNotEmpty(userIdAndDTOMap)) {
        FindUserByIdRspDTO findUserByIdRspDTO = userIdAndDTOMap.get(userId);
        // 评论发布者用户信息（头像、昵称）
        if (Objects.nonNull(findUserByIdRspDTO)) {
          childCommentRspVO.setAvatar(findUserByIdRspDTO.getAvatar());
          childCommentRspVO.setNickname(findUserByIdRspDTO.getNickName());
        }

        // 评论回复的哪个
        Long replyCommentId = childCommentDO.getReplyCommentId();
        Long parentId = childCommentDO.getParentId();

        if (Objects.nonNull(replyCommentId) && !Objects.equals(replyCommentId, parentId)) {
          Long replyUserId = childCommentDO.getReplyUserId();
          FindUserByIdRspDTO replyUser = userIdAndDTOMap.get(replyUserId);
          childCommentRspVO.setReplyUserName(replyUser.getNickName());
          childCommentRspVO.setReplyUserId(replyUser.getId());
        }
      }

      // 评论内容
      if (CollUtil.isNotEmpty(commentUuidAndContentMap)) {
        String contentUuid = childCommentDO.getContentUuid();
        if (StringUtils.isNotBlank(contentUuid)) {
          childCommentRspVO.setContent(commentUuidAndContentMap.get(contentUuid));
        }
      }
      childCommentRspVOS.add(childCommentRspVO);
    }
    return PageResponse.success(childCommentRspVOS, pageNo, count, pageSize);
  }

  /**
   * 评论点赞
   *
   * @param likeCommentReqVO
   * @return
   */
  @Override
  public Response<?> likeComment(LikeCommentReqVO likeCommentReqVO) {
    // 被点赞ID
    Long commentId = likeCommentReqVO.getCommentId();

    //  1. 校验被点赞的评论是否存在
    checkCommentIsExist(commentId);

    // 2. 判断目标评论，是否已经被点赞

    // 当前登录用户
    Long userId = LoginUserContextHolder.getUserId();
    // 布隆过滤器Key
    String bloomUserCommentLikeListKey = RedisKeyConstants.buildBloomCommentLikesKey(userId);
    // lua脚本执行
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/bloom_comment_like_check.lua")));
    script.setResultType(Long.class);
    // 执行
    Long result =
        redisTemplate.execute(
            script, Collections.singletonList(bloomUserCommentLikeListKey), commentId);

    CommentLikeLuaResultEnum commentLikeLuaResultEnum = CommentLikeLuaResultEnum.valueOf(result);
    if (Objects.isNull(commentLikeLuaResultEnum)) {
      throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
    }

    switch (commentLikeLuaResultEnum) {
      // redis中布隆过滤器不存在
      case NOT_EXIST -> {
        // 从数据库中校验评论是否被点赞，并异步初始化布隆过滤器，设置过期时间
        int count = commentLikeDOMapper.selectCountByUserIdAndCommentId(userId, commentId);
        // 过期时间
        long expireSeconds = 60 * 60 + RandomUtil.randomInt(60 * 60);
        // 如果已经被点赞
        if (count > 0) {
          // 异步初始化布隆过滤器
          threadPoolTaskExecutor.submit(
              () ->
                  batchAddCommentLike2BloomAndExpire(
                      userId, expireSeconds, bloomUserCommentLikeListKey));

          throw new BizException(ResponseCodeEnum.COMMENT_ALREADY_LIKED);
        }

        // 若目标评论未被点赞，查询当前用户是否有点赞其他评论，有则同步初始化布隆过滤器
        batchAddCommentLike2BloomAndExpire(userId, expireSeconds, bloomUserCommentLikeListKey);

        // 添加点赞当前评论ID到布隆过滤器 redis lua
        script.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/bloom_add_comment_like_and_expire.lua")));
        script.setResultType(Long.class);
        redisTemplate.execute(
            script,
            Collections.singletonList(bloomUserCommentLikeListKey),
            commentId,
            expireSeconds);
      }
      // 目标评论已经被点赞（可能误判，需要进一步确认）
      case COMMENT_LIKED -> {
        // 查询数据库校验是否点赞
        int count = commentLikeDOMapper.selectCountByUserIdAndCommentId(userId, commentId);

        if (count > 0) {
          throw new BizException(ResponseCodeEnum.COMMENT_ALREADY_LIKED);
        }
      }
    }

    // 3. 发送 MQ, 异步将评论点赞记录落库
    // 构建消息体
    LikeUnlikeCommentMqDTO likeUnlikeCommentMqDTO =
        LikeUnlikeCommentMqDTO.builder()
            .userId(userId)
            .commentId(commentId)
            .type(LikeUnlikeCommentTypeEnum.LIKE.getCode()) // 点赞评论
            .createTime(LocalDateTime.now())
            .build();

    // 构建消息体
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeCommentMqDTO)).build();
    // 主题及tag
    String destination = MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_LIKE;
    // MQ分区键
    String hashKey = String.valueOf(userId);

    // 异发送
    rocketMQTemplate.asyncSendOrderly(
        destination,
        message,
        hashKey,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==> 【评论点赞】MQ 发送成功，SendResult: {}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==> 【评论点赞】MQ 发送异常：", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 取消评论点赞
   *
   * @param unLikeCommentReqVO
   * @return
   */
  @Override
  public Response<?> unlikeComment(UnLikeCommentReqVO unLikeCommentReqVO) {
    // 被取消点赞的评论
    Long commentId = unLikeCommentReqVO.getCommentId();

    // 1.校验评论是否存在
    checkCommentIsExist(commentId);

    // 2. 校验评论是否被点赞过
    Long userId = LoginUserContextHolder.getUserId();
    // 布隆过滤器key
    String bloomUserCommentLikeListKey = RedisKeyConstants.buildBloomCommentLikesKey(userId);

    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/bloom_comment_unlike_check.lua")));
    script.setResultType(Long.class);

    // 执行Lua脚本
    Long result =
        redisTemplate.execute(
            script, Collections.singletonList(bloomUserCommentLikeListKey), commentId);

    CommentUnlikeLuaResultEnum commentUnlikeLuaResultEnum =
        CommentUnlikeLuaResultEnum.valueOf(result);

    if (Objects.isNull(commentUnlikeLuaResultEnum)) {
      throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
    }

    switch (commentUnlikeLuaResultEnum) {
      // 布隆过滤器不存在
      case NOT_EXIST -> {
        // 异步初始化布隆过滤器
        threadPoolTaskExecutor.submit(
            () -> {
              // 保底1小时+随机秒数
              long expireSeconds = 60 * 60 + RandomUtil.randomInt(60 * 60);
              batchAddCommentLike2BloomAndExpire(
                  userId, expireSeconds, bloomUserCommentLikeListKey);
            });
        // 从数据库中校验评论是否被点赞
        int count = commentLikeDOMapper.selectCountByUserIdAndCommentId(userId, commentId);

        // 未点赞，无法取消点赞操作，抛出业务异常
        if (count == 0) {
          throw new BizException(ResponseCodeEnum.COMMENT_NOT_LIKED);
        }
      }
      // 校验目标未被点赞（判断绝对正确）
      case COMMENT_NOT_LIKED -> {
        throw new BizException(ResponseCodeEnum.COMMENT_NOT_LIKED);
      }
    }

    // 3. 发送顺序 MQ，删除评论点赞记录

    // 构建消息体
    LikeUnlikeCommentMqDTO likeUnlikeCommentMqDTO =
        LikeUnlikeCommentMqDTO.builder()
            .userId(userId)
            .commentId(commentId)
            .type(LikeUnlikeCommentTypeEnum.UNLIKE.getCode())
            .createTime(LocalDateTime.now())
            .build();

    // 构建消息对象，并将DTO转换成Json字符串设置到消息体中
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeCommentMqDTO)).build();

    // 通过冒号连接，并将DTO转换成Json字符串设置到消息体中
    String destination = MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_UNLIKE;

    // MQ分区键
    String hashKey = String.valueOf(userId);

    // 异步发送MQ顺序消息，提升接口响应速度
    rocketMQTemplate.asyncSendOrderly(
        destination,
        message,
        hashKey,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==> 【评论取消点赞】MQ 发送成功，SendResult: {}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==> 【评论取消点赞】MQ 发送异常：", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 删除评论
   *
   * @param deleteCommentReqVO
   * @return
   */
  @Override
  public Response<?> deleteComment(DeleteCommentReqVO deleteCommentReqVO) {
    Long commentId = deleteCommentReqVO.getCommentId();

    // 1. 校验评论是否存在
    CommentDO commentDO = commentDOMapper.selectByPrimaryKey(commentId);
    if (Objects.isNull(commentDO)) {
      throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
    }

    // 2. 校验是否有权限删除
    Long currUserId = LoginUserContextHolder.getUserId();
    if (!Objects.equals(currUserId, commentDO.getUserId())) {
      throw new BizException(ResponseCodeEnum.COMMENT_CANT_OPERATE);
    }

    // 3. 物理删除评论、评论内容

    // 编程式事务
    transactionTemplate.execute(
        status -> {
          try {
            // 删除评论元数据
            commentDOMapper.deleteByPrimaryKey(commentId);

            // 删除评论内容
            keyValueRpcService.deleteCommentContent(
                commentDO.getNoteId(), commentDO.getCreateTime(), commentDO.getContentUuid());

            return null;
          } catch (Exception ex) {
            status.setRollbackOnly(); // 标记为事务回滚
            log.error("", ex);
            throw ex;
          }
        });

    // 4. 删除 Redis 缓存（ZSet 和 String）
    Integer level = commentDO.getLevel();
    Long noteId = commentDO.getNoteId();
    Long parentCommentId = commentDO.getParentId();

    // 根据评论级别，构建对应的ZSet key
    String redisZSetKey =
        Objects.equals(level, 1)
            ? RedisKeyConstants.buildCommentListKey(noteId)
            : RedisKeyConstants.buildChildCommentListKey(parentCommentId);

    // 使用RedisTe 执行管道操作
    redisTemplate.executePipelined(
        new SessionCallback<>() {
          @Override
          public Object execute(RedisOperations operations) {
            // 删除ZSet中对应评论ID
            operations.opsForZSet().remove(redisZSetKey, commentId);
            // 删除评论详情
            operations.delete(RedisKeyConstants.buildCommentDetailKey(commentId));

            return null;
          }
        });

    // 5. 发布广播 MQ, 将本地缓存删除
    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_DELETE_COMMENT_LOCAL_CACHE,
        commentId,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==> 【删除评论详情本地缓存】MQ 发送成功，SendResult: {}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==> 【删除评论详情本地缓存】MQ 发送异常：", throwable);
          }
        });

    // 6. 发送 MQ, 异步去更新计数、删除关联评论、热度值等

    // 构建消息对象，并将DO转成Json字符串设置到消息体中
    Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(commentDO)).build();
    // 异步发送MQ消息，提升接口响应速度
    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_DELETE_COMMENT,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==> 【删除评论】MQ 发送成功，SendResult: {}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==> 【删除评论】MQ 发送异常", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 删除本地评论缓存
   *
   * @param commentId
   */
  @Override
  public void deleteCommentLocalCache(Long commentId) {
    LOCAL_CACHE.invalidate(commentId);
  }

  /**
   * 初始化评论点赞布隆过滤器
   *
   * @param userId
   * @param expireSeconds
   * @param bloomUserCommentLikeListKey
   * @return
   */
  private void batchAddCommentLike2BloomAndExpire(
      Long userId, long expireSeconds, String bloomUserCommentLikeListKey) {
    try {
      // 查询该用户点赞的所有评论
      List<CommentLikeDO> commentLikeDOS = commentLikeDOMapper.selectByUserId(userId);

      // 若不为空，批量添加到布隆过滤器
      if (CollUtil.isNotEmpty(commentLikeDOS)) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/bloom_batch_add_comment_like_and_expire.lua")));
        script.setResultType(Long.class);

        // 构建Lua参数
        List<Object> luaArgs = Lists.newArrayList();
        commentLikeDOS.forEach(
            commentLikeDO -> luaArgs.add(commentLikeDO.getCommentId())); // 将每个点赞Id传入
        luaArgs.add(expireSeconds);
        redisTemplate.execute(
            script, Collections.singletonList(bloomUserCommentLikeListKey), luaArgs.toArray());
      }
    } catch (Exception e) {
      log.error("## 异步初始化【评论点赞】布隆过滤器异常: ", e);
    }
  }

  /**
   * 校验被点赞的评论是否存在
   *
   * @param commentId
   */
  private void checkCommentIsExist(Long commentId) {
    // 先从本地缓存中查询
    String localCacheJson = LOCAL_CACHE.getIfPresent(commentId);

    // 若本地缓存中不存在，该评论不存在
    if (StringUtils.isBlank(localCacheJson)) {
      // 再从redis中查询
      String commentDetailRedisKey = RedisKeyConstants.buildCommentDetailKey(commentId);

      boolean hasKey = redisTemplate.hasKey(commentDetailRedisKey);

      // 若redis中也不存在
      if (!hasKey) {
        // 从数据库校验
        CommentDO commentDO = commentDOMapper.selectByPrimaryKey(commentId);

        if (Objects.isNull(commentDO)) {
          throw new BizException(ResponseCodeEnum.COMMENT_NOT_FOUND);
        }
      }
    }
  }

  /**
   * 设置子评论VO的计数
   *
   * @param commentRspVOS
   * @param expiredCommentIds
   */
  private void setChildCommentCountData(
      List<FindChildCommentItemRspVO> commentRspVOS, List<Long> expiredCommentIds) {
    // 准备从评论的Hash中查询计数（被点赞数）
    // 缓存中存在的子评论ID
    List<Long> notExpiredCommentIds = Lists.newArrayList();

    // 遍历从缓存中解析出来的VO集合，提取二级评论ID
    commentRspVOS.forEach(
        commentRspVO -> {
          Long childCommentId = commentRspVO.getCommentId();
          notExpiredCommentIds.add(childCommentId);
        });

    // 从Redis中查询评论计数Hash数据
    Map<Long, Map<Object, Object>> commentIdAndCountMap =
        getCommentCountDataAndSync2RedisHash(notExpiredCommentIds);

    // 遍历vo，设置对应评论计数Hash数据
    for (FindChildCommentItemRspVO commentRspVO : commentRspVOS) {
      // 评论ID
      Long commentId = commentRspVO.getCommentId();

      // 若当前这条评论是从数据库中查询出来的，则无需设置点赞数，以数据库查询出来的为主
      if (CollUtil.isNotEmpty(expiredCommentIds) && expiredCommentIds.contains(commentId)) {
        continue;
      }

      // 设置子评论的点赞数
      Map<Object, Object> hash = commentIdAndCountMap.get(commentId);
      if (CollUtil.isNotEmpty(hash)) {
        Long likeTotal = Long.valueOf(hash.get(RedisKeyConstants.FIELD_LIKE_TOTAL).toString());
        commentRspVO.setLikeTotal(likeTotal);
      }
    }
  }

  /**
   * @param notExpiredCommentIds
   * @return
   */
  private Map<Long, Map<Object, Object>> getCommentCountDataAndSync2RedisHash(
      List<Long> notExpiredCommentIds) {
    // 已经失效的Hash评论ID
    List<Long> expiredCountCommentIds = Lists.newArrayList();
    // 构建需要查询的Hash key集合
    List<String> commentCountKeys =
        notExpiredCommentIds.stream().map(RedisKeyConstants::buildCountCommentKey).toList();

    // 使用RedisTemplate执行管道批量操作
    List<Object> results =
        redisTemplate.executePipelined(
            new SessionCallback<>() {

              @Override
              public Object execute(RedisOperations operations) {
                // 遍历需要查询的评论计数hash集合
                commentCountKeys.forEach(
                    key ->
                        // 在此管道中执行Redis的hash.entries操作
                        // 此操作会获取指定hash键中所有的字段和值
                        operations.opsForHash().entries(key));

                return null;
              }
            });

    // 评论ID-计数数据字典
    Map<Long, Map<Object, Object>> commentIdAndCountMap = Maps.newHashMap();
    // 遍历未过期的评论ID集合
    for (int i = 0; i < notExpiredCommentIds.size(); i++) {
      // 当前评论ID
      Long currCommentId = Long.valueOf(notExpiredCommentIds.get(i).toString());
      // 从缓存结果中查询，获取对应Hash
      Map<Object, Object> hash = (Map<Object, Object>) results.get(i);
      // 若Hash结果为空，说明缓存中不存在，添加到exoiredCountCommentIds中，保存一下
      if (CollUtil.isEmpty(hash)) {
        expiredCountCommentIds.add(currCommentId);
        continue;
      }
      // 若存在，则将数据添加到commentIdAndCountMap中，方便后续读取
      commentIdAndCountMap.put(currCommentId, hash);
    }

    // 若已经过期的计数评论ID集合大于0，说明部分计数数据不在Redis缓存中
    // 需要查询数据库，并将这部分的评论计数Hash同步到Redis中
    if (CollUtil.size(expiredCountCommentIds) > 0) {
      // 查询数据库
      List<CommentDO> commentDOS =
          commentDOMapper.selectCommentCountByIds((expiredCountCommentIds));

      commentDOS.forEach(
          commentDO -> {
            Integer level = commentDO.getLevel();
            Map<Object, Object> map = Maps.newHashMap();
            map.put(RedisKeyConstants.FIELD_LIKE_TOTAL, commentDO.getLikeTotal());
            // 只有一级评论需要统计子评论总数
            if (Objects.equals(level, CommentLevelEnum.ONE.getCode())) {
              map.put(
                  RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, commentDO.getChildCommentTotal());
            }
            // 统一添加到commentIdAndCountMap字典中，方便后续查询
            commentIdAndCountMap.put(commentDO.getId(), map);
          });

      // 异步同步到Redis中
      threadPoolTaskExecutor.execute(
          () -> {
            redisTemplate.executePipelined(
                new SessionCallback<>() {
                  @Override
                  public Object execute(RedisOperations operations) {
                    commentDOS.forEach(
                        commentDO -> {
                          // 构建Hash key
                          String key = RedisKeyConstants.buildCountCommentKey(commentDO.getId());
                          Integer level = commentDO.getLevel();
                          // 设置Field数据
                          Map<String, Long> fieldsMap =
                              Objects.equals(level, CommentLevelEnum.ONE.getCode())
                                  ? Map.of(
                                      RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL,
                                      commentDO.getChildCommentTotal(),
                                      RedisKeyConstants.FIELD_LIKE_TOTAL,
                                      commentDO.getLikeTotal())
                                  : Map.of(
                                      RedisKeyConstants.FIELD_LIKE_TOTAL, commentDO.getLikeTotal());
                          // 添加Hash数据
                          operations.opsForHash().putAll(key, fieldsMap);

                          // 设置过期时间（5小时内）
                          long expireTime = 60 * 60 + RandomUtil.randomInt(4 * 60 * 60);
                          operations.expire(key, expireTime, TimeUnit.SECONDS);
                        });
                    return null;
                  }
                });
          });
    }
    return commentIdAndCountMap;
  }

  /**
   * 获取子评论，并同步到Redis中
   *
   * @param childCommentDOS
   * @param childCommentRspVOS
   */
  private void getChildCommentDataAndSync2Redis(
      List<CommentDO> childCommentDOS, List<FindChildCommentItemRspVO> childCommentRspVOS) {
    // 调用kv服务需要的入参
    List<FindCommentContentReqDTO> findCommentContentReqDTOS = Lists.newArrayList();
    // 调用用户服务的入参
    Set<Long> userIds = Sets.newHashSet();

    // 归属的笔记ID
    Long noteId = null;

    // 循环提取RPC调用需要的入参数据
    for (CommentDO childCommentDO : childCommentDOS) {
      noteId = childCommentDO.getNoteId();
      // 构建调用KV服务批量查询评论内容的入参
      boolean isContentEmpty = childCommentDO.getIsContentEmpty();
      if (!isContentEmpty) {
        FindCommentContentReqDTO findCommentContentReqDTO =
            FindCommentContentReqDTO.builder()
                .contentId(childCommentDO.getContentUuid())
                .yearMonth(DateConstants.DATE_FORMAT_Y_M.format(childCommentDO.getCreateTime()))
                .build();
        findCommentContentReqDTOS.add(findCommentContentReqDTO);
      }

      // 构建调用用户服务批量查询用户信息入参（包含评论发布者，回复的目标用户）
      userIds.add(childCommentDO.getUserId());

      Long parentId = childCommentDO.getParentId();
      Long replyCommentId = childCommentDO.getReplyCommentId();
      // 若当前评论的replyCommentId不等于parentId，则前端需要展示回复的哪个用户，如 “回复 小张：”
      if (!Objects.equals(parentId, replyCommentId)) {
        userIds.add(childCommentDO.getReplyUserId());
      }
    }

    // RPC：调用KV服务，评论获取评论内容
    List<FindCommentContentRspDTO> findCommentContentRspDTOS =
        keyValueRpcService.batchFindCommentContent(noteId, findCommentContentReqDTOS);
    // DTO集合转MAP
    Map<String, String> commentUuidAndContentMap = null;
    if (CollUtil.isNotEmpty(findCommentContentRspDTOS)) {
      commentUuidAndContentMap =
          findCommentContentRspDTOS.stream()
              .collect(
                  Collectors.toMap(
                      FindCommentContentRspDTO::getContentId,
                      FindCommentContentRspDTO::getContent));
    }

    // RPC：调用用户服务，批量获取用户信息
    List<FindUserByIdRspDTO> findUserByIdRspDTOS =
        userRpcService.findByIds(userIds.stream().toList());
    // DTO集合转MAp
    Map<Long, FindUserByIdRspDTO> userIdAndDTOMap = null;
    if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
      userIdAndDTOMap =
          findUserByIdRspDTOS.stream()
              .collect(Collectors.toMap(FindUserByIdRspDTO::getId, dto -> dto));
    }

    // DO转VO
    for (CommentDO childCommentDO : childCommentDOS) {
      // 构建VO实体类
      Long userId = childCommentDO.getUserId();
      FindChildCommentItemRspVO childCommentRspVO =
          FindChildCommentItemRspVO.builder()
              .userId(userId)
              .commentId(childCommentDO.getId())
              .imageUrl(childCommentDO.getImageUrl())
              .crateTime(DateUtils.formatRelativeTime(childCommentDO.getCreateTime()))
              .likeTotal(childCommentDO.getLikeTotal())
              .build();

      // 填充用户信息（包括评论发布者、回复的用户）
      if (CollUtil.isNotEmpty(userIdAndDTOMap)) {
        FindUserByIdRspDTO findUserByIdRspDTO = userIdAndDTOMap.get(userId);
        // 评论发布者用户信息
        if (Objects.nonNull(findUserByIdRspDTO)) {
          childCommentRspVO.setAvatar(findUserByIdRspDTO.getAvatar());
          childCommentRspVO.setNickname(findUserByIdRspDTO.getNickName());
        }

        // 评论回复的哪个
        Long replyCommentId = childCommentDO.getReplyCommentId();
        Long parentId = childCommentDO.getParentId();

        if (Objects.nonNull(replyCommentId) && !Objects.equals(replyCommentId, parentId)) {
          Long replyUserId = childCommentDO.getReplyUserId();
          FindUserByIdRspDTO replyUser = userIdAndDTOMap.get(replyUserId);
          childCommentRspVO.setReplyUserName(replyUser.getNickName());
          childCommentRspVO.setReplyUserId(replyUser.getId());
        }
      }

      // 评论内容
      if (CollUtil.isNotEmpty(commentUuidAndContentMap)) {
        String contentUuid = childCommentDO.getContentUuid();
        if (StringUtils.isNotBlank(contentUuid)) {
          childCommentRspVO.setContent(commentUuidAndContentMap.get(contentUuid));
        }
      }

      childCommentRspVOS.add(childCommentRspVO);
    }

    // 异步将笔记详情，同步到redis
    threadPoolTaskExecutor.execute(
        () -> {
          // 准备写入的数据
          Map<String, String> data = Maps.newHashMap();
          childCommentRspVOS.forEach(
              commentRspVO -> {
                // 评论ID
                Long commentId = commentRspVO.getCommentId();
                // 构建Key
                String key = RedisKeyConstants.buildCommentDetailKey(commentId);
                data.put(key, JsonUtils.toJsonString(commentRspVO));
              });
          batchAddCommentDetailJson2Redis(data);
        });
  }

  /**
   * 批量添加评论详情json到Redis中
   *
   * @param data
   */
  private void batchAddCommentDetailJson2Redis(Map<String, String> data) {
    // 使用Redis pipeline提升性能
    redisTemplate.executePipelined(
        (RedisCallback<?>)
            (connection) -> {
              for (Map.Entry<String, String> entry : data.entrySet()) {
                // 将Java对象序列化为Json字符串
                String jsonStr = JsonUtils.toJsonString(entry.getValue());

                // 随机生成过期时间
                int randomExpire = 60 * 60 + RandomUtil.randomInt(4 * 60 * 60);

                // 批量写入并设置过期时间
                connection.setEx(
                    redisTemplate.getStringSerializer().serialize(entry.getKey()),
                    randomExpire,
                    redisTemplate.getStringSerializer().serialize(jsonStr));
              }
              return null;
            });
  }

  /**
   * 同步子评论到Redis中
   *
   * @param parentCommentId
   * @param childCommentZSetKey
   */
  private void syncChildComment2Redis(Long parentCommentId, String childCommentZSetKey) {
    List<CommentDO> childCommentDOS =
        commentDOMapper.selectChildCommentsByParentIdAndLimit(parentCommentId, 6 * 10);

    if (CollUtil.isNotEmpty(childCommentDOS)) {
      // 使用redis pipeline写入
      redisTemplate.executePipelined(
          (RedisCallback<Object>)
              connection -> {
                ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();

                // 遍历子评论数据并批量写入ZSET
                for (CommentDO childCommentDO : childCommentDOS) {
                  Long commentId = childCommentDO.getId();
                  // create_time转时间戳
                  long commentTimestamp =
                      DateUtils.localDateTime2Timestamp(childCommentDO.getCreateTime());
                  zSetOps.add(childCommentZSetKey, commentId, commentTimestamp);
                }

                // 设置随机过期时间，（保底一小时+随机时间），单位 秒
                int randomExpiryTime = 60 * 60 + RandomUtil.randomInt(4 * 60 * 60); // 5小时内
                redisTemplate.expire(childCommentZSetKey, randomExpiryTime, TimeUnit.SECONDS);
                return null; // 无返回值
              });
    }
  }

  /**
   * 同步评论计数到Redis中
   *
   * @param countCommentKey
   * @param dbCount
   */
  private void syncCommentCount2Redis(String countCommentKey, Long dbCount) {
    redisTemplate.executePipelined(
        new SessionCallback<>() {
          @Override
          public Object execute(RedisOperations operations) {
            // 同步hash数据
            operations
                .opsForHash()
                .put(countCommentKey, RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL, dbCount);

            // 随机过期时间 (保底1小时 + 随机时间)，单位：秒
            long expireTime = 60 * 60 + RandomUtil.randomInt(4 * 60 * 60);
            operations.expire(countCommentKey, expireTime, TimeUnit.SECONDS);
            return null;
          }
        });
  }

  /**
   * 同步评论详情到本地缓存中
   *
   * @param commentRspVOS
   */
  private void syncCommentDetail2LocalCache(List<FindCommentItemRspVO> commentRspVOS) {
    // 开启一个异步线程
    threadPoolTaskExecutor.execute(
        () -> {
          // 构建缓存所需要的键值
          Map<Long, String> localCacheData = Maps.newHashMap();
          commentRspVOS.forEach(
              commentRspVO -> {
                Long commentId = commentRspVO.getCommentId();
                localCacheData.put(commentId, JsonUtils.toJsonString(commentRspVO));
              });

          // 批量写入本地缓存
          LOCAL_CACHE.putAll(localCacheData);
        });
  }

  /**
   * 获取全部评论数据，并将评论详情同步到 Redis 中
   *
   * @param oneLevelCommentDOS
   * @param noteId
   * @param commentRspVOS
   */
  private void getCommentDataAndSync2Redis(
      List<CommentDO> oneLevelCommentDOS, Long noteId, List<FindCommentItemRspVO> commentRspVOS) {
    // 过滤出所有最早回复的二级评论ID
    List<Long> twoLevelCommentIds =
        oneLevelCommentDOS.stream()
            .map(CommentDO::getFirstReplyCommentId)
            .filter(firstReplyCommentId -> firstReplyCommentId != 0)
            .toList();

    // 查询二级评论
    Map<Long, CommentDO> commentIdAndDOMap = null;
    List<CommentDO> twoLevelCommonDOS = null;
    if (CollUtil.isNotEmpty(twoLevelCommentIds)) {
      twoLevelCommonDOS = commentDOMapper.selectTwoLevelCommentByIds(twoLevelCommentIds);

      // 转Map集合，方便后续拼装数据
      commentIdAndDOMap =
          twoLevelCommonDOS.stream()
              .collect(Collectors.toMap(CommentDO::getId, commentDO -> commentDO));
    }

    // 调用KV服务需要的入参
    List<FindCommentContentReqDTO> findCommentContentReqDTOS = Lists.newArrayList();
    // 调用用户服务的入参
    List<Long> userIds = Lists.newArrayList();

    // 将一级评论和二级评论合并到一起
    List<CommentDO> allCommentDOS = Lists.newArrayList();
    CollUtil.addAll(allCommentDOS, oneLevelCommentDOS);
    CollUtil.addAll(allCommentDOS, twoLevelCommonDOS);

    // 循环提取RPC调用需要的入参数据
    allCommentDOS.forEach(
        commentDO -> {
          // 构建调用kv服务批量查询评论内容的入参
          boolean isContentEmpty = commentDO.getIsContentEmpty();
          if (!isContentEmpty) {
            FindCommentContentReqDTO findCommentContentReqDTO =
                FindCommentContentReqDTO.builder()
                    .contentId(commentDO.getContentUuid())
                    .yearMonth(DateConstants.DATE_FORMAT_Y_M.format(commentDO.getCreateTime()))
                    .build();

            findCommentContentReqDTOS.add(findCommentContentReqDTO);
          }

          // 构建调用用户服务批量查询用户信息的入参
          userIds.add(commentDO.getUserId());
        });

    // RPC：调用KV服务，批量获取评论内容
    List<FindCommentContentRspDTO> findCommentContentRspDTOS =
        keyValueRpcService.batchFindCommentContent(noteId, findCommentContentReqDTOS);

    // DTO集合转MAP ，方便后续拼装数据
    Map<String, String> commentUuidAndContentMap = null;
    if (CollUtil.isNotEmpty(findCommentContentRspDTOS)) {
      commentUuidAndContentMap =
          findCommentContentRspDTOS.stream()
              .collect(
                  Collectors.toMap(
                      FindCommentContentRspDTO::getContentId,
                      FindCommentContentRspDTO::getContent));
    }

    // RPC：调用用户服务，批量获取用户信息（头像，昵称等）
    List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);

    // DTO集合转Map ，方便后续拼装数据
    Map<Long, FindUserByIdRspDTO> userIdAndDTOMap = null;
    if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
      userIdAndDTOMap =
          findUserByIdRspDTOS.stream()
              .collect(
                  Collectors.toMap(
                      FindUserByIdRspDTO::getId, findUserByIdRspDTO -> findUserByIdRspDTO));
    }

    // DO转VO，组合拼装一二级评论数据
    for (CommentDO commentDO : oneLevelCommentDOS) {
      // 一级评论
      Long userId = commentDO.getUserId();
      FindCommentItemRspVO oneLevelCommentRspVO =
          FindCommentItemRspVO.builder()
              .userId(userId)
              .commentId(commentDO.getId())
              .imageUrl(commentDO.getImageUrl())
              .createTime(DateUtils.formatRelativeTime(commentDO.getCreateTime()))
              .likeTotal(commentDO.getLikeTotal())
              .childCommentTotal(commentDO.getChildCommentTotal())
              .heat(commentDO.getHeat())
              .build();

      // 用户信息 static 对象引用
      setUserInfo(commentIdAndDOMap, userIdAndDTOMap, userId, oneLevelCommentRspVO);
      // 笔记内容 static 对象引用
      setCommentContent(commentUuidAndContentMap, commentDO, oneLevelCommentRspVO);

      // 二级内容
      Long firstReplyCommentId = commentDO.getFirstReplyCommentId();
      if (CollUtil.isNotEmpty(commentIdAndDOMap)) {
        CommentDO firstReplyCommentDO =
            commentIdAndDOMap.get(firstReplyCommentId); // 以一级评论中的首个回复ID查询二级评论
        if (Objects.nonNull(firstReplyCommentDO)) {
          Long firstReplyCommentUserId = firstReplyCommentDO.getUserId();
          FindCommentItemRspVO firstReplyCommentRspVO =
              FindCommentItemRspVO.builder()
                  .userId(firstReplyCommentDO.getUserId())
                  .commentId(firstReplyCommentDO.getId())
                  .imageUrl(firstReplyCommentDO.getImageUrl())
                  .createTime(DateUtils.formatRelativeTime(firstReplyCommentDO.getCreateTime()))
                  .likeTotal(firstReplyCommentDO.getLikeTotal())
                  .heat(firstReplyCommentDO.getHeat())
                  .build();

          // 用户信息 static 对象引用
          setUserInfo(
              commentIdAndDOMap, userIdAndDTOMap, firstReplyCommentUserId, firstReplyCommentRspVO);

          oneLevelCommentRspVO.setFirstReplyComment(firstReplyCommentRspVO);
          // 笔记内容 static 对象引用
          setCommentContent(commentUuidAndContentMap, firstReplyCommentDO, firstReplyCommentRspVO);
        }
      }
      commentRspVOS.add(oneLevelCommentRspVO);
    }

    // 异步将笔记详情，同步到Redis中
    threadPoolTaskExecutor.execute(
        () -> {
          // 准备批量写入数据
          Map<String, String> data = Maps.newHashMap();
          commentRspVOS.forEach(
              commentRspVO -> {
                // 评论ID
                Long commentId = commentRspVO.getCommentId();
                // 构建Key
                String key = RedisKeyConstants.buildCommentDetailKey(commentId);
                data.put(key, JsonUtils.toJsonString(commentRspVO));
              });

          // 使用Redis PipLine提升写入性能
          batchAddCommentDetailJson2Redis(data);
        });
  }

  /**
   * 同步热点评论到redis
   *
   * @param key
   * @param noteId
   */
  private void syncHeatComments2Redis(String key, Long noteId) {
    List<CommentDO> commentDOS = commentDOMapper.selectHeatComments(noteId);
    if (CollUtil.isNotEmpty(commentDOS)) {
      // 使用 Redis Pipeline 提升写入性能
      redisTemplate.executePipelined(
          (RedisCallback<Object>)
              connection -> {
                ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();

                // 遍历评论数据并批量写入 ZSet
                for (CommentDO commentDO : commentDOS) {
                  Long commentId = commentDO.getId();
                  Double commentHeat = commentDO.getHeat();
                  zSetOps.add(key, commentId, commentHeat);
                }

                // 设置随机过期时间，单位：秒
                int randomExpiryTime = RandomUtil.randomInt(5 * 60 * 60); // 5小时以内
                redisTemplate.expire(key, randomExpiryTime, TimeUnit.SECONDS);
                return null; // 无返回值
              });
    }
  }

  /**
   * 同步评评论总数到redis中
   *
   * @param noteCommentTotalKey
   * @param dbCount
   */
  private void syncNoteCommentTotal2Redis(String noteCommentTotalKey, Long dbCount) {
    redisTemplate.executePipelined(
        new SessionCallback<>() {

          @Override
          public Object execute(RedisOperations operations) {
            // 同步Hash数据
            operations
                .opsForHash()
                .put(noteCommentTotalKey, RedisKeyConstants.FIELD_COMMENT_TOTAL, dbCount);
            // 设置过期时间
            long expireTime = 60 * 60 + RandomUtil.randomInt(4 * 60 * 60);
            operations.expire(noteCommentTotalKey, expireTime, TimeUnit.SECONDS);

            return null;
          }
        });
  }

  /**
   * 设置评论内容
   *
   * @param commentUuidAndContentMap
   * @param commentDO1
   * @param firstReplyCommentRspVO
   */
  private static void setCommentContent(
      Map<String, String> commentUuidAndContentMap,
      CommentDO commentDO1,
      FindCommentItemRspVO firstReplyCommentRspVO) {
    if (CollUtil.isNotEmpty(commentUuidAndContentMap)) {
      String contentUuid = commentDO1.getContentUuid();
      if (StringUtils.isNotBlank(contentUuid)) {
        firstReplyCommentRspVO.setContent(commentUuidAndContentMap.get(contentUuid));
      }
    }
  }

  /**
   * 用户信息
   *
   * @param commentIdAndDOMap
   * @param userIdAndDTOMap
   * @param userId
   * @param oneLevelCommentRspVO
   */
  private void setUserInfo(
      Map<Long, CommentDO> commentIdAndDOMap,
      Map<Long, FindUserByIdRspDTO> userIdAndDTOMap,
      Long userId,
      FindCommentItemRspVO oneLevelCommentRspVO) {
    FindUserByIdRspDTO findUserByIdRspDTO = userIdAndDTOMap.get(userId);
    if (Objects.nonNull(findUserByIdRspDTO)) {
      oneLevelCommentRspVO.setAvatar(findUserByIdRspDTO.getAvatar());
      oneLevelCommentRspVO.setNickname(findUserByIdRspDTO.getNickName());
    }
  }

  private void setCommentCountData(
      List<FindCommentItemRspVO> commentRspVOS, List<Long> expiredCommentIds) {
    // 准备从评论Hash中查询计数（子评论总数，被点赞数）
    // 缓存中存在的评论ID
    List<Long> notExpiredCommentIds = expiredCommentIds;

    // 遍历从缓存中解析的VO集合，提取一级、二级评论ID
    commentRspVOS.forEach(
        commentRspVO -> {
          Long oneLevelCommentId = commentRspVO.getCommentId();
          notExpiredCommentIds.add(oneLevelCommentId);
          FindCommentItemRspVO firstCommentVO = commentRspVO.getFirstReplyComment();
          if (Objects.nonNull(firstCommentVO)) {
            notExpiredCommentIds.add(firstCommentVO.getCommentId());
          }
        });

    // 已经失效的Hash评论ID
    Map<Long, Map<Object, Object>> commentIdAndCountMap =
        getCommentCountDataAndSync2RedisHash(notExpiredCommentIds);

    // 遍历VO，设置对应评论的二级评论数，点赞数
    for (FindCommentItemRspVO commentRspVO : commentRspVOS) {
      // 评论ID
      Long commentId = commentRspVO.getCommentId();

      // 若当前这条评论是从数据库查询出来的，则无需设置二级评论数、点赞数，以数据库查询出来的为主
      if (CollUtil.isNotEmpty(expiredCommentIds) && expiredCommentIds.contains(commentId)) {
        continue;
      }

      // 设置一级评论子评论总数，点赞数
      Map<Object, Object> hash = commentIdAndCountMap.get(commentId);
      if (CollUtil.isNotEmpty(hash)) {
        Object likeTotalObj = hash.get(RedisKeyConstants.FIELD_CHILD_COMMENT_TOTAL);
        Long childCommentTotal =
            Objects.isNull(likeTotalObj) ? 0 : Long.parseLong(likeTotalObj.toString());
        Long likeTotal = Long.valueOf(hash.get(RedisKeyConstants.FIELD_LIKE_TOTAL).toString());
        commentRspVO.setChildCommentTotal(childCommentTotal);
        commentRspVO.setLikeTotal(likeTotal);
        // 最初回复的二级评论
        FindCommentItemRspVO firstCommentVO = commentRspVO.getFirstReplyComment();
        if (Objects.nonNull(firstCommentVO)) {
          Long firstCommentId = firstCommentVO.getCommentId();
          Map<Object, Object> firstCommentHash = commentIdAndCountMap.get(firstCommentId);
          if (CollUtil.isNotEmpty(firstCommentHash)) {
            Long firstCommentLikeTotal =
                Long.valueOf(firstCommentHash.get(RedisKeyConstants.FIELD_LIKE_TOTAL).toString());
            firstCommentVO.setLikeTotal(firstCommentLikeTotal);
          }
        }
      }
    }
  }
}
