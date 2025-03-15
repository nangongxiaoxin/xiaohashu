package com.slilio.xiaohashu.auth.alarm.impl;

import com.slilio.xiaohashu.auth.alarm.AlarmInterface;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmsAlarmHelper implements AlarmInterface {
  /**
   * 发送警告信息
   *
   * @param message
   * @return
   */
  @Override
  public boolean send(String message) {
    log.info("==> 【短信告警】：{}", message);
    // todo 业务逻辑
    return false;
  }
}
