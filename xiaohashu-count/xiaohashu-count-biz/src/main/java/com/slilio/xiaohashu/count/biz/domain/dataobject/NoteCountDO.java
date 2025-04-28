package com.slilio.xiaohashu.count.biz.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteCountDO {
  private Long id;

  private Long noteId;

  private Long likeTotal;

  private Long collectTotal;

  private Long commentTotal;
}
