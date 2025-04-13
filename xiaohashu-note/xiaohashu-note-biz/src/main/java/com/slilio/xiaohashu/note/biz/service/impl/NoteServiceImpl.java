package com.slilio.xiaohashu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.note.biz.constant.MQConstants;
import com.slilio.xiaohashu.note.biz.constant.RedisKeyConstants;
import com.slilio.xiaohashu.note.biz.domain.dataobject.NoteDO;
import com.slilio.xiaohashu.note.biz.domain.mapper.NoteDOMapper;
import com.slilio.xiaohashu.note.biz.domain.mapper.TopicDOMapper;
import com.slilio.xiaohashu.note.biz.enums.NoteStatusEnum;
import com.slilio.xiaohashu.note.biz.enums.NoteTypeEnum;
import com.slilio.xiaohashu.note.biz.enums.NoteVisibleEnum;
import com.slilio.xiaohashu.note.biz.enums.ResponseCodeEnum;
import com.slilio.xiaohashu.note.biz.model.vo.FindNoteDetailReqVO;
import com.slilio.xiaohashu.note.biz.model.vo.FindNoteDetailRspVO;
import com.slilio.xiaohashu.note.biz.model.vo.PublishNoteReqVO;
import com.slilio.xiaohashu.note.biz.model.vo.UpdateNoteReqVO;
import com.slilio.xiaohashu.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.slilio.xiaohashu.note.biz.rpc.KeyValueRpcService;
import com.slilio.xiaohashu.note.biz.rpc.UserRpcService;
import com.slilio.xiaohashu.note.biz.service.NoteService;
import com.slilio.xiaohashu.user.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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

  @Resource(name = "taskExecutor")
  private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  @Resource private RedisTemplate<String, String> redisTemplate;

  /** 笔记详情本地缓存 */
  private static final Cache<Long, String> LOCAL_CACHE =
      Caffeine.newBuilder()
          .initialCapacity(10000) // 设置初始缓存容量为10000个条目
          .maximumSize(10000) // 设置缓存的最大容量为10000个条目
          .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存的条目在写入后一个小时过期
          .build();

  @Autowired private ResourceLoader resourceLoader;
  @Autowired private RocketMQTemplate rocketMQTemplate;

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
      // 内容是否为空，默认为flase，即为不空
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

    return Response.success();
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
    String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
    redisTemplate.delete(noteDetailRedisKey);

    // 删除本地缓存
    // LOCAL_CACHE.invalidate(noteId);
    // 同步发送广播模式MQ，将所有实例中本地缓存都删除
    rocketMQTemplate.syncSend(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
    log.info("=====》 MQ：删除笔记本地缓存发送成功。。。");

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
   * 删除本地笔记缓存
   *
   * @param noteId
   */
  @Override
  public void deleteNoteLocalCache(Long noteId) {
    LOCAL_CACHE.invalidate(noteId);
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
