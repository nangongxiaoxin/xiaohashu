package com.slilio.xiaohashu.kv.dto.req;

import jakarta.validation.constraints.NotBlank;
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
public class FindCommentContentReqDTO {
  @NotBlank(message = "发布年月不能为空")
  private String yearMonth;

  @NotBlank(message = "评论正文ID不能为空")
  private String contentId;
}
