package com.slilio.xiaohashu.comment.biz.domain.dataobject;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDO {
  private Long id;

  private Long noteId;

  private Long userId;

  private String contentUuid;

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

  private Long childCommentTotal;
}
