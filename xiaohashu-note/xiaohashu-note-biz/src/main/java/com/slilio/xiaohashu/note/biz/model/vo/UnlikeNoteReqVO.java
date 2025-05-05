package com.slilio.xiaohashu.note.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-06 @Description: 笔记取消点赞入参 @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnlikeNoteReqVO {
  @NotNull(message = "笔记ID不能为空")
  private Long id;
}
