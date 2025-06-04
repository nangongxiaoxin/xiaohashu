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
public class CommentLikeDO {
  private Long id;

  private Long userId;

  private Long commentId;

  private LocalDateTime createTime;
}
