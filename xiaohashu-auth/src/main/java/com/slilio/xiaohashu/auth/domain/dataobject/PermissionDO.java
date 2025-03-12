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
public class PermissionDO {
  private Long id;

  private Long parentId;

  private String name;

  private String type;

  private String menuUrl;

  private String menuIcon;

  private Integer sort;

  private String permissionKey;

  private String status;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;

  private Boolean isDeleted;
}
