package com.slilio.xiaohashu.user.relation.biz.constant;

/**
 * @Author: slilio @CreateTime: 2025-04-21 @Description: MQ常量类 @Version: 1.0
 */
public interface MQConstants {
  String TOPIC_FOLLOW_OR_UNFOLLOW = "FollowUnfollowTopic"; // topic ：关注、取关公用一个
  String TAG_FOLLOW = "Follow"; // tag：关注
  String TAG_UNFOLLOW = "Unfollow"; // tag：取关
}
