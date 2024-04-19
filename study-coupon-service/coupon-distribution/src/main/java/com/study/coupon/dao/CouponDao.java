package com.study.coupon.dao;

import com.study.coupon.constant.CouponStatus;
import com.study.coupon.entity.Coupon;
import com.study.coupon.exception.CouponException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Coupon Dao接口定义
 */
public interface CouponDao extends JpaRepository<Coupon, Integer> {

    // 根据userId和status找到缓存的优惠券列表数据
    List<Coupon> findAllByUserIdAndStatus(Long userId, CouponStatus status);

    // 保存空的优惠券列表到缓存中
    void saveEmptyCouponListToCache(Long userId, List<Integer> status);

    // 尝试从Cache中获取一个优惠券码
    String tryToAcquireCouponCodeFromCache(Integer templateId);

    // 将优惠券保存到Cache中
    Integer addCouponToCache(Long userId, List<Coupon> coupons, Integer status) throws CouponException;
}
