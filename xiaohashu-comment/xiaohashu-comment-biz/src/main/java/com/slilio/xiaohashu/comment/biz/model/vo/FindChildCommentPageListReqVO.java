package com.slilio.xiaohashu.comment.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-18 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindChildCommentPageListReqVO {
  @NotNull(message = "父评论ID不能为空")
  private Long parentCommentId;

  @NotNull(message = "页码不能为空")
  private Integer pageNo = 1;
}
