package com.slilio.xiaohashu.data.align.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-21 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectUnCollectNoteMqDTO {
  private Long userId;
  private Long noteId;
  private Integer type; // 1：收藏 0：取消收藏
  private LocalDateTime createTime;
  private Long noteCreatorId; // 笔记发布者ID
}
