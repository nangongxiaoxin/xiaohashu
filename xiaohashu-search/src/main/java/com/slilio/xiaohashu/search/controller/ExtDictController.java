package com.slilio.xiaohashu.search.controller;

import com.slilio.framework.biz.operationlog.aspect.ApiOperationLog;
import com.slilio.xiaohashu.search.service.ExtDictService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: slilio @CreateTime: 2025-05-31 @Description: 词典热更新 @Version: 1.0
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class ExtDictController {
  @Resource private ExtDictService extDictService;

  @GetMapping("/ext/dict")
  @ApiOperationLog(description = "热更新词典")
  public ResponseEntity<String> extDict() {
    return extDictService.getHotUpdateExtDict();
  }
}
