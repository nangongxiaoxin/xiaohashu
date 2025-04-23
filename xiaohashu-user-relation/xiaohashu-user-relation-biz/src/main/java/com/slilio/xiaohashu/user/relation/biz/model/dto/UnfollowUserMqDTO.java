package com.slilio.xiaohashu.user.relation.biz.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-24 @Description: 消息体实体 @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UnfollowUserMqDTO {
  private Long userId;
  private Long unfollowUserId;
  private LocalDateTime createTime;
}
