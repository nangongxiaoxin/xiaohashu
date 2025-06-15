package com.slilio.xiaohashu.kv.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-14 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchFindCommentContentReqDTO {
  @NotNull(message = "评论ID不能为空")
  private Long noteId;

  @NotEmpty(message = "评论内容不能为空")
  @Valid // 确保嵌套的对象也会被验证 指定集合内的DTO也需要被验证
  private List<FindCommentContentReqDTO> commentContentKeys;
}
