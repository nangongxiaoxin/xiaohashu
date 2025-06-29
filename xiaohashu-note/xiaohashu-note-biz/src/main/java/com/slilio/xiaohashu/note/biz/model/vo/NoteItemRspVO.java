package com.slilio.xiaohashu.note.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-28 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteItemRspVO {
  private Long noteId; // 笔记ID
  private Integer type; // 笔记类型(0：图文 1：视频)
  private String cover; // 笔记封面
  private String videoUri; // 视频文件链接
  private String title; // 笔记标题
  private Long creatorId; // 发布者笔记ID
  private String nickname; // 昵称
  private String avatar; // 头像
  private String likeTotal; // 被点赞数
  private Boolean isLiked; // 当前登录用户是否已经点赞
}
