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
import com.slilio.xiaohashu.comment.biz.domain.mapper.CommentDOMapper;
import com.slilio.xiaohashu.comment.biz.domain.mapper.NoteCountDOMapper;
import com.slilio.xiaohashu.comment.biz.enums.ResponseCodeEnum;
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
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

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
          if (Objects.nonNull(commentJson)) {
            // 缓存中存在的评论Json，直接转换为VO添加到返参集合中
            FindCommentItemRspVO commentRspVO =
                JsonUtils.parseObject(commentJson, FindCommentItemRspVO.class);
            commentRspVOS.add(commentRspVO);
          } else {
            // 评论失效，添加到失效评论列表
            expiredCommentIds.add(Long.valueOf(commentIdList.get(i).toString()));
          }
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

    // todo：从缓存查询

    // 查询一级评论下子评论的总数（直接查询t_comment表的child_comment_total字段，提升查询性能，避免count(*)）
    Long count = commentDOMapper.selectChildCommentTotalById(parentCommentId);

    // 若一级评论不存在，或者子评论总数为0
    if (Objects.isNull(count) || count == 0) {
      return PageResponse.success(null, pageNo, 0);
    }

    // 遍历这个父级评论ID下的子评论数据，封装返回    遍历childCommentDOS，因为内部包含了子评论的信息

    // 分页返参
    List<FindChildCommentItemRspVO> childCommentRspVOS = Lists.newArrayList();

    // 计算分页查询的偏移量offset（需要+1，因为最早回复的二级评论已经被展示了）
    long offset = PageResponse.getOffset(pageNo, pageSize) + 1;

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
          redisTemplate.executePipelined(
              (RedisCallback<?>)
                  (connection) -> {
                    for (Map.Entry<String, String> entry : data.entrySet()) {
                      // 将Java对象序列化为Json字符串
                      String jsonStr = JsonUtils.toJsonString(entry.getValue());

                      // 随机生成过期时间（5小时内）
                      int randomExpire = RandomUtil.randomInt(5 * 60 * 60);

                      // 批量写入并设置过期时间
                      connection.setEx(
                          redisTemplate.getStringSerializer().serialize(entry.getKey()),
                          randomExpire,
                          redisTemplate.getStringSerializer().serialize(jsonStr));
                    }
                    return null;
                  });
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
}
