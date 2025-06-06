package com.slilio.xiaohashu.comment.biz.model.bo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-06 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentBO {
  private Long id;

  private Long noteId;

  private Long userId;

  private String contentUuid;

  private String content;

  private Boolean isContentEmpty;

  private String imageUrl;

  private Integer level;

  private Long replyTotal;

  private Long likeTotal;

  private Long parentId;

  private Long replyCommentId;

  private Long replyUserId;

  private Boolean isTop;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;
}
