package com.slilio.xiaohashu.count.biz;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import java.util.ArrayList;
import java.util.List;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author: slilio @CreateTime: 2025-04-28 @Description: 启动类 @Version: 1.0
 */
@SpringBootApplication
@MapperScan("com.slilio.xiaohashu.count.biz.domain.mapper")
public class XiaohashuCountBizApplication {
  public static void main(String[] args) {
    SpringApplication.run(XiaohashuCountBizApplication.class, args);

    // 初始化流量控制规则
    //    initFlowRules(); //使用平台规则下发
  }

  // 初始化流量控制规则 已经采用nacos云端下发
  private static void initFlowRules() {
    // 创建集合，存放所有限流规则
    List<FlowRule> rules = new ArrayList<>();

    // 创建一条规则
    FlowRule rule = new FlowRule();

    // 设置需要保护的资源名称（注解的限流方法名）
    rule.setResource("findUserCountData");

    // 设置每秒限流阈值类型为QPS（每秒查询数）
    rule.setGrade(RuleConstant.FLOW_GRADE_QPS);

    // 设置QPS阈值
    rule.setCount(5);

    // 添加到规则中
    rules.add(rule);

    // 加载规则列表到Sentinel的规则管理器
    FlowRuleManager.loadRules(rules);
  }
}
