package com.study.coupon.executor.impl;

import com.study.coupon.constant.RuleFlag;
import com.study.coupon.executor.AbstractExecutor;
import com.study.coupon.executor.RuleExecutor;
import com.study.coupon.vo.CouponTemplateSDK;
import com.study.coupon.vo.SettlementInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 立减优惠券结算规则执行器
 */
@Slf4j
@Component
public class LiJianExecutor extends AbstractExecutor implements RuleExecutor {

    /**
     * 规则类型标记
     * @return
     */
    @Override
    public RuleFlag ruleConfig() {
        return RuleFlag.LIJIAN;
    }

    /**
     * 优惠券规则的计算
     * @param settlement
     * @return
     */
    @Override
    public SettlementInfo computeRule(SettlementInfo settlement) {
        // 商品总价
        double goodsSum = retain2Decimals(goodsCostSum(settlement.getGoodsInfos()));

        // 处理商品类型与优惠券限制不匹配的情况
        SettlementInfo probability = processGoodsTypeNotSatisfy(settlement, goodsSum);
        if (null != probability) {
            log.debug("商品类型不符合立减优惠券的限制");
            return probability;
        }

        // 立减优惠券直接使用 没有门槛
        CouponTemplateSDK templateSDK = settlement.getCouponAndTemplateInfos().get(0).getTemplate();
        // 额度 如减10 10就是满减额度
        double quota = (double) templateSDK.getRule().getDiscount().getQuota();

        // 计算使用优惠券之后的价格
        settlement.setCost(retain2Decimals(goodsSum - quota) > minCost() ? retain2Decimals(goodsSum - quota) : minCost());
        log.debug("使用立减优惠券之后，原始价格:{}，优惠价格:{}", goodsSum, settlement.getCost());
        return settlement;
    }
}
