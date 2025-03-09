package com.slilio.xiaohashu.auth;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootTest
@Slf4j
public class ThreadPoolTaskExecutorTests {

  @Resource private ThreadPoolTaskExecutor threadPoolTaskExecutor;

  @Test
  void testSubmit() {
    threadPoolTaskExecutor.submit(
        () -> {
          log.info("异步线程：￥￥￥");
        });
  }

  @Test
  void testTaskExecutor() {
    for (int i = 0; i < 20; i++) {
      testSubmit();
    }
  }
}
