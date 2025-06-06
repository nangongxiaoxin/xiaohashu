package com.slilio.xiaohashu.comment.biz.model.dto;

import java.time.LocalDateTime;
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
public class PublishCommentMqDTO {
  private Long noteId;

  private String content; // 评论内容

  private String imageUrl; // 评论图片地址

  private Long replyCommentId; // 回复评论的ID

  private LocalDateTime createTime; // 发布时间

  private Long creatorId; // 发布者ID

  private Long commentId; // 评论ID
}
