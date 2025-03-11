package com.slilio.xiaohashu.auth.domain.dataobject;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDO {
  private Long id;

  private String xiaohashuId;

  private String password;

  private String nickname;

  private String avatar;

  private LocalDateTime birthday;

  private String backgroundImg;

  private String phone;

  private Integer sex;

  private Integer status;

  private String introduction;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;

  private Boolean isDeleted;
}
