package com.slilio.xiaohashu.data.align.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-23 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteOperateMqDTO {
  private Long creatorId; // 笔记发布者ID
  private Long noteId; // 笔记ID
  private Integer type; // 操作类型：0：删除笔记；1：笔记发布
}
