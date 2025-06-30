package com.slilio.xiaohashu.note.biz.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: slilio @CreateTime: 2025-07-01 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishNoteDTO {
  private Long id;

  private String title;

  private Boolean isContentEmpty;

  private Long creatorId;

  private Long topicId;

  private String topicName;

  private Boolean isTop;

  private Integer type;

  private String imgUris;

  private String videoUri;

  private Integer visible;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;

  private Integer status;

  private String contentUuid;

  private String content;
}
