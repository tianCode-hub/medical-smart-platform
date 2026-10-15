package com.medical.appointment.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 限流 / 熔断 / 降级配置（本地规则初始化）。
 * 说明：使用本地规则即可生效，无需依赖 Sentinel Dashboard；Dashboard 仅作观察面板，
 * 未启动时不影响本地规则起作用。
 * - 对 createAppointment（预约下单）做 QPS 限流 + 慢调用/异常比例熔断，避免高峰期打爆预约服务。
 * - 对 getDoctorTodayStat 做轻量 QPS 限流。
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void init() {
        // 限流规则
        List<FlowRule> flowRules = new ArrayList<>();

        FlowRule createFlow = new FlowRule("createAppointment");
        createFlow.setGrade(RuleConstant.FLOW_GRADE_QPS);
        createFlow.setCount(50);   // 预约下单 QPS 上限 50，超出触发 BLOCK（blockHandler）
        createFlow.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        flowRules.add(createFlow);

        FlowRule statFlow = new FlowRule("getDoctorTodayStat");
        statFlow.setGrade(RuleConstant.FLOW_GRADE_QPS);
        statFlow.setCount(200);
        flowRules.add(statFlow);

        FlowRuleManager.loadRules(flowRules);

        // 熔断降级规则
        List<DegradeRule> degradeRules = new ArrayList<>();

        DegradeRule createDegrade = new DegradeRule("createAppointment");
        // 异常比例熔断：最近窗口内异常比例超过 30% 即熔断
        createDegrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        createDegrade.setCount(0.3);
        createDegrade.setTimeWindow(10); // 熔断 10 秒，快速失败
        createDegrade.setMinRequestAmount(5);
        createDegrade.setStatIntervalMs(1000);
        degradeRules.add(createDegrade);

        DegradeRuleManager.loadRules(degradeRules);

        log.info(">>> Sentinel 本地规则已加载：createAppointment QPS=50 / 异常比例熔断；getDoctorTodayStat QPS=200");
    }
}