package com.study.coupon.service.impl;

import com.study.coupon.dao.CouponTemplateDao;
import com.study.coupon.entity.CouponTemplate;
import com.study.coupon.exception.CouponException;
import com.study.coupon.service.ITemplateBaseService;
import com.study.coupon.vo.CouponTemplateSDK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券模板基础服务接口实现
 */
@Slf4j
@Service
public class TemplateBaseServiceImpl implements ITemplateBaseService {

    // CouponTemplateDao
    @Autowired
    private CouponTemplateDao templateDao;

    /**
     * 根据优惠券模板id获取优惠券模板信息
     * @param id 模板id
     * @return {@link CouponTemplate} 优惠券模板实体
     * @throws CouponException
     */
    @Override
    public CouponTemplate buildTemplateInfo(Integer id) throws CouponException {
        Optional<CouponTemplate> template = templateDao.findById(id);
        // 判断该优惠券模板信息
        if (!template.isPresent()) {
            throw new CouponException("该优惠券模板信息为空:" + id);
        }
        return template.get();
    }

    /**
     * 查找所有可用的优惠券模板
     * @return {@link CouponTemplateSDK}
     */
    @Override
    public List<CouponTemplateSDK> findAllUsableTemplate() {
        List<CouponTemplate> templates = templateDao.findAllByAvailableAndExpired(true, false);
        return templates.stream().map(this::template2TemplateSDK).collect(Collectors.toList());
    }

    /**
     * 获取模板ids到CouponTemplateSDK的映射
     * @param ids 模板ids
     * @return Map<key:模板id,value:CouponTemplateSDK>
     */
    @Override
    public Map<Integer, CouponTemplateSDK> findIds2TemplateSDK(Collection<Integer> ids) {
        List<CouponTemplate> templates = templateDao.findAllById(ids);
        return templates.stream().map(this::template2TemplateSDK).collect(Collectors.toMap(CouponTemplateSDK::getId, Function.identity()));
    }

    /**
     * 将CouponTemplate转换为CouponTemplateSDK
     * @param template
     * @return
     */
    private CouponTemplateSDK template2TemplateSDK(CouponTemplate template) {
        return new CouponTemplateSDK(template.getId(), template.getName(), template.getLogo(), template.getDesc(), template.getCategory().getCode(), template.getProductLine().getCode(), template.getKey(), template.getTarget().getCode(), template.getRule());
    }
}
