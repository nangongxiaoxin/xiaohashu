package com.slilio.xiaohashu.note.biz.domain.dataobject;

import java.time.LocalDateTime;
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

  private LocalDateTime createTime;

  private Integer status;
}
