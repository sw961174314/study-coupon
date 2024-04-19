package com.study.coupon.service;

import com.study.coupon.entity.CouponTemplate;
import com.study.coupon.exception.CouponException;
import com.study.coupon.vo.CouponTemplateSDK;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 优惠券模板基础服务定义
 */
public interface ITemplateBaseService {

    /**
     * 根据优惠券模板id获取优惠券模板信息
     * @param id 模板id
     * @return {@link CouponTemplate} 优惠券模板实体
     * @throws CouponException
     */
    CouponTemplate buildTemplateInfo(Integer id) throws CouponException;

    /**
     * 查找所有可用的优惠券模板
     * @return {@link CouponTemplateSDK}
     */
    List<CouponTemplateSDK> findAllUsableTemplate();

    /**
     * 获取模板ids到CouponTemplateSDK的映射
     * @param ids 模板ids
     * @return Map<key:模板id,value:CouponTemplateSDK>
     */
    Map<Integer, CouponTemplateSDK> findIds2TemplateSDK(Collection<Integer> ids);
}
