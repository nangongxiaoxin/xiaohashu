package com.slilio.xiaohashu.search.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-03 @Description: DTO入参 @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RebuildNoteDocumentReqDTO {
  @NotNull(message = "笔记ID不能为空")
  private Long id;
}
