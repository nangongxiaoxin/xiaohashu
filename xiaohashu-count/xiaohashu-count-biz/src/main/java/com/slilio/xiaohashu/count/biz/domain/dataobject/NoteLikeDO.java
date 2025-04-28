package com.slilio.xiaohashu.count.biz.domain.dataobject;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteLikeDO {
  private Long id;

  private Long userId;

  private Long noteId;

  private Date createTime;

  private Byte status;
}
