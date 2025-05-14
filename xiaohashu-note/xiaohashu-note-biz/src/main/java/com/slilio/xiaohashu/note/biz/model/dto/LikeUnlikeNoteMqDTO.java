package com.slilio.xiaohashu.note.biz.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-05 @Description: MQ DTO实体类 @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeUnlikeNoteMqDTO {
  private Long userId;
  private Long noteId;
  private Integer type; // 0：取消点赞；1：点赞
  private LocalDateTime createTime;
  private Long noteCreatorId;
}
