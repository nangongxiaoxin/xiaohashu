package com.slilio.xiaohashu.comment.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-18 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindChildCommentItemRspVO {
  private Long commentId; // 评论ID
  private Long userId; // 发布者用户ID
  private String avatar; // 头像
  private String nickname; // 昵称
  private String content; // 评论内容
  private String imageUrl; // 评论图片
  private String crateTime; // 发布时间
  private Long likeTotal; // 被点赞数
  private String replyUserName; // 回复的用户昵称;
  private Long replyUserId; // 回复的用户昵称
}
