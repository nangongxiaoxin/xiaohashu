package com.slilio.xiaohashu.search.biz.service;

import org.springframework.http.ResponseEntity;

/**
 * @Author: slilio @CreateTime: 2025-05-31 @Description: 热更新词典 @Version: 1.0
 */
public interface ExtDictService {

  /**
   * 获取热更新词典
   *
   * @return
   */
  ResponseEntity<String> getHotUpdateExtDict();
}
