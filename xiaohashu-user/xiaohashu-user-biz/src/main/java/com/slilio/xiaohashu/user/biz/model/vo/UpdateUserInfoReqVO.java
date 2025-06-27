package com.slilio.xiaohashu.user.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateUserInfoReqVO {
  @NotNull(message = "用户ID不能为空")
  private Long userId; // 用户ID

  private MultipartFile avatar; // 头像
  private String nickname; // 昵称
  private String xiaohashuId; // 小哈书ID
  private Integer sex; // 性别
  private LocalDate birthday; // 生日
  private String introduction; // 介绍
  private MultipartFile backgroundImg; // 背景图
}
