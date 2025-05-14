package com.slilio.xiaohashu.count.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-14 @Description: @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AggregationCountCollectUnCollectNoteMqDTO {
  private Long creatorId;
  private Long noteId;
  private Integer count;
}
