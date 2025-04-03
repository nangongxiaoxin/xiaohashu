package com.slilio.xiaohashu.kv.biz;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.kv.biz.domain.dataobject.NoteContentDO;
import com.slilio.xiaohashu.kv.biz.domain.repository.NoteContentRepository;
import jakarta.annotation.Resource;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class CassandraTests {
  @Resource private NoteContentRepository noteContentRepository;

  // 测试插入数据
  @Test
  void testInsert() {
    NoteContentDO noteContentDO =
        NoteContentDO.builder().id(UUID.randomUUID()).content("插入测试").build();

    noteContentRepository.insert(noteContentDO);
  }

  // 测试更新数据
  @Test
  void testUpdate() {
    NoteContentDO noteContentDO =
        NoteContentDO.builder()
            .id(UUID.fromString("9a828a20-128e-453f-b995-22e75b8dfcee"))
            .content("更新测试")
            .build();

    noteContentRepository.save(noteContentDO);
  }

  // 查询数据
  @Test
  void testSelect() {
    Optional<NoteContentDO> optional =
        noteContentRepository.findById(UUID.fromString("9a828a20-128e-453f-b995-22e75b8dfcee"));

    optional.ifPresent(noteContentDO -> log.info("查询结果：{}", JsonUtils.toJsonString(noteContentDO)));
  }

  // 删除数据
  @Test
  void testDelete() {
    noteContentRepository.deleteById(UUID.fromString("9a828a20-128e-453f-b995-22e75b8dfcee"));
  }
}
