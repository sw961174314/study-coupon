package com.study.coupon.dao;

import com.study.coupon.entity.CouponTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * CouponTemplate Dao 接口定义
 */
public interface CouponTemplateDao extends JpaRepository<CouponTemplate, Integer> {

    // 根据模板名称查询模板
    CouponTemplate findByName(String name);

    // 根据available和expired标记查找模板记录
    List<CouponTemplate> findAllByAvailableAndExpired(Boolean available, Boolean expired);

    // 根据expired标记查找模板记录
    List<CouponTemplate> findAllByExpired(Boolean expired);
}
