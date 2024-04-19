package com.study.coupon.controller;

import com.study.coupon.exception.CouponException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康检查接口
 */
@Slf4j
@RestController
public class HealthCheck {

    // 服务发现客户端 DiscoveryClient是Spring Cloud Netflix Eureka客户端的核心接口 用于与Eureka服务注册中心进行交互 通过DiscoveryClient可以获取服务实例列表、注册服务实例等操作
    @Autowired
    private DiscoveryClient client;

    // 服务注册接口 提供获取服务id的方法 Registration是Spring Cloud Netflix Eureka客户端的核心接口 用于向Eureka服务注册中心注册服务实例 通过Registration可以获取服务实例的元数据信息、取消服务实例的注册等操作
    @Autowired
    private Registration registration;

    /**
     * 健康检查接口
     * 127.0.0.1:7001/coupon-template/health
     * @return
     */
    @GetMapping("/health")
    public String health() {
        log.debug("健康检查接口");
        return "优惠券模板正常";
    }

    /**
     * 异常测试接口
     * 127.0.0.1:7001/coupon-template/exception
     * @return
     * @throws CouponException
     */
    @GetMapping("/exception")
    public String exception() throws CouponException {
        log.debug("异常测试接口");
        throw new CouponException("优惠券模板有问题");
    }

    /**
     * 获取Eureka Server上的微服务元信息
     * 127.0.0.1:7001/coupon-template/info
     * @return
     */
    @GetMapping("/info")
    public List<Map<String, Object>> info() {
        // 预计需要等待2min才能获取到注册信息
        List<ServiceInstance> instances = client.getInstances(registration.getServiceId());

        List<Map<String, Object>> result = new ArrayList<>(instances.size());
        instances.forEach(i -> {
            Map<String, Object> info = new HashMap<>();
            info.put("serviceId", i.getServiceId());
            info.put("instanceId", i.getInstanceId());
            info.put("port", i.getPort());
            result.add(info);
        });

        return result;
    }
}
