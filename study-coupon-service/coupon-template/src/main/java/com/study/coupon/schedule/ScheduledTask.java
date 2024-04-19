package com.study.coupon.schedule;

import com.study.coupon.dao.CouponTemplateDao;
import com.study.coupon.entity.CouponTemplate;
import com.study.coupon.vo.TemplateRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 定时清理已过期的优惠券模板
 */
@Slf4j
@Component
public class ScheduledTask {

    @Autowired
    private CouponTemplateDao templateDao;

    /**
     * 下线已过期的优惠券模板
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void offlineCouponTemplate() {
        log.info("开始查找已过期的优惠券模板");
        List<CouponTemplate> templates = templateDao.findAllByExpired(false);
        if (CollectionUtils.isEmpty(templates)) {
            log.info("结束查找已过期的优惠券模板");
            return;
        }

        Date cur = new Date();
        List<CouponTemplate> expiredTemplates = new ArrayList<>(templates.size());
        templates.forEach(t -> {
            // 根据优惠券模板规则中的"过期规则"来校验模板是否过期
            TemplateRule rule = t.getRule();
            if (rule.getExpiration().getDeadline() < cur.getTime()) {
                t.setExpired(true);
                // 将已过期的优惠券模板放入expiredTemplates中
                expiredTemplates.add(t);
            }
        });

        if (CollectionUtils.isNotEmpty(expiredTemplates)) {
            log.info("已过期的优惠券模板编号为:{}", templateDao.saveAll(expiredTemplates));
        }
        log.info("结束查找已过期的优惠券模板");
    }
}
