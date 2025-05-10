package com.slilio.xiaohashu.note.biz.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-11 @Description: 消息体传输数据 @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CollectUnCollectNoteMqDTO {
  private Long userId;
  private Long noteId;
  private Integer type; // 0：取消收藏；1：收藏
  private LocalDateTime createTime;
}
