package com.slilio.xiaohashu.user.relation.biz.domain.dataobject;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowingDO {
  private Long id;

  private Long userId;

  private Long followingUserId;

  private Date createTime;
}
