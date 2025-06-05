package com.slilio.xiaohashu.kv.biz.domain.dataobject;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

/**
 * @Author: slilio @CreateTime: 2025-06-06 @Description: @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyClass
public class CommentContentPrimaryKey {
  @PrimaryKeyColumn(name = "note_id", type = PrimaryKeyType.PARTITIONED)
  private Long noteId; // 分区键1

  @PrimaryKeyColumn(name = "year_month", type = PrimaryKeyType.PARTITIONED)
  private String yearMonth; // 分区键2

  @PrimaryKeyColumn(name = "content_id", type = PrimaryKeyType.CLUSTERED)
  private UUID contentId; // 聚簇键
}
