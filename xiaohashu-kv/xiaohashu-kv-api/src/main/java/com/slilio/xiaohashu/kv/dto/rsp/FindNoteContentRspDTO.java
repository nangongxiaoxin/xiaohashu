package com.slilio.xiaohashu.kv.dto.rsp;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindNoteContentRspDTO {

  private UUID uuid; // 笔记uuid
  private String content; // 笔记内容
}
