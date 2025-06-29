package com.slilio.xiaohashu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.DateUtils;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.framework.common.util.NumberUtils;
import com.slilio.xiaohashu.count.dto.FindNoteCountsByIdRspDTO;
import com.slilio.xiaohashu.note.biz.constant.MQConstants;
import com.slilio.xiaohashu.note.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteCollectionDO;
import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteDO;
import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteLikeDO;
import com.slilio.xiaohashu.note.biz.domain.mapper.NoteCollectionDOMapper;
import com.slilio.xiaohashu.note.biz.domain.mapper.NoteDOMapper;
import com.slilio.xiaohashu.note.biz.domain.mapper.NoteLikeDOMapper;
import com.slilio.xiaohashu.note.biz.domain.mapper.TopicDOMapper;
import com.slilio.xiaohashu.note.biz.enums.*;
import com.slilio.xiaohashu.note.biz.model.dto.CollectUnCollectNoteMqDTO;
import com.slilio.xiaohashu.note.biz.model.dto.LikeUnlikeNoteMqDTO;
import com.slilio.xiaohashu.note.biz.model.dto.NoteOperateMqDTO;
import com.slilio.xiaohashu.note.biz.model.vo.*;
import com.slilio.xiaohashu.note.biz.rpc.CountRpcService;
import com.slilio.xiaohashu.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.slilio.xiaohashu.note.biz.rpc.KeyValueRpcService;
import com.slilio.xiaohashu.note.biz.rpc.UserRpcService;
import com.slilio.xiaohashu.note.biz.service.NoteService;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
  @Resource private NoteDOMapper noteDOMapper;
  @Resource private TopicDOMapper topicDOMapper;
  @Resource private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
  @Resource private KeyValueRpcService keyValueRpcService;
  @Resource private UserRpcService userRpcService;
  @Resource private RedisTemplate<String, String> redisTemplate;
  @Resource private ResourceLoader resourceLoader;
  @Resource private RocketMQTemplate rocketMQTemplate;
  @Resource private NoteLikeDOMapper noteLikeDOMapper;
  @Resource private NoteCollectionDOMapper noteCollectionDOMapper;
  @Resource private CountRpcService countRpcService;

  @Resource(name = "taskExecutor")
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  /** 笔记详情本地缓存 */
  private static final Cache<Long, String> LOCAL_CACHE =
      Caffeine.newBuilder()
          .initialCapacity(10000) // 设置初始缓存容量为10000个条目
          .maximumSize(10000) // 设置缓存的最大容量为10000个条目
          .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存的条目在写入后一个小时过期
          .build();

  /**
   * 笔记发布
   *
   * @param publishNoteReqVO
   * @return
   */
  @Override
  public Response<?> publishNote(PublishNoteReqVO publishNoteReqVO) {
    // 笔记类型
    Integer type = publishNoteReqVO.getType();
    // 获取对应类型的枚举
    NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

    // 若非图文、视频、抛出业务异常
    if (Objects.isNull(noteTypeEnum)) {
      throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
    }

    String imgUris = null;
    String videoUri = null;
    // 笔记内容是否为空，默认值为true，即空
    Boolean isContentEmpty = true;

    switch (noteTypeEnum) {
      case IMAGE_TEXT: // 图文笔记
        List<String> imgUriList = publishNoteReqVO.getImgUris();
        // 校验图片是否为空
        Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
        // 校验图片数量
        Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多与8张");
        // 将图片链接拼接，以逗号分隔
        imgUris = StringUtils.join(imgUriList, ",");

        break;
      case VIDEO:
        videoUri = publishNoteReqVO.getVideoUri();
        // 校验视频连接是否为空
        Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");

        break;
      default:
        break;
    }

    // RPC调用分布式ID生成服务，生成笔记ID
    String snowflakeIdId = distributedIdGeneratorRpcService.getSnowflakeId();
    // 笔记内容UUID
    String contentUuid = null;

    // 笔记内容
    String content = publishNoteReqVO.getContent();

    // 若用户填写了笔记内容
    if (StringUtils.isNotBlank(content)) {
      // 内容是否为空，默认为false，即为不空
      isContentEmpty = false;
      // 生成笔记UUID
      contentUuid = UUID.randomUUID().toString();
      // RPC：调用KV键值服务，存储短文本
      boolean isSavedSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);

      // 若存储失败，抛出业务异常，提示用户发布笔记失败
      if (!isSavedSuccess) {
        throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
      }
    }

    // 话题
    Long topicId = publishNoteReqVO.getTopicId();
    String topicName = null;
    if (Objects.nonNull(topicId)) {
      // 获取话题名称
      topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
    }

    // 发布者以用户ID
    Long creatorId = LoginUserContextHolder.getUserId();

    // 构建笔记OD对象
    NoteDO noteDO =
        NoteDO.builder()
            .id(Long.valueOf(snowflakeIdId))
            .isContentEmpty(isContentEmpty)
            .creatorId(creatorId)
            .imgUris(imgUris)
            .title(publishNoteReqVO.getTitle())
            .topicId(topicId)
            .topicName(topicName)
            .type(type)
            .visible(NoteVisibleEnum.PUBLIC.getCode())
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .status(NoteStatusEnum.NORMAL.getCode())
            .isTop(Boolean.FALSE)
            .videoUri(videoUri)
            .contentUuid(contentUuid)
            .build();

    // 删除个人主页 - 已发布笔记列表缓存
    // TODO: 应采取灵活的策略，如果是大V, 应该直接更新缓存，而不是直接删除；普通用户则可直接删除
    String publishedNoteListRedisKey = RedisKeyConstants.buildPublishedNoteListKey(creatorId);
    redisTemplate.delete(publishedNoteListRedisKey);

    // 尝试笔记入库
    try {
      // 笔记入库
      noteDOMapper.insert(noteDO);
    } catch (Exception e) {
      log.error("===》 笔记存储失败", e);

      // RPC:笔记保存失败，则删除笔记内容
      if (StringUtils.isNotBlank(contentUuid)) {
        keyValueRpcService.deleteNoteContent(contentUuid);
      }
    }

    // 延迟双删：发送延迟消息
    sendDelayDeleteRedisPublishedNoteListCacheMQ(creatorId);

    // 发送MQ
    // 构建消息体DTO
    NoteOperateMqDTO noteOperateMqDTO =
        NoteOperateMqDTO.builder()
            .creatorId(creatorId)
            .noteId(Long.valueOf(snowflakeIdId))
            .type(NoteOperateEnum.PUBLISH.getCode())
            .build();
    // 构建消息对象，并将DTO转成json字符串设置到消息体中
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(noteOperateMqDTO)).build();

    String destination = MQConstants.TOPIC_NOTE_OPERATE + ":" + MQConstants.TAG_NOTE_PUBLISH;
    // 发送MQ
    rocketMQTemplate.asyncSend(
        destination,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("===》【笔记发布】 MQ发送成功，SendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("===》【笔记发布】 MQ发送异常：", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 延迟双删：发送延迟消息
   *
   * @param userId
   */
  private void sendDelayDeleteRedisPublishedNoteListCacheMQ(Long userId) {
    Message<String> message = MessageBuilder.withPayload(String.valueOf(userId)).build();

    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_DELAY_DELETE_PUBLISHED_NOTE_LIST_REDIS_CACHE,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("## 延时删除 Redis 已发布笔记列表缓存消息发送成功：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.error("## 延时删除 Redis 已发布笔记列表缓存消息发送失败...", throwable);
          }
        },
        3000, // 超时时间
        1 // 延迟级别，1表示延时1s
        );
  }

  /**
   * 笔记详情
   *
   * @param findNoteDetailReqVO
   * @return
   */
  @Override
  @SneakyThrows
  public Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO) {
    // 查询的笔记ID
    Long noteId = findNoteDetailReqVO.getId();

    // 当前登录用户
    Long userId = LoginUserContextHolder.getUserId();

    // 先从本地缓存中查询
    String findNoteDetailRspVOStrLocalCache = LOCAL_CACHE.getIfPresent(noteId);
    if (StringUtils.isNotBlank(findNoteDetailRspVOStrLocalCache)) {
      FindNoteDetailRspVO findNoteDetailRspVO =
          JsonUtils.parseObject(findNoteDetailRspVOStrLocalCache, FindNoteDetailRspVO.class);
      log.info("===》 命中了本地缓存；{}", findNoteDetailRspVOStrLocalCache);
      // 可见性校验
      checkNoteVisibleFromVO(userId, findNoteDetailRspVO);
      return Response.success(findNoteDetailRspVO);
    }

    // 从redis缓存中读取
    String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
    String noteDetailJson = redisTemplate.opsForValue().get(noteDetailRedisKey);

    // 若缓存中有该笔记，则直接返回
    if (StringUtils.isNotBlank(noteDetailJson)) {
      FindNoteDetailRspVO findNoteDetailRspVO =
          JsonUtils.parseObject(noteDetailJson, FindNoteDetailRspVO.class);
      // 异步线程将用户信息存入本地缓存
      threadPoolTaskExecutor.submit(
          () -> {
            // 写入本地缓存
            LOCAL_CACHE.put(
                noteId,
                Objects.isNull(findNoteDetailRspVO)
                    ? "null"
                    : JsonUtils.toJsonString(findNoteDetailRspVO));
          });

      // 可见性校验
      if (Objects.nonNull(findNoteDetailRspVO)) {
        Integer visible = findNoteDetailRspVO.getVisible();
        checkNoteVisible(visible, userId, findNoteDetailRspVO.getCreatorId());
      }
      return Response.success(findNoteDetailRspVO);
    }

    // 若Redis获取不到，则走数据库查询
    // 查询笔记
    NoteDO noteDO = noteDOMapper.selectByPrimaryKey(noteId);

    // 若该笔记不存在则抛出业务异常
    if (Objects.isNull(noteDO)) {
      threadPoolTaskExecutor.execute(
          () -> {
            // 防止缓存穿透，将空数据存入redis缓存（过期时间不宜过长）
            // 保底1分钟+随机秒数
            long expireSeconds = 60 + RandomUtil.randomInt(60);
            redisTemplate
                .opsForValue()
                .set(noteDetailRedisKey, "null", expireSeconds, TimeUnit.SECONDS);
          });
      throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
    }

    // 可见性校验
    Integer visible = noteDO.getVisible();
    checkNoteVisible(visible, userId, noteDO.getCreatorId());

    // 并发查询优化
    // RPC：调用用户服务
    Long creatorId = noteDO.getCreatorId();
    CompletableFuture<FindUserByIdRspDTO> userResultFuture =
        CompletableFuture.supplyAsync(
            () -> userRpcService.findById(creatorId), threadPoolTaskExecutor);

    // RPC：调用K-V存储服务获取内容
    CompletableFuture<String> contentResultFuture = CompletableFuture.completedFuture(null);
    if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
      contentResultFuture =
          CompletableFuture.supplyAsync(
              () -> keyValueRpcService.findNoteContent(noteDO.getContentUuid()),
              threadPoolTaskExecutor);
    }
    CompletableFuture<String> finalContentResultFuture = contentResultFuture;

    CompletableFuture<FindNoteDetailRspVO> resultFuture =
        CompletableFuture.allOf(
                userResultFuture, contentResultFuture) // 再创建CompletableFuture用于等待userResultFuture,
            // contentResultFuture全部完成
            .thenApply(
                s -> {
                  // 获取Future返回的结果
                  FindUserByIdRspDTO findUserByIdRspDTO = userResultFuture.join();
                  String content = finalContentResultFuture.join();

                  // 笔记类型
                  Integer noteType = noteDO.getType();
                  // 图文笔记链接（字符串）
                  String imgUrisStr = noteDO.getImgUris();
                  // 图文笔记链接集合
                  List<String> imgUris = null;
                  // 如果查询的是图文笔记，需要将图片链接的逗号分隔开，转换为集合
                  if (Objects.equals(noteType, NoteTypeEnum.IMAGE_TEXT.getCode())
                      && StringUtils.isNotBlank(imgUrisStr)) {
                    imgUris = List.of(imgUrisStr.split(","));
                  }

                  // 构建返参VO实体类
                  return FindNoteDetailRspVO.builder()
                      .id(noteDO.getId())
                      .type(noteDO.getType())
                      .title(noteDO.getTitle())
                      .content(content)
                      .imgUris(imgUris)
                      .topicId(noteDO.getTopicId())
                      .topicName(noteDO.getTopicName())
                      .creatorId(noteDO.getCreatorId())
                      .creatorName(findUserByIdRspDTO.getNickName())
                      .avatar(findUserByIdRspDTO.getAvatar())
                      .videoUri(noteDO.getVideoUri())
                      .updateTime(noteDO.getUpdateTime())
                      .visible(noteDO.getVisible())
                      .build();
                });

    // 获取拼装后的FindNoteDetailRspVO
    FindNoteDetailRspVO findNoteDetailRspVO = resultFuture.get();

    // 异步将笔记详情存入redis
    threadPoolTaskExecutor.submit(
        () -> {
          String noteDetailJson1 = JsonUtils.toJsonString(findNoteDetailRspVO);
          // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
          long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
          redisTemplate
              .opsForValue()
              .set(noteDetailRedisKey, noteDetailJson1, expireSeconds, TimeUnit.SECONDS);
        });

    return Response.success(findNoteDetailRspVO);
  }

  /**
   * 笔记更新
   *
   * @param updateNoteReqVO
   * @return
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO) {
    // 笔记ID
    Long noteId = updateNoteReqVO.getId();
    // 笔记类型
    Integer type = updateNoteReqVO.getType();

    // 获取对应类型的枚举
    NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

    // 若非图文、视频、抛出业务异常
    if (Objects.isNull(noteTypeEnum)) {
      throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
    }

    String imgUris = null;
    String videoUri = null;
    switch (noteTypeEnum) {
      case IMAGE_TEXT: // 图文笔记
        List<String> imgUrisList = updateNoteReqVO.getImgUris();
        // 校验图片是否为空
        Preconditions.checkArgument(CollUtil.isNotEmpty(imgUrisList), "笔记图片不能为空");
        // 校验图片数量
        Preconditions.checkArgument(imgUrisList.size() <= 8, "笔记图片不能多于8张");

        imgUris = StringUtils.join(imgUrisList, ",");
        break;
      case VIDEO:
        videoUri = updateNoteReqVO.getVideoUri();
        // 校验视频链接是否为空
        Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
        break;
      default:
        break;
    }

    // 当前登录用户ID
    Long currUserId = LoginUserContextHolder.getUserId();
    NoteDO selectNoteDO = noteDOMapper.selectByPrimaryKey(noteId);

    // 笔记不存在
    if (Objects.isNull(selectNoteDO)) {
      throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
    }

    // 判断权限：非笔记发布者不允许更新笔记
    if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
      throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
    }

    // 话题
    Long topicId = updateNoteReqVO.getTopicId();
    String topicName = null;
    if (Objects.nonNull(topicId)) {
      topicName = topicDOMapper.selectNameByPrimaryKey(topicId);

      // 判断一下提交的话题，是否真实存在
      if (StringUtils.isBlank(topicName)) {
        throw new BizException(ResponseCodeEnum.TOPIC_NOT_FOUND);
      }
    }

    // 删除redis缓存
    String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
    String publishedNoteListRedisKey = RedisKeyConstants.buildPublishedNoteListKey(currUserId);
    redisTemplate.delete(Arrays.asList(noteDetailRedisKey, publishedNoteListRedisKey));

    // 更新笔记元数据表 t_note
    String content = updateNoteReqVO.getContent();
    NoteDO noteDO =
        NoteDO.builder()
            .id(noteId)
            .isContentEmpty(StringUtils.isBlank(content))
            .imgUris(imgUris)
            .title(updateNoteReqVO.getTitle())
            .topicId(topicId)
            .topicName(topicName)
            .type(type)
            .updateTime(LocalDateTime.now())
            .videoUri(videoUri)
            .build();

    noteDOMapper.updateByPrimaryKey(noteDO);

    // 删除redis缓存
    // 异步发送延迟信息
    sendDelayDeleteRedisNoteCacheMQ(Arrays.asList(noteId, currUserId));

    // 删除本地缓存
    // LOCAL_CACHE.invalidate(noteId);
    // 同步发送广播模式MQ，将所有实例中本地缓存都删除
    rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
    log.info("====》 MQ：删除笔记本地缓存发送成功。。。");

    // 笔记内容更新
    // 查询笔记的对应uuid
    NoteDO noteDO1 = noteDOMapper.selectByPrimaryKey(noteId);
    String contentUuid = noteDO1.getContentUuid();

    // 笔记是否内容更新
    boolean isUpdateContentSuccess = false;
    if (StringUtils.isBlank(content)) {
      // 若笔记内容为空，则删除K-V存储
      isUpdateContentSuccess = keyValueRpcService.deleteNoteContent(contentUuid);
    } else {
      // 若将无内容的笔记更新成了有内容的笔记，需要重新生成uuid
      contentUuid = StringUtils.isBlank(contentUuid) ? UUID.randomUUID().toString() : contentUuid;
      // 调用k-v更新短文本
      isUpdateContentSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);
    }

    // 如果更新失败，抛出业务异常，回滚事务
    if (!isUpdateContentSuccess) {
      throw new BizException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
    }

    return Response.success();
  }

  /**
   * 异步发送延时消息
   *
   * @param noteIdAndUserId
   */
  private void sendDelayDeleteRedisNoteCacheMQ(List<Long> noteIdAndUserId) {
    Message<String> message = MessageBuilder.withPayload(String.valueOf(noteIdAndUserId)).build();
    rocketMQTemplate.asyncSend(
        MQConstants.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("## 延时删除 Redis 笔记缓存消息发送成功...");
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("## 延时删除 Redis 笔记缓存消息发送失败...");
          }
        },
        3000, // 超时时间（毫秒）
        1 // 延迟级别，1表示延时1s
        );
  }

  /**
   * 删除本地笔记缓存
   *
   * @param noteId
   */
  @Override
  public void deleteNoteLocalCache(Long noteId) {
    LOCAL_CACHE.invalidate(noteId);
  }

  /**
   * 删除笔记
   *
   * @param deleteNoteReqVO
   * @return
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO) {
    // 笔记ID
    Long noteId = deleteNoteReqVO.getId();

    NoteDO selectNoteDO = noteDOMapper.selectByPrimaryKey(noteId);
    // 判断笔记是否存在
    if (Objects.isNull(selectNoteDO)) {
      throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
    }
    // 判断权限：非笔记发布者不允许删除笔记
    Long currUserId = LoginUserContextHolder.getUserId();
    if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
      throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
    }

    // 删除缓存
    String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
    String publishedNoteListRedisKey = RedisKeyConstants.buildPublishedNoteListKey(currUserId);
    redisTemplate.delete(Arrays.asList(noteDetailRedisKey, publishedNoteListRedisKey));

    // 逻辑删除
    NoteDO noteDO =
        NoteDO.builder()
            .id(noteId)
            .status(NoteStatusEnum.DELETED.getCode())
            .updateTime(LocalDateTime.now())
            .build();

    noteDOMapper.updateByPrimaryKeySelective(noteDO);

    // 延迟双删
    sendDelayDeleteRedisPublishedNoteListCacheMQ(currUserId);

    // 同步发送广播模式MQ，将所有实例的本地缓存删除掉
    rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
    log.info("====》 MQ：删除笔记本地缓存发送成功。。。");

    // 发送MQ
    // 构建消息体
    NoteOperateMqDTO noteOperateMqDTO =
        NoteOperateMqDTO.builder()
            .creatorId(selectNoteDO.getCreatorId())
            .noteId(noteId)
            .type(NoteOperateEnum.DELETE.getCode()) // 删除笔记
            .build();

    // 构建消息对象，DTO转json设置到消息体里
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(noteOperateMqDTO)).build();

    // 冒号连接，topic携带tag
    String destination = MQConstants.TOPIC_NOTE_OPERATE + ":" + MQConstants.TAG_NOTE_DELETE;

    // 发送MQ
    rocketMQTemplate.asyncSend(
        destination,
        message,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("===》【笔记删除】 MQ发送成功，SendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("===》【笔记删除】 MQ发送异常：", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 笔记仅自己可见
   *
   * @param updateNoteVisibleOnlyMeReqVO
   * @return
   */
  @Override
  public Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
    // 笔记ID
    Long noteId = updateNoteVisibleOnlyMeReqVO.getId();

    NoteDO selectNoteDO = noteDOMapper.selectByPrimaryKey(noteId);
    // 判断笔记是否存在
    if (Objects.isNull(selectNoteDO)) {
      throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
    }
    // 判断权限：非笔记发布者无权修改
    Long currUserId = LoginUserContextHolder.getUserId();
    if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
      throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
    }

    // 构建新的DO实体类
    NoteDO noteDO =
        NoteDO.builder()
            .id(noteId)
            .visible(NoteVisibleEnum.PRIVATE.getCode())
            .updateTime(LocalDateTime.now())
            .build();
    // 执行SQL
    int count = noteDOMapper.updateVisibleOnlyMe(noteDO);

    // 若影响行数为0，则表示该笔记无法修改为仅自己可见
    if (count == 0) {
      throw new BizException(ResponseCodeEnum.NOTE_CANT_VISIBLE_ONLY_ME);
    }

    // 删除redis缓存
    String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
    redisTemplate.delete(noteDetailRedisKey);

    // 同步发送广播模式MQ，将所有实例的本地缓存都删除掉
    rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
    log.info("====》 MQ：删除笔记本地缓存发送成功。。。");

    return Response.success();
  }

  /**
   * 笔记置顶、取消
   *
   * @param topNoteReqVO
   * @return
   */
  @Override
  public Response<?> topNote(TopNoteReqVO topNoteReqVO) {
    // 笔记ID
    Long noteId = topNoteReqVO.getId();
    // 是否置顶
    Boolean isTop = topNoteReqVO.getIsTop();

    // 当前登录用户
    Long currUserId = LoginUserContextHolder.getUserId();

    // 构建置顶、取消DO实体类
    NoteDO noteDO =
        NoteDO.builder()
            .id(noteId)
            .isTop(isTop)
            .updateTime(LocalDateTime.now())
            .creatorId(currUserId) // 笔记所有者才能操作自己的笔记
            .build();

    int count = noteDOMapper.updateIsTop(noteDO);

    if (count == 0) {
      throw new BizException(ResponseCodeEnum.NOTE_CANT_OPERATE);
    }

    // 删除redis缓存
    String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
    redisTemplate.delete(noteDetailRedisKey);

    // 同步发送广播模式MQ。将所有的实例的本地缓存都删除掉
    rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
    log.info("====》 MQ：删除笔记本地缓存发送成功。。。");

    return Response.success();
  }

  /**
   * 点赞笔记
   *
   * @param likeNoteReqVO
   * @return
   */
  @Override
  public Response<?> likeNote(LikeNoteReqVO likeNoteReqVO) {
    // 笔记ID
    Long noteId = likeNoteReqVO.getId();

    // 1.校验被点赞的笔记是否存在
    Long creatorId = checkNoteIsExistAndGetCreatorId(noteId);

    // 2.判断目标笔记，是否已经点赞过
    // 当前登录用户
    Long userId = LoginUserContextHolder.getUserId();

    // Roaring Bitmap Key
    String rbitmapUserNoteLikeListKey = RedisKeyConstants.buildRBitmapUserNoteLikeListKey(userId);

    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    // Lua脚本路径
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/rbitmap_note_like_check.lua")));
    // 返回值类型
    script.setResultType(Long.class);
    // 执行Lua脚本
    Long result =
        redisTemplate.execute(
            script, Collections.singletonList(rbitmapUserNoteLikeListKey), noteId);

    NoteLikeLuaResultEnum noteLikeLuaResultEnum = NoteLikeLuaResultEnum.valueOf(result);

    // 用户点赞列表 ZSet Key
    String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);
    switch (noteLikeLuaResultEnum) {
      // Redis中布隆过滤器不存在
      case NOT_EXIST -> {
        // 从数据库中校验笔记是否被点赞，并异步初始化布隆过滤器，设置过期时间
        int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);

        // 过期时间 1天+
        long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

        // 目标笔记已经被点赞（替换为RBitmap进行缓存，降低内存开销和误判风险）
        if (count > 0) {
          // 异步初始化布隆过滤器
          threadPoolTaskExecutor.submit(
              () ->
                  batchAddNoteLike2RBitmapAndExpire(
                      userId, expireSeconds, rbitmapUserNoteLikeListKey));
          throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
        }

        // 若目标笔记未被点赞，查询当前用户是否有点赞其他笔记，有则同步初始化 Roaring Bitmap
        batchAddNoteLike2RBitmapAndExpire(userId, expireSeconds, rbitmapUserNoteLikeListKey);

        // 添加当前点赞笔记 ID 到 Roaring Bitmap 中
        // Lua 脚本路径
        script.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/rbitmap_add_note_like_and_expire.lua")));
        // 返回值类型
        script.setResultType(Long.class);
        redisTemplate.execute(
            script, Collections.singletonList(rbitmapUserNoteLikeListKey), noteId, expireSeconds);
      }
      // 目标笔记已经被点赞（替换bloom为roaring bitmap无误判风险）
      case NOTE_LIKED -> {
        //        // 校验ZSet列表中是否包含被点赞的笔记ID
        //        Double score = redisTemplate.opsForZSet().score(userNoteLikeZSetKey, noteId);
        //
        //        if (Objects.nonNull(score)) {
        //          throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
        //        }
        //
        //        // 若Score为空，则表示ZSet点赞列表中不存在，查询数据库校验
        //        int count = noteLikeDOMapper.selectNoteIsLiked(userId, noteId);
        //        if (count > 0) {
        //          // 数据库里面有点赞，而redis中Zset不存在，则需要重新异步初始化Zset
        //          asyncInitUserNoteLikesZSet(userId, userNoteLikeZSetKey);
        //
        //          throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
        //        }

        throw new BizException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
      }
    }

    // 3.更新用户Zset点赞列表
    LocalDateTime now = LocalDateTime.now();
    // Lua脚本路径
    script.setScriptSource(
        new ResourceScriptSource(
            new ClassPathResource("/lua/note_like_check_and_update_zset.lua")));
    // 返回值类型
    script.setResultType(Long.class);
    // 执行Lua脚本
    result =
        redisTemplate.execute(
            script,
            Collections.singletonList(userNoteLikeZSetKey),
            noteId,
            DateUtils.localDateTime2Timestamp(now));

    // 若Zset不存在，则需要重新初始化
    if (Objects.equals(result, NoteLikeLuaResultEnum.NOT_EXIST.getCode())) {
      // 查询当前用户最新点赞的100篇笔记
      List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);

      if (CollUtil.isNotEmpty(noteLikeDOS)) {
        // 过期时间
        long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
        // 构建Lua参数
        Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOS, expireSeconds);

        DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
        script2.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
        // 返回值类型
        script2.setResultType(Long.class);
        // 执行
        redisTemplate.execute(script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs);

        // 再次调用note_like_check_and_update_zset.lua脚本，将点赞数据添加到zset中
        redisTemplate.execute(
            script,
            Collections.singletonList(userNoteLikeZSetKey),
            noteId,
            DateUtils.localDateTime2Timestamp(now));
      }
    }
    // 4.发送MQ，将点赞落数据库
    // 构建消息体DTO
    LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO =
        LikeUnlikeNoteMqDTO.builder()
            .userId(userId)
            .noteId(noteId)
            .type(LikeUnlikeNoteTypeEnum.LIKE.getCode())
            .createTime(now)
            .noteCreatorId(creatorId) // 笔记发布者ID
            .build();

    // 构建消息对象
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeNoteMqDTO)).build();
    // 通过冒号链接，让MQ发送主题topic时携带标签tag
    String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_LIKE;
    String hashKey = String.valueOf(userId);
    // 异步发送MQ消息，提升接口响应速度
    rocketMQTemplate.asyncSendOrderly(
        destination,
        message,
        hashKey,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==》 【笔记点赞】MQ发送成功，SendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==》 【笔记点赞】MQ发送异常：", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 初始化笔记点赞Roaring Bitmap
   *
   * @param userId
   * @param expireSeconds
   * @param rbitmapUserNoteLikeListKey
   * @return
   */
  private void batchAddNoteLike2RBitmapAndExpire(
      Long userId, long expireSeconds, String rbitmapUserNoteLikeListKey) {
    try {
      // 异步全量同步一下，并设置过期时间
      List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectByUserId(userId);

      if (CollUtil.isNotEmpty(noteLikeDOS)) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/rbitmap_batch_add_note_like_and_expire.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 构建 Lua 参数
        List<Object> luaArgs = Lists.newArrayList();
        noteLikeDOS.forEach(noteLikeDO -> luaArgs.add(noteLikeDO.getNoteId())); // 将每个点赞的笔记 ID 传入
        luaArgs.add(expireSeconds); // 最后一个参数是过期时间（秒）
        redisTemplate.execute(
            script, Collections.singletonList(rbitmapUserNoteLikeListKey), luaArgs.toArray());
      }
    } catch (Exception e) {
      log.error("## 异步初始化【笔记点赞】Roaring Bitmap 异常: ", e);
    }
  }

  /**
   * 取消点赞笔记
   *
   * @param unLikeNoteReqVO
   * @return
   */
  @Override
  public Response<?> unLikeNote(UnlikeNoteReqVO unLikeNoteReqVO) {
    // 笔记ID
    Long noteId = unLikeNoteReqVO.getId();

    // 1.校验笔记是否存在
    Long creatorId = checkNoteIsExistAndGetCreatorId(noteId);

    // 2.校验笔记是否被点赞过
    // 当前登录用户ID
    Long userId = LoginUserContextHolder.getUserId();

    // Roaring bitmap（替换掉布隆过滤器）
    //    String bloomUserNoteLikeListKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);
    String rbitmapUserNoteLikeListKey = RedisKeyConstants.buildRBitmapUserNoteLikeListKey(userId);

    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    // 脚本路径
    // script.setScriptSource(
    //    new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_unlike_check.lua")));
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/rbitmap_note_unlike_check.lua")));
    // 返回值类型
    script.setResultType(Long.class);
    // 执行脚本
    Long result =
        redisTemplate.execute(
            script, Collections.singletonList(rbitmapUserNoteLikeListKey), noteId);

    NoteUnlikeLuaResultEnum noteUnlikeLuaResultEnum = NoteUnlikeLuaResultEnum.valueOf(result);

    switch (noteUnlikeLuaResultEnum) {
      // 布隆过滤器不存在
      case NOT_EXISTS -> {
        // 异步初始化布隆过滤器
        threadPoolTaskExecutor.submit(
            () -> {
              // 过期时间 1天+
              long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
              //              batchAddNoteLike2BloomAndExpire(userId, expireSeconds,
              // bloomUserNoteLikeListKey);
              batchAddNoteLike2RBitmapAndExpire(userId, expireSeconds, rbitmapUserNoteLikeListKey);
            });

        // 从数据库校验笔记是否被点赞
        int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(userId, noteId);

        // 未点赞，无法点赞，抛出异常
        if (count == 0) {
          throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
        }
      }
      // Roaring Bitmap 校验目标笔记未被点赞
      case NOTE_NOT_LIKED -> {
        throw new BizException(ResponseCodeEnum.NOTE_NOT_LIKED);
      }
    }

    // 3.校验完毕，然后，删除ZSET中已经点赞的笔记ID
    // 用户点赞列表ZSet Key
    String userNoteLikeZSetKey = RedisKeyConstants.buildUserNoteLikeZSetKey(userId);
    redisTemplate.opsForZSet().remove(userNoteLikeZSetKey, noteId);
    // 4.发送MQ，数据更新落库
    // 构建消息体DTO
    LikeUnlikeNoteMqDTO likeUnlikeNoteMqDTO =
        LikeUnlikeNoteMqDTO.builder()
            .userId(userId)
            .noteId(noteId)
            .type(LikeUnlikeNoteTypeEnum.UNLIKE.getCode()) // 取消点赞
            .createTime(LocalDateTime.now())
            .noteCreatorId(creatorId) // 笔记发布者ID
            .build();

    // 构建消息对象，并将DTO转换为json设置到消息体中
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(likeUnlikeNoteMqDTO)).build();
    // 通过冒号连接，让MQ发送主题topic携带tag
    String destination = MQConstants.TOPIC_LIKE_OR_UNLIKE + ":" + MQConstants.TAG_UNLIKE;
    String hashKey = String.valueOf(userId);
    // 异步发送MQ消息，提升接口响应速度
    rocketMQTemplate.asyncSendOrderly(
        destination,
        message,
        hashKey,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==》 【笔记取消点赞】MQ发送成功，SendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==》 【笔记取消点赞】MQ发送异常：", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 收藏笔记
   *
   * @param collectNoteReqVO
   * @return
   */
  @Override
  public Response<?> collectNote(CollectNoteReqVO collectNoteReqVO) {
    // 笔记ID
    Long noteId = collectNoteReqVO.getId();

    // 1.校验收藏的笔记是否存在
    Long creatorId = checkNoteIsExistAndGetCreatorId(noteId);
    // 2.判断目标笔记，是否已经收藏过
    // 当前登录用户ID
    Long userId = LoginUserContextHolder.getUserId();

    // 布隆过滤器Key
    //    String bloomUserNoteCollectListKey =
    // RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);
    String rbitmapUserNoteCollectListKey =
        RedisKeyConstants.buildRBitmapUserNoteCollectListKey(userId);

    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    // lua脚本路径
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/rbitmap_note_collect_check.lua")));
    script.setResultType(Long.class);

    // 执行Lua脚本
    Long result =
        redisTemplate.execute(
            script, Collections.singletonList(rbitmapUserNoteCollectListKey), noteId);

    NoteCollectLuaResultEnum noteCollectLuaResultEnum = NoteCollectLuaResultEnum.valueOf(result);

    // 用户收藏ZSetKey
    String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);

    switch (noteCollectLuaResultEnum) {
      // redis中布隆过滤器不存在
      case NOT_EXIST -> {
        // 从数据库校验笔记是否被收藏，并异步初始化布隆过滤器，设置过期时间
        int count = noteCollectionDOMapper.selectCountByUserIdAndNoteId(userId, noteId);
        // 过期时间
        long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
        // 目标笔记已经被收藏
        if (count > 0) {
          // 异步初始化布隆过滤器
          threadPoolTaskExecutor.submit(
              () ->
                  batchAddNoteCollect2RBitmapAndExpire(
                      userId, expireSeconds, rbitmapUserNoteCollectListKey));
          throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
        }

        //  若目标笔记未被收藏，查询当前用户是否有收藏其他笔记，有则同步初始化布隆过滤器
        batchAddNoteCollect2RBitmapAndExpire(userId, expireSeconds, rbitmapUserNoteCollectListKey);
        //  添加当前提交的noteId收藏笔记ID到存储到布隆过滤器中
        script.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/rbitmap_add_note_collect_and_expire.lua")));
        script.setResultType(Long.class);
        redisTemplate.execute(
            script,
            Collections.singletonList(rbitmapUserNoteCollectListKey),
            noteId,
            expireSeconds);
      }
      // 目标已经被收藏（Roaring Bitmap不存在误判，不需要进一步判断）
      case NOTE_COLLECTED -> {
        //        // 校验Zset列表中是否包含被收藏的笔记ID
        //        Double score = redisTemplate.opsForZSet().score(userNoteCollectZSetKey, noteId);
        //        if (Objects.nonNull(score)) {
        //          throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
        //        }
        //
        //        // 若score为空，则表示ZSet收藏列表不存在，查询数据库校验
        //        int count = noteCollectionDOMapper.selectNoteIsCollected(userId, noteId);
        //        if (count > 0) {
        //          // 数据库有记录，需要向redis中同步，而redis中zset未初始化，需要重新异步初始化ZSet
        //          asyncInitUserNoteCollectsZSet(userId, userNoteCollectZSetKey);
        //
        //          throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
        //        }
        throw new BizException(ResponseCodeEnum.NOTE_ALREADY_COLLECTED);
      }
    }

    // 3.更新用户zset收藏列表
    LocalDateTime now = LocalDateTime.now();
    // Lua脚本处理
    script.setScriptSource(
        new ResourceScriptSource(
            new ClassPathResource("/lua/note_collect_check_and_update_zset.lua")));
    script.setResultType(Long.class);
    result =
        redisTemplate.execute(
            script,
            Collections.singletonList(userNoteCollectZSetKey),
            noteId,
            DateUtils.localDateTime2Timestamp(now));

    // 若zset不存在，则需要重新初始化
    if (Objects.equals(result, NoteCollectLuaResultEnum.NOT_EXIST.getCode())) {
      // 重新初始化ZSET
      // 查询当前用户最新收藏的300篇笔记
      List<NoteCollectionDO> noteCollectionDOS =
          noteCollectionDOMapper.selectCollectedByUserIdAndLimit(userId, 300);

      // 保底1天+随机
      long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

      DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
      script2.setScriptSource(
          new ResourceScriptSource(
              new ClassPathResource("/lua/batch_add_note_collect_zset_and_expire.lua")));
      script2.setResultType(Long.class);

      // 若数据库存在历史收藏笔记，需要批量同步
      if (CollUtil.isNotEmpty(noteCollectionDOS)) {
        // 构建Lua参数
        Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);
        // 执行Lua脚本
        redisTemplate.execute(script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs);
        // 再次调用note_collect_check_and_update_zset.lua脚本，将当前收藏的笔记添加到zset中
        redisTemplate.execute(
            script,
            Collections.singletonList(userNoteCollectZSetKey),
            noteId,
            DateUtils.localDateTime2Timestamp(now));
      } else {
        // 若无历史收藏的笔记，则直接将当前收藏的笔记ID添加到ZSET中，随机过期时间
        List<Object> luaArgs = Lists.newArrayList();
        luaArgs.add(DateUtils.localDateTime2Timestamp(LocalDateTime.now())); // score：时间戳
        luaArgs.add(noteId);
        luaArgs.add(expireSeconds);

        redisTemplate.execute(
            script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs.toArray());
      }
    }

    // 4.发送MQ，将收藏数据落库
    CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO =
        CollectUnCollectNoteMqDTO.builder()
            .userId(userId)
            .noteId(noteId)
            .type(CollectUnCollectNoteTypeEnum.COLLECT.getCode()) // 收藏笔记
            .createTime(now)
            .noteCreatorId(creatorId)
            .build();

    // 构建消息对象，并将DTO转成Json字符串设置到消息体中
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(collectUnCollectNoteMqDTO)).build();
    // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
    String destination = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT + ":" + MQConstants.TAG_COLLECT;

    String hashKey = String.valueOf(userId);

    // 异步发送顺序MQ消息，提升接口响应速度
    rocketMQTemplate.asyncSendOrderly(
        destination,
        message,
        hashKey,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==》【笔记收藏】MQ发送成功，SendResult：{}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==》【笔记收藏】MQ发送异常：", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 取消收藏笔记
   *
   * @param unCollectNoteReqVO
   * @return
   */
  @Override
  public Response<?> unCollectNote(UnCollectNoteReqVO unCollectNoteReqVO) {
    // 笔记ID
    Long noteId = unCollectNoteReqVO.getId();

    // 1.校验笔记是否真实存在
    Long creatorId = checkNoteIsExistAndGetCreatorId(noteId);
    // 2.校验笔记是否被收藏过
    Long userId = LoginUserContextHolder.getUserId();

    // 布隆过滤器Key
    //    String bloomUserNoteCollectListKey =
    // RedisKeyConstants.buildBloomUserNoteCollectListKey(userId);
    String rbitmapUserNoteCollectListKey =
        RedisKeyConstants.buildRBitmapUserNoteCollectListKey(userId);

    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/rbitmap_note_uncollect_check.lua")));
    script.setResultType(Long.class);

    // 执行Lua脚本
    Long result =
        redisTemplate.execute(
            script, Collections.singletonList(rbitmapUserNoteCollectListKey), noteId);
    NoteUnCollectLuaResultEnum noteUnCollectLuaResultEnum =
        NoteUnCollectLuaResultEnum.valueOf(result);

    switch (noteUnCollectLuaResultEnum) {
      // 布隆过滤器不存在
      case NOT_EXISTS -> {
        // 异步初始化布隆过滤器
        threadPoolTaskExecutor.submit(
            () -> {
              // 过期时间：保底1天+随机秒数
              long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
              batchAddNoteCollect2RBitmapAndExpire(
                  userId, expireSeconds, rbitmapUserNoteCollectListKey);
            });

        // 从数据库中校验笔记是否被收藏
        int count = noteCollectionDOMapper.selectCountByUserIdAndNoteId(userId, noteId);

        // 未收藏，无法取消收藏，抛出异常
        if (count == 0) {
          throw new BizException(ResponseCodeEnum.NOTE_NOT_COLLECTED);
        }
      }
      // 布隆过滤器校验目标未被收藏（判断绝对准确）
      case NOTE_NOT_COLLECTED -> {
        throw new BizException(ResponseCodeEnum.NOTE_NOT_COLLECTED);
      }
    }

    // 3.删除ZSET中已收藏的笔记ID 由上述检测后，到此处，布隆过滤器已有收藏，可以直接删除ZSET中的笔记ID
    // 用户收藏列表ZSet key
    String userNoteCollectZSetKey = RedisKeyConstants.buildUserNoteCollectZSetKey(userId);

    redisTemplate.opsForZSet().remove(userNoteCollectZSetKey, noteId);

    // 4.发送MQ，数据更新落库
    CollectUnCollectNoteMqDTO collectUnCollectNoteMqDTO =
        CollectUnCollectNoteMqDTO.builder()
            .userId(userId)
            .noteId(noteId)
            .type(CollectUnCollectNoteTypeEnum.UN_COLLECT.getCode()) // 取消收藏笔记
            .createTime(LocalDateTime.now())
            .noteCreatorId(creatorId)
            .build();
    // 构建消息对象
    Message<String> message =
        MessageBuilder.withPayload(JsonUtils.toJsonString(collectUnCollectNoteMqDTO)).build();

    // 通过冒号连接, 可让 MQ 发送给主题 Topic 时，携带上标签 Tag
    String destination = MQConstants.TOPIC_COLLECT_OR_UN_COLLECT + ":" + MQConstants.TAG_UN_COLLECT;

    String hashKey = String.valueOf(userId);

    // 异步发送顺序MQ消息，提升接口响应速度
    rocketMQTemplate.asyncSendOrderly(
        destination,
        message,
        hashKey,
        new SendCallback() {
          @Override
          public void onSuccess(SendResult sendResult) {
            log.info("==> 【笔记取消收藏】MQ 发送成功，SendResult: {}", sendResult);
          }

          @Override
          public void onException(Throwable throwable) {
            log.info("==> 【笔记取消收藏】MQ 发送异常：", throwable);
          }
        });

    return Response.success();
  }

  /**
   * 初始化笔记收藏Roaring Bitmap
   *
   * @param userId
   * @param expireSeconds
   * @param rbitmapUserNoteCollectListKey
   */
  private void batchAddNoteCollect2RBitmapAndExpire(
      Long userId, long expireSeconds, String rbitmapUserNoteCollectListKey) {
    try {
      // 异步全量同步一下，并设置过期时间
      List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectByUserId(userId);

      if (CollUtil.isNotEmpty(noteCollectionDOS)) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/rbitmap_batch_add_note_collect_and_expire.lua")));
        script.setResultType(Long.class);

        // 构建Lua参数
        List<Object> luaArgs = Lists.newArrayList();
        noteCollectionDOS.forEach(
            noteCollectionDO -> luaArgs.add(noteCollectionDO.getNoteId())); // 将每个收藏的ID写入
        luaArgs.add(expireSeconds); // 最后写入过期时间
        // 执行
        redisTemplate.execute(
            script, Collections.singletonList(rbitmapUserNoteCollectListKey), luaArgs.toArray());
      }
    } catch (Exception e) {
      log.error("## 异步初始化【笔记收藏】Roaring Bitmap 异常: ", e);
    }
  }

  /**
   * 获取是否点赞，收藏数据
   *
   * @param findNoteIsLikedAndCollectedReqVO
   * @return
   */
  @Override
  public Response<FindNoteIsLikedAndCollectedRspVO> isLikedAndCollectedData(
      FindNoteIsLikedAndCollectedReqVO findNoteIsLikedAndCollectedReqVO) {
    Long noteId = findNoteIsLikedAndCollectedReqVO.getNoteId();
    // 已经登录的用户
    Long currUserId = LoginUserContextHolder.getUserId();
    // 默认未点赞、未收藏
    boolean isLiked = false;
    boolean isCollected = false;

    // 若当前用户已经登录
    if (Objects.nonNull((currUserId))) {
      // 校验是否点赞
      isLiked = checkNoteIsLiked(noteId, currUserId);
      // 校验是否收藏
      isCollected = checkNoteIsCollected(noteId, currUserId);
    }

    return Response.success(
        FindNoteIsLikedAndCollectedRspVO.builder()
            .noteId(noteId)
            .isLiked(isLiked)
            .isCollected(isCollected)
            .build());
  }

  /**
   * 用户主页查询-查询已经发布的笔记列表
   *
   * @param findPublishedNoteListReqVO
   * @return
   */
  @Override
  public Response<FindPublishedNoteListRspVO> findPublishedNoteList(
      FindPublishedNoteListReqVO findPublishedNoteListReqVO) {

    Long userId = findPublishedNoteListReqVO.getUserId();
    // 游标
    Long cursor = findPublishedNoteListReqVO.getCursor();

    // 返参VO
    FindPublishedNoteListRspVO findPublishedNoteListRspVO = null;

    // 优先查询缓存
    String publishedNoteListRedisKey = RedisKeyConstants.buildPublishedNoteListKey(userId);
    // 游标为空则查询第一页
    if (Objects.isNull((cursor))) {
      String publishedNoteListJson = redisTemplate.opsForValue().get(publishedNoteListRedisKey);

      if (StringUtils.isNotBlank(publishedNoteListJson)) {
        try {
          log.info("## 已发布笔记列表命中了redis缓存...");
          // json转vo集合
          List<NoteItemRspVO> noteItemRspVOS =
              JsonUtils.parseList(publishedNoteListJson, NoteItemRspVO.class);
          // 按照笔记id降序，最新发布的笔记排在最前面
          List<NoteItemRspVO> sortList =
              noteItemRspVOS.stream()
                  .sorted(Comparator.comparing(NoteItemRspVO::getNoteId).reversed())
                  .toList();
          // 过滤出最早发布的笔记ID，充当下一页的游标
          Optional<Long> earliestNoteId =
              noteItemRspVOS.stream().map(NoteItemRspVO::getNoteId).min(Long::compareTo);

          // 如果是博主本人，需要调用计数服务，获取最新的点赞数据
          getAndSetLatestLikeTotalIfAuthor(userId, sortList);

          // 批量获取笔记的点赞状态
          batchGetAndSetNoteIsLiked(sortList);

          findPublishedNoteListRspVO =
              FindPublishedNoteListRspVO.builder()
                  .notes(sortList)
                  .nextCursor(earliestNoteId.orElse(null))
                  .build();

          return Response.success(findPublishedNoteListRspVO);

        } catch (Exception e) {
          log.error("", e);
        }
      }
    }

    // 缓存无，查询数据库
    List<NoteDO> noteDOS = noteDOMapper.selectPublishedNoteListByUserIdAndCursor(userId, cursor);

    if (CollUtil.isNotEmpty(noteDOS)) {
      // do转vo
      List<NoteItemRspVO> noteVOS =
          noteDOS.stream()
              .map(
                  noteDO -> {
                    // 封面
                    String cover =
                        StringUtils.isNotBlank(noteDO.getImgUris())
                            ? StringUtils.split(noteDO.getImgUris(), ",")[0]
                            : null;

                    NoteItemRspVO noteItemRspVO =
                        NoteItemRspVO.builder()
                            .noteId(noteDO.getId())
                            .type(noteDO.getType())
                            .creatorId(noteDO.getCreatorId())
                            .cover(cover)
                            .videoUri(noteDO.getVideoUri())
                            .title(noteDO.getTitle())
                            .isLiked(false) // 默认为未点赞
                            .build();
                    return noteItemRspVO;
                  })
              .toList();

      // Feign调用用户服务，获取用户头像和昵称
      CompletableFuture<FindUserByIdRspDTO> userFuture =
          CompletableFuture.supplyAsync(
              () -> {
                Optional<Long> creatorIdOptional =
                    noteDOS.stream().map(NoteDO::getCreatorId).findAny();
                return userRpcService.findById(creatorIdOptional.get());
              },
              threadPoolTaskExecutor);

      // Feign调用计数服务，批量获取笔记点赞数
      CompletableFuture<List<FindNoteCountsByIdRspDTO>> noteCountFuture =
          CompletableFuture.supplyAsync(
              () -> {
                List<Long> noteIds = noteDOS.stream().map(NoteDO::getId).toList();
                return countRpcService.findByNotesIds(noteIds);
              },
              threadPoolTaskExecutor);

      // 等待所有任务完成，并合并结果
      CompletableFuture.allOf(userFuture, noteCountFuture).join();

      try {
        // 获取Future返回结果
        FindUserByIdRspDTO findUserByIdRspDTO = userFuture.get();
        List<FindNoteCountsByIdRspDTO> findNoteCountsByIdRspDTOS = noteCountFuture.get();

        if (Objects.nonNull(findUserByIdRspDTO)) {
          // 循环 VO 集合，分别设置头像、昵称
          noteVOS.forEach(
              noteItemRspVO -> {
                noteItemRspVO.setAvatar(findUserByIdRspDTO.getAvatar());
                noteItemRspVO.setNickname(findUserByIdRspDTO.getNickName());
              });
        }

        // 设置笔记点赞数
        setVOListLikeTotal(noteVOS, findNoteCountsByIdRspDTOS);

        // 批量获取笔记的点赞状态
        batchGetAndSetNoteIsLiked(noteVOS);

      } catch (Exception e) {
        log.error("## 并发调用错误: ", e);
      }

      // 过滤出最早发布的笔记ID，充当下一页的游标
      Optional<Long> earliestNoteId = noteDOS.stream().map(NoteDO::getId).min(Long::compareTo);

      findPublishedNoteListRspVO =
          FindPublishedNoteListRspVO.builder()
              .notes(noteVOS)
              .nextCursor(earliestNoteId.orElse(null))
              .build();

      // 同步第一页已发布笔记到redis中
      if (Objects.isNull(cursor)) {
        syncFirstPagePublishedNoteList2Redis(noteVOS, publishedNoteListRedisKey);
      }
    }
    return Response.success(findPublishedNoteListRspVO);
  }

  /**
   * 批量获取笔记的点赞状态
   *
   * @param noteItemRspVOS
   */
  private void batchGetAndSetNoteIsLiked(List<NoteItemRspVO> noteItemRspVOS) {
    // 当前登录的用户ID
    Long loginUserId = LoginUserContextHolder.getUserId();
    // 若用户已经登录
    if (Objects.nonNull(loginUserId)) {
      // 提取所有需要获取点赞状态的笔记id
      List<Long> noteIds = noteItemRspVOS.stream().map(NoteItemRspVO::getNoteId).toList();
      // 构建roaring bitmap key
      String rbitmapUserNoteLikeListKey =
          RedisKeyConstants.buildRBitmapUserNoteLikeListKey(loginUserId);

      DefaultRedisScript<List> script = new DefaultRedisScript<>();
      script.setScriptSource(
          new ResourceScriptSource(new ClassPathResource("/lua/rbitmap_batch_get_note_liked.lua")));
      // 返回值类型
      script.setResultType(List.class);
      // 执行
      List<Long> results =
          redisTemplate.execute(
              script, Collections.singletonList(rbitmapUserNoteLikeListKey), noteIds.toArray());

      // 若redis中缓存不存在，下标0存放的标识为-1
      Long hasKey = results.get(0);
      // 若Roaring bitmap不存在
      if (Objects.equals(hasKey, NoteLikeLuaResultEnum.NOT_EXIST.getCode())) {
        // 从数据库查询
        List<NoteLikeDO> noteLikeDOS =
            noteLikeDOMapper.selectByUserIdAndNoteIds(loginUserId, noteIds);
        if (CollUtil.isEmpty(noteLikeDOS)) {
          return;
        }

        // do转map，方便查询
        Map<Long, NoteLikeDO> noteIdIsLikedMap =
            noteLikeDOS.stream()
                .collect(Collectors.toMap(NoteLikeDO::getNoteId, noteLikeDO -> noteLikeDO));
        // 循环vo集合，设置是否点赞
        noteItemRspVOS.forEach(
            noteItemRspVO -> {
              Long currNoteId = noteItemRspVO.getNoteId();
              NoteLikeDO noteLikeDO = noteIdIsLikedMap.get(currNoteId);
              if (Objects.nonNull(noteLikeDO)) {
                noteItemRspVO.setIsLiked(true);
              }
            });

        // 再异步初始化roaring bitmap
        threadPoolTaskExecutor.submit(
            () -> {
              long expireSeconds = 60 * 30 + RandomUtil.randomInt(60 * 60);
              batchAddNoteLike2RBitmapAndExpire(
                  loginUserId, expireSeconds, rbitmapUserNoteLikeListKey);
            });

        return;
      }

      // 否则，则 Roaring Bitmap 存在

      // 初始化一个字典，解析 Lua 结果，并设置每篇笔记是否被点赞（map:1号文章,已经点赞）
      Map<Long, Boolean> likedMap = Maps.newHashMapWithExpectedSize(noteIds.size());
      for (int i = 0; i < noteIds.size(); i++) {
        Long currNoteId = noteIds.get(i);
        Boolean isLiked = Objects.equals(results.get(i), 1L);
        likedMap.put(currNoteId, isLiked);
      }
      // 循环VO集合，设置是否点赞
      noteItemRspVOS.forEach(
          noteItemRspVO -> {
            Long currNoteId = noteItemRspVO.getNoteId();
            noteItemRspVO.setIsLiked(likedMap.get(currNoteId));
          });
    }
  }

  /**
   * 如果是博主本人，需要调用计数服务，获取最新的点赞数据
   *
   * @param userId
   * @param sortList
   */
  private void getAndSetLatestLikeTotalIfAuthor(Long userId, List<NoteItemRspVO> sortList) {
    Long loginUserId = LoginUserContextHolder.getUserId();
    // 用户已登录，并且查询是自己
    if (Objects.nonNull(loginUserId) && Objects.equals(loginUserId, userId)) {
      List<Long> noteIds = sortList.stream().map(NoteItemRspVO::getNoteId).toList();
      List<FindNoteCountsByIdRspDTO> findNoteCountsByIdRspDTOS =
          countRpcService.findByNotesIds(noteIds);

      // 设置笔记的点赞数
      setVOListLikeTotal(sortList, findNoteCountsByIdRspDTOS);
    }
  }

  /**
   * 设置 VO 集合中每篇笔记的点赞量
   *
   * @param noteItemRspVOS
   * @param findNoteCountsByIdRspDTOS
   */
  private static void setVOListLikeTotal(
      List<NoteItemRspVO> noteItemRspVOS,
      List<FindNoteCountsByIdRspDTO> findNoteCountsByIdRspDTOS) {

    if (CollUtil.isNotEmpty(findNoteCountsByIdRspDTOS)) {
      // DTO 集合转 Map
      Map<Long, FindNoteCountsByIdRspDTO> noteIdAndDTOMap =
          findNoteCountsByIdRspDTOS.stream()
              .collect(Collectors.toMap(FindNoteCountsByIdRspDTO::getNoteId, dto -> dto));

      // 循环设置 VO 集合，设置每篇笔记的点赞量
      noteItemRspVOS.forEach(
          noteItemRspVO -> {
            Long currNoteId = noteItemRspVO.getNoteId();
            FindNoteCountsByIdRspDTO findNoteCountsByIdRspDTO = noteIdAndDTOMap.get(currNoteId);
            noteItemRspVO.setLikeTotal(
                (Objects.nonNull(findNoteCountsByIdRspDTO)
                        && Objects.nonNull(findNoteCountsByIdRspDTO.getLikeTotal()))
                    ? NumberUtils.formatNumberString(findNoteCountsByIdRspDTO.getLikeTotal())
                    : "0");
          });
    }
  }

  /**
   * // 同步第一页已发布笔记到redis中
   *
   * @param noteVOS
   * @param publishedNoteListRedisKey
   */
  private void syncFirstPagePublishedNoteList2Redis(
      List<NoteItemRspVO> noteVOS, String publishedNoteListRedisKey) {
    if (CollUtil.isEmpty(noteVOS)) {
      return;
    }
    threadPoolTaskExecutor.submit(
        () -> {
          long expireSeconds = 60 * 30 + RandomUtil.randomInt(60 * 30);
          redisTemplate
              .opsForValue()
              .set(
                  publishedNoteListRedisKey,
                  JsonUtils.toJsonString(noteVOS),
                  expireSeconds,
                  TimeUnit.SECONDS);
        });
  }

  /**
   * 校验是否收藏
   *
   * @param noteId
   * @param currUserId
   * @return
   */
  private boolean checkNoteIsCollected(Long noteId, Long currUserId) {
    // 是否收藏
    boolean isCollected = false;
    // Roaring bitmap key
    String rbitmapUserNoteCollectListKey =
        RedisKeyConstants.buildRBitmapUserNoteCollectListKey(currUserId);

    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptSource(
        new ResourceScriptSource(
            new ClassPathResource("/lua/rbitmap_note_collect_only_check.lua")));
    script.setResultType(Long.class);

    // 执行Lua
    Long result =
        redisTemplate.execute(
            script, Collections.singletonList(rbitmapUserNoteCollectListKey), noteId);

    NoteCollectLuaResultEnum noteCollectLuaResultEnum = NoteCollectLuaResultEnum.valueOf(result);

    switch (noteCollectLuaResultEnum) {
      // redis中roaring bitmap不存在
      case NOT_EXIST -> {
        // 从数据库中校验笔记是否被收藏，并异步初始化布隆过滤器，设置过期时间
        int count = noteCollectionDOMapper.selectCountByUserIdAndNoteId(currUserId, noteId);

        // 过期时间
        long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

        // 目标笔记已经被收藏
        if (count > 0) {
          // 异步初始化rbitmap
          threadPoolTaskExecutor.submit(
              () ->
                  batchAddNoteCollect2RBitmapAndExpire(
                      currUserId, expireSeconds, rbitmapUserNoteCollectListKey));
          isCollected = true;
        }
      }
      // 目标笔记已经被收藏
      case NOTE_COLLECTED -> isCollected = true;
    }
    return isCollected;
  }

  /**
   * 校验是否点赞
   *
   * @param noteId
   * @param currUserId
   * @return
   */
  private boolean checkNoteIsLiked(Long noteId, Long currUserId) {
    // 是否点赞
    boolean isLiked = false;

    // Roaring bitmap key
    String rbitmapUserNoteLikeListKey =
        RedisKeyConstants.buildRBitmapUserNoteLikeListKey(currUserId);

    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("/lua/rbitmap_note_like_only_check.lua")));
    script.setResultType(Long.class);

    Long result =
        redisTemplate.execute(
            script, Collections.singletonList(rbitmapUserNoteLikeListKey), noteId);

    NoteLikeLuaResultEnum noteLikeLuaResultEnum = NoteLikeLuaResultEnum.valueOf(result);

    switch (noteLikeLuaResultEnum) {
      // redis中roaring bitmap
      case NOT_EXIST -> {
        // 从数据库中校验笔记是否被点赞，并异步初始化 Roaring Bitmap，设置过期时间
        int count = noteLikeDOMapper.selectCountByUserIdAndNoteId(currUserId, noteId);
        // 过期时间
        long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
        // 目标笔记已经被点赞
        if (count > 0) {
          // 异步初始化roaring bitmap
          threadPoolTaskExecutor.submit(
              () ->
                  batchAddNoteLike2RBitmapAndExpire(
                      currUserId, expireSeconds, rbitmapUserNoteLikeListKey));
          isLiked = true;
        }
      }
      case NOTE_LIKED -> isLiked = true; // Roaring Bitmap 判断已点赞
    }
    return isLiked;
  }

  /**
   * 异步初始化用户收藏笔记
   *
   * @param userId
   * @param userNoteCollectZSetKey
   */
  private void asyncInitUserNoteCollectsZSet(Long userId, String userNoteCollectZSetKey) {
    threadPoolTaskExecutor.execute(
        () -> {
          // 判断用户笔记收藏ZSet是否存在
          boolean hasKey = redisTemplate.hasKey(userNoteCollectZSetKey);

          // 不存在，则重新初始化
          if (!hasKey) {
            // 查询当前用户收藏的前300篇笔记
            List<NoteCollectionDO> noteCollectionDOS =
                noteCollectionDOMapper.selectCollectedByUserIdAndLimit(userId, 300);
            if (CollUtil.isNotEmpty(noteCollectionDOS)) {
              // 保底1天＋随机秒数
              long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
              // 构建Lua参数
              Object[] luaArgs = buildNoteCollectZSetLuaArgs(noteCollectionDOS, expireSeconds);

              DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
              script2.setScriptSource(
                  new ResourceScriptSource(
                      new ClassPathResource("/lua/batch_add_note_collect_zset_and_expire.lua")));
              script2.setResultType(Long.class);

              redisTemplate.execute(
                  script2, Collections.singletonList(userNoteCollectZSetKey), luaArgs);
            }
          }
        });
  }

  /**
   * 构建笔记收藏ZSET lua脚本参数
   *
   * @param noteCollectionDOS
   * @param expireSeconds
   * @return
   */
  private static Object[] buildNoteCollectZSetLuaArgs(
      List<NoteCollectionDO> noteCollectionDOS, long expireSeconds) {
    int argsLength = noteCollectionDOS.size() * 2 + 1; // 每个笔记收藏关系有2个参数（score和value）外加一个过期时间
    Object[] luaArgs = new Object[argsLength];

    int i = 0;
    for (NoteCollectionDO noteCollectionDO : noteCollectionDOS) {
      luaArgs[i] =
          DateUtils.localDateTime2Timestamp(noteCollectionDO.getCreateTime()); // 收藏时间作为score
      luaArgs[i + 1] = noteCollectionDO.getNoteId(); // 笔记ID
      i += 2;
    }
    luaArgs[argsLength - 1] = expireSeconds; // 最后一个为过期时间
    return luaArgs;
  }

  /**
   * 初始化布隆笔记收藏过滤器
   *
   * @param userId
   * @param expireSeconds
   * @param bloomUserNoteCollectListKey
   */
  private void batchAddNoteCollect2BloomAndExpire(
      Long userId, long expireSeconds, String bloomUserNoteCollectListKey) {
    try {
      // 异步全量同步一下，并设置过期时间
      List<NoteCollectionDO> noteCollectionDOS = noteCollectionDOMapper.selectByUserId(userId);

      if (CollUtil.isNotEmpty(noteCollectionDOS)) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/bloom_batch_add_note_collect_and_expire.lua")));
        script.setResultType(Long.class);

        // 构建Lua参数
        List<Object> luaArgs = Lists.newArrayList();
        noteCollectionDOS.forEach(
            noteCollectionDO -> luaArgs.add(noteCollectionDO.getNoteId())); // 将每个收藏的ID写入
        luaArgs.add(expireSeconds); // 最后写入过期时间
        // 执行
        redisTemplate.execute(
            script, Collections.singletonList(bloomUserNoteCollectListKey), luaArgs.toArray());
      }
    } catch (Exception e) {
      log.error("## 异步初始化【笔记收藏】布隆过滤器异常: ", e);
    }
  }

  /**
   * 异步初始化布隆过滤器
   *
   * @param userId
   * @param expireSeconds
   * @param bloomUserNoteLikeListKey
   */
  private void batchAddNoteLike2BloomAndExpire(
      Long userId, long expireSeconds, String bloomUserNoteLikeListKey) {
    try {
      // 异步全量同步一下，并设置过期时间
      List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectByUserId(userId);

      if (CollUtil.isNotEmpty(noteLikeDOS)) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(
            new ResourceScriptSource(
                new ClassPathResource("/lua/bloom_batch_add_note_like_and_expire.lua")));
        script.setResultType(Long.class);

        // 构建Lua参数
        List<Object> luaArgs = Lists.newArrayList();
        noteLikeDOS.forEach(noteLikeDO -> luaArgs.add(noteLikeDO.getNoteId())); // 将每个点赞的笔记ID传入
        luaArgs.add(expireSeconds); // 最后一个为过期时间
        redisTemplate.execute(
            script, Collections.singletonList(bloomUserNoteLikeListKey), luaArgs.toArray());
      }
    } catch (Exception e) {
      log.error("## 异步初始化布隆过滤器异常： ", e);
    }
  }

  /**
   * 异步初始化用户点赞笔记
   *
   * @param userId
   * @param userNoteLikeZSetKey
   */
  private void asyncInitUserNoteLikesZSet(Long userId, String userNoteLikeZSetKey) {
    threadPoolTaskExecutor.execute(
        () -> {
          // 判断点赞的笔记ZSET是否存在
          boolean hasKey = redisTemplate.hasKey(userNoteLikeZSetKey);

          // 不存在，则重新初始化
          if (!hasKey) {
            // 查询当前用户最新点赞的100篇笔记
            List<NoteLikeDO> noteLikeDOS =
                noteLikeDOMapper.selectLikedByUserIdAndLimit(userId, 100);
            if (CollUtil.isNotEmpty(noteLikeDOS)) {
              // 过期时间
              long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
              // 构建Lua参数
              Object[] luaArgs = buildNoteLikeZSetLuaArgs(noteLikeDOS, expireSeconds);

              DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
              script2.setScriptSource(
                  new ResourceScriptSource(
                      new ClassPathResource("/lua/batch_add_note_like_zset_and_expire.lua")));
              script2.setResultType(Long.class);
              // 执行脚本
              redisTemplate.execute(
                  script2, Collections.singletonList(userNoteLikeZSetKey), luaArgs);
            }
          }
        });
  }

  /**
   * 构建Lua脚本参数
   *
   * @param noteLikeDOS
   * @param expireSeconds
   * @return
   */
  private Object[] buildNoteLikeZSetLuaArgs(List<NoteLikeDO> noteLikeDOS, long expireSeconds) {
    int argsLength = noteLikeDOS.size() * 2 + 1; // 每个笔记点赞关系有2个参数（score和value），最后一个为过期时间
    Object[] luaArgs = new Object[argsLength];

    int i = 0;
    for (NoteLikeDO noteLikeDO : noteLikeDOS) {
      luaArgs[i] = DateUtils.localDateTime2Timestamp(noteLikeDO.getCreateTime()); // 点赞时间作为score
      luaArgs[i + 1] = noteLikeDO.getNoteId(); // 笔记ID作为ZSet value
      i += 2;
    }

    luaArgs[argsLength - 1] = expireSeconds;
    return luaArgs;
  }

  /**
   * 异步初始化布隆过滤器
   *
   * @param userId
   * @param expireSeconds
   * @param bloomUserNoteLikeListKey
   */
  private void asyncBatchAddNoteLike2BloomAndExpire(
      Long userId, long expireSeconds, String bloomUserNoteLikeListKey) {
    threadPoolTaskExecutor.submit(
        () -> {
          try {
            // 异步全量同步，并设置过期时间
            List<NoteLikeDO> noteLikeDOS = noteLikeDOMapper.selectByUserId(userId);

            if (CollUtil.isNotEmpty(noteLikeDOS)) {
              DefaultRedisScript<Long> script = new DefaultRedisScript<>();
              // Lua脚本路径
              script.setScriptSource(
                  new ResourceScriptSource(
                      new ClassPathResource("/lua/bloom_batch_add_note_like_and_expire.lua")));
              // 返回值类型
              script.setResultType(Long.class);

              // 构建Lua参数
              List<Object> luaArgs = Lists.newArrayList();
              noteLikeDOS.forEach(
                  noteLikeDO -> luaArgs.add(noteLikeDO.getNoteId())); // 将每个点赞的笔记ID传入
              luaArgs.add(expireSeconds); // 最后一个为过期时间
              redisTemplate.execute(
                  script, Collections.singletonList(bloomUserNoteLikeListKey), luaArgs.toArray());
            }
          } catch (Exception e) {
            log.error("## 异步初始化布隆过滤器异常： ", e);
          }
        });
  }

  /**
   * 校验笔记是否存在
   *
   * @param noteId
   */
  private Long checkNoteIsExistAndGetCreatorId(Long noteId) {
    // 先从本地缓存校验
    String findNoteDetailRspVOStrLocalCache = LOCAL_CACHE.getIfPresent(noteId);
    // 解析Json字符串为VO对象
    FindNoteDetailRspVO findNoteDetailRspVO =
        JsonUtils.parseObject(findNoteDetailRspVOStrLocalCache, FindNoteDetailRspVO.class);

    // 若本地缓存没有
    if (Objects.isNull(findNoteDetailRspVO)) {
      // 再从redis中校验
      String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
      String noteDetailJson = redisTemplate.opsForValue().get(noteDetailRedisKey);

      // 解析Json字符串为VO对象
      findNoteDetailRspVO = JsonUtils.parseObject(noteDetailJson, FindNoteDetailRspVO.class);

      // 都不存在，再查询数据库校验是否存在
      if (Objects.isNull(findNoteDetailRspVO)) {
        Long creatorId = noteDOMapper.selectCreatorIdByNoteId(noteId);
        // 若数据库也不存在，提示用户
        if (Objects.isNull(creatorId)) {
          throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 若数据库存在，异步同步缓存
        threadPoolTaskExecutor.submit(
            () -> {
              FindNoteDetailReqVO findNoteDetailReqVO =
                  FindNoteDetailReqVO.builder().id(noteId).build();
              findNoteDetail(findNoteDetailReqVO);
            });
        return creatorId;
      }
    }
    return findNoteDetailRspVO.getCreatorId();
  }

  /** 笔记可见性校验，针对vo实体类 */
  private void checkNoteVisibleFromVO(Long userId, FindNoteDetailRspVO findNoteDetailRspVO) {
    if (Objects.nonNull(findNoteDetailRspVO)) {
      Integer visible = findNoteDetailRspVO.getVisible();
      checkNoteVisible(visible, userId, findNoteDetailRspVO.getCreatorId());
    }
  }

  /**
   * 笔记可见性校验
   *
   * @param visible
   * @param userId
   * @param creatorId
   */
  private void checkNoteVisible(Integer visible, Long userId, Long creatorId) {
    if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode())
        && !Objects.equals(userId, creatorId)) {
      throw new BizException(ResponseCodeEnum.NOTE_PRIVATE);
    }
  }
}
