package com.slilio.xiaohashu.count.biz.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-08 @Description: 点赞服务MQ实体 @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountLikeUnLikeNoteMqDTO {
  private Long userId;
  private Long noteId;
  private Integer type; // 0：取消点赞 1：点赞
  private LocalDateTime createTime;
  private Long noteCreatorId;
}
