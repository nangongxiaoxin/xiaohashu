package com.slilio.xiaohashu.search.biz.service.impl;

import com.slilio.xiaohashu.search.biz.service.ExtDictService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * @Author: slilio @CreateTime: 2025-05-31 @Description: 热更新词典 @Version: 1.0
 */
@Service
@Slf4j
public class ExtDictServiceImpl implements ExtDictService {

  @Value("${elasticsearch.hotUpdateExtDict}")
  private String hotUpdateExtDict;

  /**
   * 获取热更新词典
   *
   * @return
   */
  @Override
  public ResponseEntity<String> getHotUpdateExtDict() {
    try {
      // 获取文件的最后修改时间
      Path path = Paths.get(hotUpdateExtDict);

      long lastModifiedTime = Files.getLastModifiedTime(path).toMillis();

      // 生成ETag（使用文件内容的哈希值）
      String fileContent = Files.lines(path).collect(Collectors.joining("\n"));
      String eTag = String.valueOf(fileContent.hashCode());

      // 设置响应头
      HttpHeaders headers = new HttpHeaders();
      headers.set("ETag", eTag);

      // 设置内容类型为 UTF-8
      headers.setContentType(MediaType.valueOf("text/plain;charset=UTF-8"));

      // 返回文件内容和Http头部
      return ResponseEntity.ok()
          .headers(headers)
          .lastModified(lastModifiedTime) // 请求头中设置 Last-Modified
          .body(fileContent);

    } catch (Exception e) {
      log.error("==> 获取热更新词典异常: ", e);
    }

    return null;
  }
}
