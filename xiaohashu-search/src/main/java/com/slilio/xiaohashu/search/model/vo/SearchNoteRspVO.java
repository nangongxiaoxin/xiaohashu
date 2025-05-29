package com.slilio.xiaohashu.search.model.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-29 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchNoteRspVO {
  private Long noteId; // 笔记ID
  private String cover; // 封面
  private String title; // 标题
  private String highlightTitle; // 关键词高亮
  private String avatar; // 发布者头像
  private String nickname; // 发布者昵称
  private LocalDateTime updateTime; // 最后一次编辑时间
  private String likeTotal; // 被总点赞数
  private String commentTotal; // 被总评论数
  private String collectTotal; // 被总收藏数
}
