package com.study.coupon.controller;

import com.alibaba.fastjson.JSON;
import com.study.coupon.entity.CouponTemplate;
import com.study.coupon.exception.CouponException;
import com.study.coupon.service.IBuildTemplateService;
import com.study.coupon.service.ITemplateBaseService;
import com.study.coupon.vo.CouponTemplateSDK;
import com.study.coupon.vo.TemplateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 优惠券模板相关的功能控制器
 */
@Slf4j
@RestController
public class CouponTemplateController {

    // 构建优惠券模板服务
    @Autowired
    private IBuildTemplateService buildTemplateService;

    // 优惠券模板基础服务
    @Autowired
    private ITemplateBaseService templateBaseService;

    /**
     * 构建优惠券模板
     * 127.0.0.1:7001/coupon-template/template/build
     * 127.0.0.1:9000/study/coupon-template/template/build
     * @param request
     * @return
     * @throws CouponException
     */
    @PostMapping("/template/build")
    public CouponTemplate buildTemplate(@RequestBody TemplateRequest request) throws CouponException {
        log.info("优惠券模板信息:{}", JSON.toJSONString(request));
        return buildTemplateService.buildTemplate(request);
    }

    /**
     * 构建优惠券模板详情
     * 127.0.0.1:7001/coupon-template/template/info
     * @param id
     * @return
     * @throws CouponException
     */
    @GetMapping("/template/info")
    public CouponTemplate buildTemplateInfo(@RequestParam("id") Integer id) throws CouponException {
        log.info("优惠券模板id:{}", id);
        return templateBaseService.buildTemplateInfo(id);
    }

    /**
     * 查找所有可用的优惠券模板
     * 127.0.0.1:7001/coupon-template/template/sdk/all
     * @return
     */
    @GetMapping("/template/sdk/all")
    public List<CouponTemplateSDK> findAllUsableTemplate() {
        log.info("查找所有可用的优惠券模板");
        return templateBaseService.findAllUsableTemplate();
    }

    /**
     * 获取模板ids到CouponTemplateSDK的映射
     * 127.0.0.1:7001/coupon-template/template/sdk/infos
     * @param ids
     * @return
     */
    @GetMapping("/templage/sdk/infos")
    public Map<Integer, CouponTemplateSDK> findIds2TemplateSDK(@RequestParam("ids") Collection<Integer> ids) {
        log.info("findIds2TemplateSDK:{}", JSON.toJSONString(ids));
        return templateBaseService.findIds2TemplateSDK(ids);
    }
}
