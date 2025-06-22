package com.slilio.xiaohashu.count.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-23 @Description: @Version: 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountLikeUnlikeCommentMqDTO {
  private Long userId;
  private Long commentId;

  // 0:取消点赞，1：点赞
  private Integer type;
}
