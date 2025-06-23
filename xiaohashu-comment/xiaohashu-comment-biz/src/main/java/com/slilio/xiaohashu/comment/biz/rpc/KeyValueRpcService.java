package com.slilio.xiaohashu.comment.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.slilio.framework.common.constant.DateConstants;
import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.comment.biz.model.bo.CommentBO;
import com.slilio.xiaohashu.kv.api.KeyValueFeignApi;
import com.slilio.xiaohashu.kv.dto.req.*;
import com.slilio.xiaohashu.kv.dto.rsp.FindCommentContentRspDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-06 @Description: @Version: 1.0
 */
@Component
public class KeyValueRpcService {
  @Resource private KeyValueFeignApi keyValueFeignApi;

  /**
   * 批量存储评论内容
   *
   * @param commentBOS
   * @return
   */
  public boolean batchSaveCommentContent(List<CommentBO> commentBOS) {
    List<CommentContentReqDTO> comments = Lists.newArrayList();

    // BO转DTO
    commentBOS.forEach(
        commentBO -> {
          CommentContentReqDTO commentContentReqDTO =
              CommentContentReqDTO.builder()
                  .noteId(commentBO.getNoteId())
                  .content(commentBO.getContent())
                  .contentId(commentBO.getContentUuid())
                  .yearMonth(commentBO.getCreateTime().format(DateConstants.DATE_FORMAT_Y_M))
                  .build();
          comments.add(commentContentReqDTO);
        });

    // 构建接口入参实体类
    BatchAddCommentContentReqDTO batchAddCommentContentReqDTO =
        BatchAddCommentContentReqDTO.builder().comments(comments).build();

    // 调用KV存储服务
    Response<?> response = keyValueFeignApi.batchAddCommentContent(batchAddCommentContentReqDTO);

    // 若返参中success为false，则主动抛出异常，以便调用层回滚事务
    if (!response.isSuccess()) {
      throw new RuntimeException("批量保存评论内容失败");
    }

    return true;
  }

  /**
   * 批量查询批量内容
   *
   * @param noteId
   * @param findCommentContentReqDTOS
   * @return
   */
  public List<FindCommentContentRspDTO> batchFindCommentContent(
      Long noteId, List<FindCommentContentReqDTO> findCommentContentReqDTOS) {
    BatchFindCommentContentReqDTO batchFindCommentContentReqDTO =
        BatchFindCommentContentReqDTO.builder()
            .noteId(noteId)
            .commentContentKeys(findCommentContentReqDTOS)
            .build();

    Response<List<FindCommentContentRspDTO>> response =
        keyValueFeignApi.batchFindCommentContent(batchFindCommentContentReqDTO);

    if (!response.isSuccess()
        || Objects.isNull(response.getData())
        || CollUtil.isEmpty(response.getData())) {
      return null;
    }
    return response.getData();
  }

  /**
   * 删除评论内容
   *
   * @param noteId
   * @param createTime
   * @param contentId
   * @return
   */
  public boolean deleteCommentContent(Long noteId, LocalDateTime createTime, String contentId) {
    DeleteCommentContentReqDTO deleteCommentContentReqDTO =
        DeleteCommentContentReqDTO.builder()
            .noteId(noteId)
            .yearMonth(DateConstants.DATE_FORMAT_Y_M.format(createTime))
            .contentId(contentId)
            .build();

    // 调用KV存储服务
    Response<?> response = keyValueFeignApi.deleteCommentContent(deleteCommentContentReqDTO);

    if (!response.isSuccess()) {
      throw new RuntimeException("删除评论内容失败");
    }

    return true;
  }
}
