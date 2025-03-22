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

  public static void main(String[] args) {

    // 初始化 InheritableThreadLocal
    ThreadLocal<Long> threadLocal = new InheritableThreadLocal<>();

    // 假设用户 ID 为 1
    Long userId = 1L;

    // 设置用户 ID 到 InheritableThreadLocal 中
    threadLocal.set(userId);

    System.out.println("主线程打印用户 ID: " + threadLocal.get());

    // 异步线程
    new Thread(
            () -> {
              System.out.println("异步线程打印用户 ID: " + threadLocal.get());
            })
        .start();
  }
}
