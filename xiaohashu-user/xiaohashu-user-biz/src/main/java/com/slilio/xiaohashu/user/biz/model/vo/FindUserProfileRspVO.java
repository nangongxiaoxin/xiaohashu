package com.slilio.xiaohashu.user.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-27 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindUserProfileRspVO {
  private Long userId;
  private String avatar;
  private String nickname;
  private String xiaohashuId;
  private Integer sex;
  private Integer age;
  private String introduction;
  private String followingTotal;
  private String fansTotal;
  private String likeAndCollectTotal;
  private String noteTotal;
  private String likeTotal;
  private String collectTotal;
}
