package com.slilio.xiaohashu.count.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-06-29 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindNoteCountsByIdsReqDTO {

  @NotNull(message = "笔记ID集合不能为空")
  @Size(min = 1, max = 20, message = "笔记ID集合大小必须大于等于1，小于等于20")
  private List<Long> noteIds;
}
