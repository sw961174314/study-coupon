package com.study.coupon.feign.hystrix;

import com.study.coupon.exception.CouponException;
import com.study.coupon.feign.SettlementClient;
import com.study.coupon.vo.CommonResponse;
import com.study.coupon.vo.SettlementInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 结算微服务熔断策略实现
 */
@Slf4j
@Component
public class SettlementClientHystrix implements SettlementClient {

    /**
     * 优惠券规则计算
     * @param settlement
     * @return
     * @throws CouponException
     */
    @Override
    public CommonResponse<SettlementInfo> computeRule(SettlementInfo settlement) throws CouponException {
        log.error("[eureka-client-coupon-settlement]computeRule当前服务不可用");
        settlement.setEmploy(false);
        settlement.setCost(-1.0);
        return new CommonResponse<>(-1, "[eureka-client-coupon-settlement]当前服务不可用", settlement);
    }
}
