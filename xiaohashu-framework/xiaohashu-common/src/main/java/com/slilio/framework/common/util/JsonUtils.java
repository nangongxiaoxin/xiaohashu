package com.slilio.framework.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

public class JsonUtils {

  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    OBJECT_MAPPER.registerModules(new JavaTimeModule()); // 解决 LocalDateTime 的序列化问题
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
   * 将对象转换为 JSON 字符串
   *
   * @param obj
   * @return
   */
  @SneakyThrows
  public static String toJsonString(Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

  @SneakyThrows
  public static <T> T parseObject(String jsonStr, Class<T> clazz) {
    if (StringUtils.isBlank(jsonStr)) {
      return null;
    }

    return OBJECT_MAPPER.readValue(jsonStr, clazz);
  }

  /**
   * 将Json字符串转换为Map
   *
   * @param jsonStr
   * @param keyClass
   * @param valueClass
   * @return
   * @param <K>
   * @param <V>
   * @throws Exception
   */
  public static <K, V> Map<K, V> parseMap(String jsonStr, Class<K> keyClass, Class<V> valueClass)
      throws Exception {
    // 创建 TypeReference，指定泛型类型
    TypeReference<Map<K, V>> typeRef = new TypeReference<Map<K, V>>() {};

    // 将 JSON 字符串转换为 Map
    return OBJECT_MAPPER.readValue(
        jsonStr, OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
  }

  /**
   * 将json字符串解析为指定类型的List对象
   *
   * @param jsonStr
   * @param clazz
   * @return
   * @param <T>
   * @throws Exception
   */
  public static <T> List<T> parseList(String jsonStr, Class<T> clazz) throws Exception {
    // 使用TypeReference指定List<T> 的泛型类型
    return OBJECT_MAPPER.readValue(
        jsonStr,
        new TypeReference<List<T>>() {
          @Override
          public Type getType() {
            return OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
          }
        });
  }

  public static <T> Set<T> parseSet(String jsonStr, Class<T> clazz) throws Exception {
    // 使用TypeReference指定的Set<T>的泛型类型
    return OBJECT_MAPPER.readValue(
        jsonStr,
        new TypeReference<>() {
          @Override
          public CollectionType getType() {
            return OBJECT_MAPPER.getTypeFactory().constructCollectionType(Set.class, clazz);
          }
        });
  }
}
