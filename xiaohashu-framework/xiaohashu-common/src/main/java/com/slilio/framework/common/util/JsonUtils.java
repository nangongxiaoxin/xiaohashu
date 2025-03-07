package com.slilio.framework.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;

public class JsonUtils {
  // 创建了一个私有的静态不可变的 ObjectMapper 实例
  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * static { ... }: 这是一个静态初始化块，用于在类加载时执行一些初始化操作。 在这里，OBJECT_MAPPER 被配置以在反序列化时忽略未知属性和在序列化时忽略空的 Java
   * Bean 属性，并且注册了一个 JavaTimeModule 模块，用于解决 LocalDateTime 类型的序列化问题。
   */
  static {
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    OBJECT_MAPPER.registerModules(new JavaTimeModule()); // 解决LocalDateTime的序列化问题
  }

  /**
   * 初始化：统一使用 Spring Boot 个性化配置的 ObjectMapper
   *
   * @param objectMapper
   */
  public static void init(ObjectMapper objectMapper) {
    OBJECT_MAPPER = objectMapper;
  }

  /**
   * 将对象转换为json字符串
   *
   * @param obj
   * @return
   */
  @SneakyThrows
  public static String toJsonString(Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }
}
