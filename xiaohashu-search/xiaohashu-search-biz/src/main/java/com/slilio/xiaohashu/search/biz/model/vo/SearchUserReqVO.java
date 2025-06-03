package com.slilio.xiaohashu.search.biz.model.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-05-27 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserReqVO {
  @NotBlank(message = "搜索关键词不能为空")
  private String keyword;

  @Min(value = 1, message = "页码不能小于 1")
  private Integer pageNo = 1; // 默认第一页
}
