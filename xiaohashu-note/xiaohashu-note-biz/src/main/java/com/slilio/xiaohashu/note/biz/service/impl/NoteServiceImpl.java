package com.slilio.xiaohashu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Preconditions;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.exception.BizException;
import com.slilio.framework.common.response.Response;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
  @Resource private NoteDOMapper noteDOMapper;
  @Resource private TopicDOMapper topicDOMapper;
  @Resource private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
  @Resource private KeyValueRpcService keyValueRpcService;
  @Resource private UserRpcService userRpcService;

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
  public Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO) {
    // 查询的笔记ID
    Long noteId = findNoteDetailReqVO.getId();

    // 当前登录用户
    Long userId = LoginUserContextHolder.getUserId();

    // 查询笔记
    NoteDO noteDO = noteDOMapper.selectByPrimaryKey(noteId);

    // 若该笔记不存在则抛出业务异常
    if (Objects.isNull(noteDO)) {
      throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
    }

    // 可见性校验
    Integer visible = noteDO.getVisible();
    checkNoteVisible(visible, userId, noteDO.getCreatorId());

    // RPC：调用用户服务
    Long creatorId = noteDO.getCreatorId();
    FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(userId);

    // RPC：调用K-V存储服务获取内容
    String content = null;
    if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
      content = keyValueRpcService.findNoteContent(noteDO.getContentUuid());
    }

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
    FindNoteDetailRspVO findNoteDetailRspVO =
        FindNoteDetailRspVO.builder()
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

    return Response.success(findNoteDetailRspVO);
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
