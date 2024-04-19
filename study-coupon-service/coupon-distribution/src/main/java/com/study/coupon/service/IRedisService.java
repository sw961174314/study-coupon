package com.study.coupon.service;

import com.study.coupon.entity.Coupon;
import com.study.coupon.exception.CouponException;

import java.util.List;

/**
 * Redis相关的操作服务接口定义
 * 1.用户的三个状态优惠券Cache相关操作
 * 2.优惠券模板生成的优惠券码Cache操作
 */
public interface IRedisService {

    // 根据userId和status找到缓存的优惠券列表数据
    List<Coupon> getCachedCoupons(Long userId, Integer status);

    // 保存空的优惠券列表到缓存中
    void saveEmptyCouponListToCache(Long userId, List<Integer> status);

    // 尝试从Cache中获取一个优惠券码
    String tryToAcquireCouponCodeFromCache(Integer templateId);

    // 将优惠券保存到Cache中
    Integer addCouponToCache(Long userId, List<Coupon> coupons, Integer status) throws CouponException;
}
