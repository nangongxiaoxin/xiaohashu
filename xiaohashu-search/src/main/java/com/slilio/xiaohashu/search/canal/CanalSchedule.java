package com.slilio.xiaohashu.search.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.common.collect.Maps;
import com.slilio.xiaohashu.search.domain.mapper.SelectMapper;
import com.slilio.xiaohashu.search.enums.NoteStatusEnum;
import com.slilio.xiaohashu.search.enums.NoteVisibleEnum;
import com.slilio.xiaohashu.search.index.NoteIndex;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Author: slilio @CreateTime: 2025-06-01 @Description: @Version: 1.0
 */
@Component
@Slf4j
public class CanalSchedule implements Runnable {

  @Resource private CanalProperties canalProperties;
  @Resource private CanalConnector canalConnector;
  @Resource private SelectMapper selectMapper;
  @Resource private RestHighLevelClient restHighLevelClient;

  @Override
  @Scheduled(fixedRate = 1000) // 每隔100ms被执行一次
  public void run() {
    // 初始化批次ID，-1表示未开始或未获取到数据
    long batchId = -1L;
    try {
      // 从canalConnector获取批量信息，返回的数据量由batchSize控制，若不足，则拉取已有的
      Message message = canalConnector.getWithoutAck(canalProperties.getBatchSize());
      // 获取当前拉取消息的批次OD
      batchId = message.getId();

      // 获取当前批次中的数据条数
      long size = message.getEntries().size();
      if (batchId == -1 || size == 0) {
        try {
          // 拉取数据为空，休眠1s，防止频繁拉取
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
        }
      } else {
        // 如果当前批次有数据，处理这批次中的数据条目
        printEntry(message.getEntries());
      }

      // 对当前批次消息进行ack确认，表示该批次的数据已经被成功消费
      canalConnector.ack(batchId);
    } catch (Exception e) {
      log.error("消费Canal批次数据异常", e);
      // 如果出现异常，需要进行数据回滚，以便重新消费这批次的数据
      canalConnector.rollback(batchId);
    }
  }

  /**
   * 打印这一批次中的数据条目（和官方示例代码一致，后续小节中会自定义这块）
   *
   * @param entrys
   */
  private void printEntry(List<CanalEntry.Entry> entrys) throws Exception {
    for (CanalEntry.Entry entry : entrys) {
      // 只处理 ROW DATA 行数据类型的 Entry，忽略事务等其他类型
      if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
        // 获取事件类型（如：INSERT、UPDATE、DELETE 等等）
        CanalEntry.EventType eventType = entry.getHeader().getEventType();
        // 获取数据库名称
        String database = entry.getHeader().getSchemaName();
        // 获取表名称
        String table = entry.getHeader().getTableName();
        // 解析出 RowChange 对象，包含 RowData 和事件相关信息
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());

        // 遍历所有行数据（RowData）
        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
          // 获取行中所有列的最新值（AfterColumns）
          List<CanalEntry.Column> columns = rowData.getAfterColumnsList();

          // 将列数据解析为Mqp，方便后续处理
          Map<String, Object> columnMap = parseColumns2Map(columns);

          // todo：自定义处理
          log.info(
              "EventType：{}，Database：{}，Tables：{}，Columns:{}",
              eventType,
              database,
              table,
              columnMap);

          // 处理事件
          processEvent(columnMap, table, eventType);
        }
      }
    }
  }

  /**
   * 处理事件
   *
   * @param columnMap 列数据
   * @param table 表名
   * @param eventType 事件类型
   */
  private void processEvent(
      Map<String, Object> columnMap, String table, CanalEntry.EventType eventType)
      throws Exception {
    switch (table) {
      case "t_note" -> handleNoteEvent(columnMap, eventType); // 笔记表
      case "t_user" -> handleUserEvent(columnMap, eventType); // 用户表
      default -> log.warn("Table {} not support", table);
    }
  }

  /**
   * 处理用户表事件
   *
   * @param columnMap 列数据
   * @param eventType 事件类型
   */
  private void handleUserEvent(Map<String, Object> columnMap, CanalEntry.EventType eventType) {}

  /**
   * 处理笔记表事件
   *
   * @param columnMap 列数据
   * @param eventType 事件类型
   */
  private void handleNoteEvent(Map<String, Object> columnMap, CanalEntry.EventType eventType)
      throws Exception {
    // 获取笔记ID
    Long noteId = Long.parseLong(columnMap.get("id").toString());

    // 不同的事件，处理逻辑不同
    switch (eventType) {
      case INSERT -> syncNoteIndex(noteId); // 记录新增事件
      case UPDATE -> {
        // 记录更新事件

        // 笔记变更后的状态
        Integer status = Integer.parseInt(columnMap.get("status").toString());
        // 笔记可见范围
        Integer visible = Integer.parseInt(columnMap.get("visible").toString());

        if (Objects.equals(status, NoteStatusEnum.NORMAL.getCode())
            && Objects.equals(visible, NoteVisibleEnum.PUBLIC.getCode())) {
          // 正常展示，并且可见性为公开

          // 对索引进行覆盖更新
          syncNoteIndex(noteId);
        } else if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode()) // 仅对自己可见
            || Objects.equals(status, NoteStatusEnum.DELETED.getCode()) // 被逻辑删除
            || Objects.equals(status, NoteStatusEnum.DOWNED.getCode()) // 被被下架
        ) {
          // 删除笔记文档
          deleteNoteDocument(String.valueOf(noteId));
        }
      }
      default -> log.warn("Unhandled event type for t_note: {}", eventType);
    }
  }

  /**
   * 删除指定ID的文档
   *
   * @param documentId
   */
  private void deleteNoteDocument(String documentId) throws IOException {
    // 创建删除请求对象，指定索引名称和文档ID
    DeleteRequest deleteRequest = new DeleteRequest(NoteIndex.NAME, documentId);
    // 执行删除，将指定文档从es中索引中删除
    restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
  }

  /**
   * 同步笔记索引
   *
   * @param noteId
   * @throws Exception
   */
  private void syncNoteIndex(Long noteId) throws Exception {
    // 从数据库查询Elasticsearch索引数据
    List<Map<String, Object>> result = selectMapper.selectEsNoteIndexData(noteId);

    // 遍历查询结果，将每条记录同步到Elasticsearch索引
    for (Map<String, Object> recordMap : result) {
      // 创建索引请求对象，指定索引名称
      IndexRequest indexRequest = new IndexRequest(NoteIndex.NAME);
      // 设置文档的ID，使用记录中的主键ID字段
      indexRequest.id((String.valueOf(recordMap.get(NoteIndex.FIELD_NOTE_ID))));
      // 设置文档的内容，使用查询结果的记录数据
      indexRequest.source(recordMap);
      // 将数据写入Elasticsearch索引
      restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }
  }

  /**
   * 将列数据解析为Map
   *
   * @param columns
   * @return
   */
  private Map<String, Object> parseColumns2Map(List<CanalEntry.Column> columns) {
    Map<String, Object> map = Maps.newHashMap();
    columns.forEach(
        column -> {
          if (Objects.isNull(column)) {
            return;
          }
          map.put(column.getName(), column.getValue());
        });
    return map;
  }
}
