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
public class AggregationCountLikeUnlikeCommentMqDTO {

  // 评论ID
  private Long commentId;

  // 聚合后的计数
  private Integer count;
}
