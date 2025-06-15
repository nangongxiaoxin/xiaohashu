package com.slilio.xiaohashu.kv.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.kv.biz.domain.dataobject.CommentContentDO;
import com.slilio.xiaohashu.kv.biz.domain.dataobject.CommentContentPrimaryKey;
import com.slilio.xiaohashu.kv.biz.domain.repository.CommentContentRepository;
import com.slilio.xiaohashu.kv.biz.service.CommentContentService;
import com.slilio.xiaohashu.kv.dto.req.BatchAddCommentContentReqDTO;
import com.slilio.xiaohashu.kv.dto.req.BatchFindCommentContentReqDTO;
import com.slilio.xiaohashu.kv.dto.req.CommentContentReqDTO;
import com.slilio.xiaohashu.kv.dto.req.FindCommentContentReqDTO;
import com.slilio.xiaohashu.kv.dto.rsp.FindCommentContentRspDTO;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

/**
 * @Author: slilio @CreateTime: 2025-06-06 @Description: @Version: 1.0
 */
@Service
@Slf4j
public class CommentContentServiceImpl implements CommentContentService {
  @Resource private CassandraTemplate cassandraTemplate;
  @Resource private CommentContentRepository commentContentRepository;

  /**
   * 批量添加评论内容
   *
   * @param batchAddCommentContentReqDTO
   * @return
   */
  @Override
  public Response<?> batchAddCommentContent(
      BatchAddCommentContentReqDTO batchAddCommentContentReqDTO) {
    List<CommentContentReqDTO> comments = batchAddCommentContentReqDTO.getComments();

    // DTO转DO
    List<CommentContentDO> contentDOS =
        comments.stream()
            .map(
                commentContentReqDTO -> {
                  // 构建主键类
                  CommentContentPrimaryKey commentContentPrimaryKey =
                      CommentContentPrimaryKey.builder()
                          .noteId(commentContentReqDTO.getNoteId())
                          .yearMonth(commentContentReqDTO.getYearMonth())
                          .contentId(UUID.fromString(commentContentReqDTO.getContentId()))
                          .build();

                  // DO实体类
                  CommentContentDO commentContentDO =
                      CommentContentDO.builder()
                          .primaryKey(commentContentPrimaryKey)
                          .content(commentContentReqDTO.getContent())
                          .build();

                  return commentContentDO;
                })
            .toList();

    // 批量插入
    cassandraTemplate.batchOps().insert(contentDOS).execute();

    return Response.success();
  }

  /**
   * 批量查询评论内容
   *
   * @param batchFindCommentContentReqDTO
   * @return
   */
  @Override
  public Response<?> batchFindCommentContent(
      BatchFindCommentContentReqDTO batchFindCommentContentReqDTO) {

    // 归属的笔记ID
    Long noteId = batchFindCommentContentReqDTO.getNoteId();

    // 查询评论发布的年月、内容UUID
    List<FindCommentContentReqDTO> commentContentKeys =
        batchFindCommentContentReqDTO.getCommentContentKeys();

    // 过滤出年月
    List<String> yearMonths =
        commentContentKeys.stream()
            .map(FindCommentContentReqDTO::getYearMonth)
            .distinct() // 去重
            .toList();

    // 过滤出内容UUID
    List<UUID> contentIds =
        commentContentKeys.stream()
            .map(commentContentKey -> UUID.fromString(commentContentKey.getContentId()))
            .distinct() // 去重
            .toList();

    // 批量查询Cassandra
    List<CommentContentDO> commentContentDOS =
        commentContentRepository
            .findByPrimaryKeyNoteIdAndPrimaryKeyYearMonthInAndPrimaryKeyContentIdIn(
                noteId, yearMonths, contentIds);

    // DO转DTO
    List<FindCommentContentRspDTO> findCommentContentRspDTOS = Lists.newArrayList();
    if (CollUtil.isNotEmpty(commentContentDOS)) {
      findCommentContentRspDTOS =
          commentContentDOS.stream()
              .map(
                  commentContentDO ->
                      FindCommentContentRspDTO.builder()
                          .contentId(
                              String.valueOf(commentContentDO.getPrimaryKey().getContentId()))
                          .content(commentContentDO.getContent())
                          .build())
              .toList();
    }

    return Response.success(findCommentContentRspDTOS);
  }
}
