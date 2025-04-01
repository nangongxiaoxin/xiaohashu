package com.slilio.xiaohashu.user.biz.domain.dataobject;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRoleDO {
  private Long id;

  private Long userId;

  private Long roleId;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;

  private Boolean isDeleted;
}
