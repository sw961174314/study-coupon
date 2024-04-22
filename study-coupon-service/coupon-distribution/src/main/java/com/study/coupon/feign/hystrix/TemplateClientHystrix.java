package com.study.coupon.feign.hystrix;

import com.study.coupon.feign.TemplateClient;
import com.study.coupon.vo.CommonResponse;
import com.study.coupon.vo.CouponTemplateSDK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 优惠券模板Feign接口的熔断降级策略
 */
@Slf4j
@Component
public class TemplateClientHystrix implements TemplateClient {

    /**
     * 查找所有可用的优惠券模板
     * @return
     */
    @Override
    public CommonResponse<List<CouponTemplateSDK>> findAllUsableTemplate() {
        log.error("[eureka-client-coupon-template]findAllUsableTemplate当前服务不可用");
        return new CommonResponse<>(-1, "[eureka-client-coupon-template]当前服务不可用", Collections.emptyList());
    }

    /**
     * 获取模板ids到CouponTemplateSDK的映射
     * @param ids
     * @return
     */
    @Override
    public CommonResponse<Map<Integer, CouponTemplateSDK>> findIds2TemplateSDK(Collection<Integer> ids) {
        log.error("[eureka-client-coupon-template]findIds2Template当前服务不可用");
        return new CommonResponse<>(-1, "[eureka-client-coupon-template]当前服务不可用", new HashMap<>());
    }
}
