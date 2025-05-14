package com.slilio.xiaohashu.count.biz.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-13 @Description: 收藏Mq @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CountCollectUnCollectNoteMqDTO {
  private Long userId;

  private Long noteId;

  /** 0: 取消收藏， 1：收藏 */
  private Integer type;

  private LocalDateTime createTime;

  private Long noteCreatorId;
}
