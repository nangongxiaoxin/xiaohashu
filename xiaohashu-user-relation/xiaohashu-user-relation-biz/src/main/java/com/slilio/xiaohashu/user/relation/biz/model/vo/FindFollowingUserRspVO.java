package com.slilio.xiaohashu.user.relation.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-26 @Description: 查询关注列表 @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindFollowingUserRspVO {
  private Long userId;
  private String avatar;
  private String nickname;
  private String introduction;
}
