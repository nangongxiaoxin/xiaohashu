package com.slilio.xiaohashu.user.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindUserByIdRspDTO {

  private Long id; // 用户ID

  private String nickName; // 昵称

  private String avatar; // 头像

  private String introduction; // 简介
}
