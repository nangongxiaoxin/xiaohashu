package com.slilio.xiaohashu.oss.api;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.oss.constant.ApiConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface FileFeignApi {
  String PREFIX = "/file";

  /**
   * 文件上传
   *
   * @param file
   * @return
   */
  @PostMapping(value = PREFIX + "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  Response<?> uploadFile(@RequestPart(value = "file") MultipartFile file);
}
