package com.slilio.xiaohashu.count.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-29 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindNoteCountsByIdRspDTO {
  private Long noteId;
  private Long likeTotal; // 点赞总数
  private Long collectTotal; // 收藏总数
  private Long commentTotal; // 评论总数
}
