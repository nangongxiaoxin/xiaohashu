package com.slilio.xiaohashu.count.biz.constant;

/**
 * @Author: slilio @CreateTime: 2025-04-29 @Description: MQ常量类 @Version: 1.0
 */
public interface MQConstants {
  String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic"; // topic：关注计数

  String TOPIC_COUNT_FANS = "CountFansTopic"; // topic:粉丝计数

  String TOPIC_COUNT_FANS_2_DB = "CountFans2DBTopic"; // topic：粉丝数计数入库

  String TOPIC_COUNT_FOLLOWING_2_DB = "CountFollowing2DBTopic"; // topic：关注数计数入库
}
