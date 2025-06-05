package com.slilio.xiaohashu.kv.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-06 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAddCommentContentReqDTO {

  @NotEmpty(message = "评论内容集合不能为空")
  @Valid // 指定集合内的DTO，也需要进行参数校验
  private List<CommentContentReqDTO> comments;
}
