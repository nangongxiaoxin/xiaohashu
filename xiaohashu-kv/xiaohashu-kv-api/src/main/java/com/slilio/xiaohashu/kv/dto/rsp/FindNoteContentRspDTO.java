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

  private UUID noteId; // 笔记ID
  private String content; // 笔记内容
}
