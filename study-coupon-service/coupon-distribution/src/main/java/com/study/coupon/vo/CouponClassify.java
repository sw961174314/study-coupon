package com.study.coupon.vo;

import com.study.coupon.constant.CouponStatus;
import com.study.coupon.constant.PeriodType;
import com.study.coupon.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.time.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 根据优惠券状态对用户优惠券进行分类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponClassify {

    // 可使用的优惠券
    private List<Coupon> usable;

    // 已使用的优惠券
    private List<Coupon> used;

    // 已过期的优惠券
    private List<Coupon> expired;

    /**
     * 对当前优惠券进行分类
     * @param coupons
     * @return
     */
    public static CouponClassify classify(List<Coupon> coupons) {
        List<Coupon> usable = new ArrayList<>(coupons.size());
        List<Coupon> used = new ArrayList<>(coupons.size());
        List<Coupon> expired = new ArrayList<>(coupons.size());

        coupons.forEach(c -> {
            // 判断优惠券是否过期
            boolean isTimeExpire;
            // 当前时间
            long curTime = new Date().getTime();

            // 判断当前优惠券的有效期类型
            if (c.getTemplateSDK().getRule().getExpiration().getPeriod().equals(PeriodType.REGULAR.getCode())) {
                // 固定的(固定日期)
                isTimeExpire = c.getTemplateSDK().getRule().getExpiration().getDeadline() <= curTime;
            } else {
                // 变动的(以领取之日开始计算)
                isTimeExpire = DateUtils.addDays(c.getAssignTime(), c.getTemplateSDK().getRule().getExpiration().getGap()).getTime() <= curTime;
            }

            // 判断优惠券状态并添加到对应列表中
            if (c.getStatus() == CouponStatus.USED) {
                used.add(c);
            } else if (c.getStatus() == CouponStatus.EXPIRED || isTimeExpire) {
                expired.add(c);
            } else {
                usable.add(c);
            }
        });

        return new CouponClassify(usable, used, expired);
    }
}
