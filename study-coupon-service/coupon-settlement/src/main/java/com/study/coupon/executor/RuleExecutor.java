package com.study.coupon.executor;

import com.study.coupon.constant.RuleFlag;
import com.study.coupon.vo.SettlementInfo;

/**
 * 优惠券模板规则处理器接口定义
 */
public interface RuleExecutor {

    // 规则类型标记
    RuleFlag ruleConfig();

    // 优惠券规则计算
    SettlementInfo computeRule(SettlementInfo settlement);
}
