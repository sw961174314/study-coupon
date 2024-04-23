package com.study.coupon.executor.impl;

import com.alibaba.fastjson.JSON;
import com.study.coupon.constant.CouponCategory;
import com.study.coupon.constant.RuleFlag;
import com.study.coupon.executor.AbstractExecutor;
import com.study.coupon.executor.RuleExecutor;
import com.study.coupon.vo.GoodsInfo;
import com.study.coupon.vo.SettlementInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 满减折扣优惠券结算规则执行器
 */
@Slf4j
@Component
public class ManJianZheKouExecutor extends AbstractExecutor implements RuleExecutor {

    /**
     * 规则类型标记
     * @return
     */
    @Override
    public RuleFlag ruleConfig() {
        return RuleFlag.MANJIAN_ZHEKOU;
    }

    /**
     * 校验商品类型与优惠券是否匹配
     * 需要注意:
     * 1.这里实现的是满减+折扣优惠券的校验
     * 2.如果想要使用多类优惠券 则必须要所有的商品类型都包含在内(差集为空)
     * @param settlement
     * @return
     */
    @Override
    protected boolean isGoodsTypeSatisfy(SettlementInfo settlement) {
        log.debug("检查满减优惠券和折扣优惠券是否匹配");
        List<Integer> goodsType = settlement.getGoodsInfos().stream().map(GoodsInfo::getType).collect(Collectors.toList());
        // 优惠券模板所支持的商品类型
        List<Integer> templateGoodsType = new ArrayList<>();
        // 为支付信息添加优惠券列表
        settlement.getCouponAndTemplateInfos().forEach(ct -> {
            templateGoodsType.addAll(JSON.parseObject(ct.getTemplate().getRule().getUsage().getGoodsType(), List.class));
        });

        // 如果想要使用多类优惠券 则必须要所有的商品类型都包含在内(差集为空)
        return CollectionUtils.isEmpty(CollectionUtils.subtract(goodsType, templateGoodsType));
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
        // processGoodsTypeNotSatisfy会调用子类重载的isGoodsTypeSatisfy
        SettlementInfo probability = processGoodsTypeNotSatisfy(settlement, goodsSum);
        if (null != probability) {
            log.debug("商品类型没有同时满足满减和折扣优惠券限制");
            return probability;
        }

        SettlementInfo.CouponAndTemplateInfo manJian = null;
        SettlementInfo.CouponAndTemplateInfo zheKou = null;

        for (SettlementInfo.CouponAndTemplateInfo ct : settlement.getCouponAndTemplateInfos()) {
            if (CouponCategory.of(ct.getTemplate().getCategory()) == CouponCategory.MANJIAN) {
                manJian = ct;
            } else {
                zheKou = ct;
            }
        }

        assert null != manJian;
        assert null != zheKou;

        // 当前优惠券和满减券如果不能一起使用 则需要清空优惠券 返回商品原价
        if (!isTemplateCanShared(manJian, zheKou)) {
            log.error("当前的满减优惠券和折扣优惠券不能一起使用");
            // 设置为商品总价
            settlement.setCost(goodsSum);
            // 置空优惠券列表
            settlement.setCouponAndTemplateInfos(Collections.emptyList());
            return settlement;
        }

        // 记录使用满减优惠券和折扣优惠券的列表
        List<SettlementInfo.CouponAndTemplateInfo> ctInfos = new ArrayList<>();

        // 最终价格
        double targetSum = goodsSum;

        // 满减基准
        double manJianBase = (double) manJian.getTemplate().getRule().getDiscount().getBase();
        // 满减额度
        double manJianQuota = (double) manJian.getTemplate().getRule().getDiscount().getQuota();
        if (targetSum >= manJianBase) {
            targetSum -= manJianQuota;
            ctInfos.add(manJian);
        }

        // 折扣额度
        double zheKouQuota = (double) zheKou.getTemplate().getRule().getDiscount().getQuota();
        targetSum *= zheKouQuota * 1.0 / 100;
        ctInfos.add(zheKou);

        settlement.setCouponAndTemplateInfos(ctInfos);
        settlement.setCost(retain2Decimals(targetSum > minCost() ? targetSum : minCost()));
        log.debug("使用满减优惠券和折扣优惠券之后，原始价格:{}，优惠价格:{}", goodsSum, settlement.getCost());
        return settlement;
    }

    /**
     * 去校验当前两张优惠券是否能共用
     * @param manJian
     * @param zheKou
     * @return
     */
    @SuppressWarnings("all")
    private boolean isTemplateCanShared(SettlementInfo.CouponAndTemplateInfo manJian, SettlementInfo.CouponAndTemplateInfo zheKou) {
        String manjianKey = manJian.getTemplate().getKey() + String.format("%04d", manJian.getTemplate().getId());
        String zhekouKey = zheKou.getTemplate().getKey() + String.format("%04d", manJian.getTemplate().getId());

        List<String> allSharedKeysForManjian = new ArrayList<>();
        // 将当前满减券添加到allSharedKeysForManjian
        allSharedKeysForManjian.add(manjianKey);
        allSharedKeysForManjian.addAll(JSON.parseObject(manJian.getTemplate().getRule().getWeight(), List.class));

        List<String> allSharedKeysForZheKou = new ArrayList<>();
        // 将当前折扣添加到allSharedKeysForZheKou
        allSharedKeysForZheKou.add(zhekouKey);
        allSharedKeysForZheKou.addAll(JSON.parseObject(zheKou.getTemplate().getRule().getWeight(), List.class));

        // Arrays.asList(manjianKey, zhekouKey) 将manjianKey和zhekouKey放置到一个列表里面
        // CollectionUtils.isSubCollection(Arrays.asList(manjianKey, zhekouKey), allSharedKeysForManjian) 判断allSharedKeysForManjian是否有manjianKey和zhekouKey 有则可以一起使用
        // CollectionUtils.isSubCollection(Arrays.asList(manjianKey, zhekouKey), allSharedKeysForZheKou) 判断allSharedKeysForZheKou是否有manjianKey和zhekouKey 有则可以一起使用
        return CollectionUtils.isSubCollection(Arrays.asList(manjianKey, zhekouKey), allSharedKeysForManjian) || CollectionUtils.isSubCollection(Arrays.asList(manjianKey, zhekouKey), allSharedKeysForZheKou);
    }
}
