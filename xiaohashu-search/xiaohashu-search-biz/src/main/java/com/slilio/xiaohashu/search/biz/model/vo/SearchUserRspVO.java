package com.slilio.xiaohashu.search.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-27 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserRspVO {
  private Long userId; // 用户ID
  private String nickname; // 昵称
  private String avatar; // 头像
  private String xiaohashuId; // 小哈书ID
  private Integer noteTotal; // 笔记发布总数
  private String fansTotal; // 粉丝总数

  private String highlightNickname; // 昵称：关键词高亮
}
