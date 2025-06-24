package com.slilio.xiaohashu.note.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-24 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindNoteIsLikedAndCollectedRspVO {
  private Long noteId; // 笔记ID
  private Boolean isLiked; // 是否被当前登录的用户点赞
  private Boolean isCollected; // 是否被当前登录的用户收藏
}
