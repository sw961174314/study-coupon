package com.study.coupon.converter;

import com.study.coupon.constant.CouponCategory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * 优惠券分类枚举属性转换器
 * AttributeConverter<X, Y>
 * X:实体属性的类型
 * Y:数据库字段的类型
 */
@Converter
public class CouponCategoryConverter implements AttributeConverter<CouponCategory, String> {

    /**
     * 将实体属性X转换为Y存储到数据库中(插入和更新时执行的动作)
     * @param couponCategory
     * @return
     */
    @Override
    public String convertToDatabaseColumn(CouponCategory couponCategory) {
        return couponCategory.getCode();
    }

    /**
     * 将数据库中的字段Y转换为实体属性X(查询操作时执行的动作)
     * @param code
     * @return
     */
    @Override
    public CouponCategory convertToEntityAttribute(String code) {
        return CouponCategory.of(code);
    }
}
