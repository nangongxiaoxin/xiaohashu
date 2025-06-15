package com.slilio.xiaohashu.comment.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-15 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindCommentPageListReqVO {
  @NotNull(message = "笔记ID不能为空")
  private Long noteId;

  @NotNull(message = "页码不能为空")
  private Integer pageNo = 1;
}
