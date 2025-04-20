package com.slilio.xiaohashu.user.relation.biz.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-21 @Description: 消息实体 @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowUserMqDTO {
  private Long userId;
  private Long followUserId;
  private LocalDateTime createTime;
}
