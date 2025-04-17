package com.slilio.xiaohashu.note.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-17 @Description: 笔记置顶/取消 @Version: 1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TopNoteReqVO {
  @NotNull(message = "笔记ID不能为空")
  private Long id;

  @NotNull(message = "置顶状态不能为空")
  private Boolean isTop;
}
