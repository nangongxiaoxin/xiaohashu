package com.slilio.xiaohashu.user.biz.rpc;

import com.slilio.framework.common.response.Response;
import com.slilio.xiaohashu.oss.api.FileFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OssRpcService {
  @Resource private FileFeignApi fileFeignApi;

  public String uploadFile(MultipartFile file) {
    // 调用对象存储服务上传文件
    Response<?> response = fileFeignApi.uploadFile(file);

    if (!response.isSuccess()) {
      return null;
    }

    // 返回图片访问链接
    return (String) response.getData();
  }
}
