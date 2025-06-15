package com.slilio.xiaohashu.comment.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.slilio.framework.biz.context.holder.LoginUserContextHolder;
import com.slilio.framework.common.constant.DateConstants;
import com.slilio.framework.common.response.PageResponse;
import com.slilio.framework.common.response.Response;
import com.slilio.framework.common.util.DateUtils;
import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.comment.biz.constant.MQConstants;
import com.slilio.xiaohashu.comment.biz.domain.dataobject.CommentDO;
import com.slilio.xiaohashu.comment.biz.domain.mapper.CommentDOMapper;
import com.slilio.xiaohashu.comment.biz.domain.mapper.NoteCountDOMapper;
import com.slilio.xiaohashu.comment.biz.model.dto.PublishCommentMqDTO;
import com.slilio.xiaohashu.comment.biz.model.vo.FindCommentItemRspVO;
import com.slilio.xiaohashu.comment.biz.model.vo.FindCommentPageListReqVO;
import com.slilio.xiaohashu.comment.biz.model.vo.PublishCommentReqVO;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
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

    // todo：先从缓存查询

    // 查询评论总数（从t_note_count笔记计数表查询，提升查询性能，避免count(*)）
    Long count = noteCountDOMapper.selectCommentTotalByNoteId(noteId);

    if (Objects.isNull(count)) {
      return PageResponse.success(null, pageNo, pageSize);
    }

    // 分页返参
    List<FindCommentItemRspVO> commentRspVOS = null;

    // 若评论总数大于0
    if (count > 0) {
      commentRspVOS = Lists.newArrayList();

      // 计算分页查询的偏移量offset
      long offset = PageResponse.getOffset(pageNo, pageSize);

      // 查询一级评论
      List<CommentDO> oneLevelCommentDOS = commentDOMapper.selectPageList(noteId, offset, pageSize);

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
                    .build();

            // 用户信息 static 对象引用
            setUserInfo(
                commentIdAndDOMap,
                userIdAndDTOMap,
                firstReplyCommentUserId,
                firstReplyCommentRspVO);

            oneLevelCommentRspVO.setFirstReplyComment(firstReplyCommentRspVO);
            // 笔记内容 static 对象引用
            setCommentContent(
                commentUuidAndContentMap, firstReplyCommentDO, firstReplyCommentRspVO);
          }
        }
        commentRspVOS.add(oneLevelCommentRspVO);
      }
    }

    return PageResponse.success(commentRspVOS, pageNo, count, pageSize);
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
