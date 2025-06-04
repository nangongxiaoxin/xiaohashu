package com.slilio.xiaohashu.comment.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-05 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishCommentReqVO {
  @NotNull(message = "笔记ID不能为空")
  private Long noteId;

  private String content; // 评论内容

  private String imgUrl; // 评论图片地址

  private Long replyCommentId; // 回复的评论ID(父级评论ID)
}
