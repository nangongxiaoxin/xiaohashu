package com.slilio.xiaohashu.user.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-04-25 @Description: 入参实体类 @Version: 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FindUsersByIdsReqDTO {
  @NotNull(message = "用户ID集合不能为空")
  @Size(min = 1, max = 10, message = "用户ID集合必须大于等于1，小于等于10")
  private List<Long> ids;
}
