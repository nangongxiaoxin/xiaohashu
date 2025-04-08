package com.slilio.xiaohashu.note.biz.domain.dataobject;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelDO {
  private Long id;

  private String name;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;

  private Boolean isDeleted;
}
