package com.slilio.xiaohashu.auth;

import com.slilio.framework.common.util.JsonUtils;
import com.slilio.xiaohashu.auth.domain.dataobject.UserDO;
import com.slilio.xiaohashu.auth.domain.mapper.UserDOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
@Slf4j
class XiaohashuAuthApplicationTests {

  @Resource
  private UserDOMapper userDOMapper;

  /**
   * 插入测试数据
   */
  @Test
  void testInsert() {
    UserDO userDO = UserDO.builder()
            .username("slilio")
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();
    userDOMapper.insert(userDO);
  }

  /**
   * 查询数据
   */
  @Test
  void testSelect() {
    //查询主键为1的记录
    UserDO userDO = userDOMapper.selectByPrimaryKey(1L);
    log.info("User：{}", JsonUtils.toJsonString(userDO));
  }

  /**
   * 更新数据
   */
  @Test
  void updateSelect() {
    UserDO userDO = UserDO.builder()
            .id(1L)
            .username("slilio")
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();
    userDOMapper.updateByPrimaryKey(userDO);
  }
}
