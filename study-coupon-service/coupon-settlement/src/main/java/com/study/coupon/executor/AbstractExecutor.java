package com.study.coupon.executor;

import com.alibaba.fastjson.JSON;
import com.study.coupon.vo.GoodsInfo;
import com.study.coupon.vo.SettlementInfo;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则执行器抽象类定义通用方法
 */
public abstract class AbstractExecutor {

    /**
     * 校验商品类型与优惠券是否匹配
     * 需要注意:
     * 1.这里实现的是单品类优惠券的校验
     * 2.商品只需要有一个优惠券要求的商品类型去匹配就可以
     * @param settlement
     * @return
     */
    @SuppressWarnings("all")
    protected boolean isGoodsTypeSatisfy(SettlementInfo settlement) {
        List<Integer> goodsType = settlement.getGoodsInfos().stream().map(GoodsInfo::getType).collect(Collectors.toList());
        List<Integer> templateGoodsType = JSON.parseObject(settlement.getCouponAndTemplateInfos().get(0).getTemplate().getRule().getUsage().getGoodsType(), List.class);

        // 存在交集即可
        return CollectionUtils.isNotEmpty(CollectionUtils.intersection(goodsType, templateGoodsType));
    }

    /**
     * 处理商品类型与优惠券限制不匹配的情况
     * @param settlement 用户传递的结算信息
     * @param goodsSum 商品总价
     * @return
     */
    protected SettlementInfo processGoodsTypeNotSatisfy(SettlementInfo settlement, double goodsSum) {
        boolean isGoodsTypeSatify = isGoodsTypeSatisfy(settlement);

        // 当商品类型与优惠券限制不匹配则需要清除settlement中的优惠券数据
        if (!isGoodsTypeSatify) {
            settlement.setCost(goodsSum);
            settlement.setCouponAndTemplateInfos(Collections.emptyList());
            return settlement;
        }

        return null;
    }

    /**
     * 商品总价
     * @param goodsInfos
     * @return
     */
    protected double goodsCostSum(List<GoodsInfo> goodsInfos) {
        return goodsInfos.stream().mapToDouble(g -> g.getPrice() * g.getCount()).sum();
    }

    /**
     * 保留两位小数
     * @param value
     * @return
     */
    protected double retain2Decimals(double value) {
        return new BigDecimal(value).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 最小支付费用
     * @return
     */
    protected double minCost() {
        return 0.1;
    }
}
