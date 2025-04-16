package com.slilio.xiaohashu.note.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateNoteVisibleOnlyMeReqVO {
  @NotNull(message = "笔记ID不能为空")
  private Long id;
}
