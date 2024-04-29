package com.study.coupon.controller;

import com.alibaba.fastjson.JSON;
import com.study.coupon.exception.CouponException;
import com.study.coupon.executor.ExecuteManager;
import com.study.coupon.vo.SettlementInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结算服务Controller
 */
@Slf4j
@RestController
public class SettlementController {

    // 结算规则执行管理器
    @Autowired
    private ExecuteManager executeManager;

    /**
     * 优惠券结算
     * @param settlement
     * @return
     * @throws CouponException
     */
    @PostMapping("/settlement/compute")
    public SettlementInfo computeRule(@RequestBody SettlementInfo settlement) throws CouponException {
        log.info("settlement:{}", JSON.toJSONString(settlement));
        return executeManager.computeRule(settlement);
    }
}
