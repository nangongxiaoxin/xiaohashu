package com.slilio.xiaohashu.comment.biz.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-21 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeUnlikeCommentMqDTO {
  private Long userId;
  private Long commentId;

  // 0：取消点赞 1：点赞
  private Integer type;
  private LocalDateTime createTime;
}
