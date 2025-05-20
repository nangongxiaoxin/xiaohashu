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
public class LikeUnlikeNoteMqDTO {
  private Long userId;
  private Long noteId;
  private Integer type; // 0：取消点赞 1：点赞
  private Long noteCreatorId; // 笔记发布者ID
  private LocalDateTime createTime;
}
