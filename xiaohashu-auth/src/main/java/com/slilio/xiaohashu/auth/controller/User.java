package com.slilio.xiaohashu.auth.controller;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
  /** 昵称 */
  @NotBlank(message = "昵称不能为空")
  private String nickName;

  /** 创建时间 */
  private LocalDateTime createTime;
}
