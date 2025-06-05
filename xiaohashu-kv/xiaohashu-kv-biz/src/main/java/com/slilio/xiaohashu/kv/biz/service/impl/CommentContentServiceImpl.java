package com.slilio.xiaohashu.kv.biz.service.impl;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.kv.biz.domain.dataobject.CommentContentDO;
import com.slilio.xiaohashu.kv.biz.domain.dataobject.CommentContentPrimaryKey;
import com.slilio.xiaohashu.kv.biz.service.CommentContentService;
import com.slilio.xiaohashu.kv.dto.req.BatchAddCommentContentReqDTO;
import com.slilio.xiaohashu.kv.dto.req.CommentContentReqDTO;
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
}
