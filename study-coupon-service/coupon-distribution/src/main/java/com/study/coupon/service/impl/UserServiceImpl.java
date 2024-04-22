package com.study.coupon.service.impl;

import com.alibaba.fastjson.JSON;
import com.study.coupon.constant.Constant;
import com.study.coupon.constant.CouponStatus;
import com.study.coupon.dao.CouponDao;
import com.study.coupon.entity.Coupon;
import com.study.coupon.exception.CouponException;
import com.study.coupon.feign.SettlementClient;
import com.study.coupon.feign.TemplateClient;
import com.study.coupon.service.IRedisService;
import com.study.coupon.service.IUserService;
import com.study.coupon.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务相关的接口实现
 * 所有的操作过程和状态都保存在Redis中 并通过Kafka把消息传递到MySQL中
 */
@Slf4j
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private CouponDao couponDao;

    @Autowired
    private IRedisService redisService;

    // 模板微服务客户端
    @Autowired
    private TemplateClient templateClient;

    // 结算微服务客户端
    @Autowired
    private SettlementClient settlementClient;

    // Kafka客户端
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 根据用户id和状态查询优惠券记录
     * @param userId
     * @param status
     * @return
     */
    @Override
    public List<Coupon> findCouponsByStatus(Long userId, Integer status) throws CouponException {
        // 缓存中的优惠券数据
        List<Coupon> curCached = redisService.getCachedCoupons(userId, status);
        List<Coupon> preTarget;
        if (CollectionUtils.isNotEmpty(curCached)) {
            log.debug("缓存中的优惠券数据不为空:{},{}", userId, status);
            preTarget = curCached;
        } else {
            log.debug("缓存中没有响应的优惠券数据，从数据库中获取优惠券数据:{},{}", userId, status);
            List<Coupon> dbCoupons = couponDao.findAllByUserIdAndStatus(userId, CouponStatus.of(status));
            // 如果数据库中没有记录则直接返回 Cache中已加入了一个无效的优惠券数据
            if (CollectionUtils.isEmpty(dbCoupons)) {
                log.debug("当前用户没有优惠券:{},{}", userId, status);
                return dbCoupons;
            }

            // 填充dbCoupons的templateSDK字段
            Map<Integer, CouponTemplateSDK> id2TemplateSDK = templateClient.findIds2TemplateSDK(dbCoupons.stream().map(Coupon::getTemplateId).collect(Collectors.toList())).getData();
            dbCoupons.forEach(dc -> dc.setTemplateSDK(id2TemplateSDK.get(dc.getTemplateId())));
            preTarget = dbCoupons;
            // 将记录写入Cache
            redisService.addCouponToCache(userId, preTarget, status);
        }

        // 将无效优惠券剔除
        preTarget = preTarget.stream().filter(c -> c.getId() != -1).collect(Collectors.toList());
        // 如果当前获取的是可用优惠券 还需要做对已过期优惠券的延迟处理
        if (CouponStatus.of(status) == CouponStatus.USABLE) {
            CouponClassify classify = CouponClassify.classify(preTarget);
            // 如果已过期状态不为空 需要做延迟处理
            if (CollectionUtils.isNotEmpty(classify.getExpired())) {
                log.info("将findCouponsByStatus的已过期优惠券放置到缓存中:{},{}", userId, status);
                redisService.addCouponToCache(userId, classify.getExpired(), CouponStatus.EXPIRED.getCode());
                // 发送到Kafka中作异步处理(KafkaServiceImpl中的consumeCouponKafkaMessage会进行处理)
                kafkaTemplate.send(Constant.TOPIC, JSON.toJSONString(new CouponKafkaMessage(CouponStatus.EXPIRED.getCode(), classify.getExpired().stream().map(Coupon::getId).collect(Collectors.toList()))));
            }
            return classify.getUsable();
        }
        return preTarget;
    }

    /**
     * 根据用户id查找当前可以领取的优惠券模板
     * @param userId
     * @return
     */
    @Override
    public List<CouponTemplateSDK> findAvailableTemplate(Long userId) throws CouponException{
        long curTime = new Date().getTime();
        List<CouponTemplateSDK> templateSDKS = templateClient.findAllUsableTemplate().getData();
        log.debug("从优惠券客户端中查找所有优惠券模板数量:{}", templateSDKS.size());

        // 过滤过期的优惠券模板
        templateSDKS = templateSDKS.stream().filter(t -> t.getRule().getExpiration().getDeadline() > curTime).collect(Collectors.toList());
        log.info("可用优惠券模板数量:{}", templateSDKS.size());

        // key:templateId value中的key:template的limitation value中的value:优惠券模板
        Map<Integer, Pair<Integer, CouponTemplateSDK>> limit2Template = new HashMap<>(templateSDKS.size());
        templateSDKS.forEach(t -> limit2Template.put(t.getId(), Pair.of(t.getRule().getLimitation(), t)));

        List<CouponTemplateSDK> result = new ArrayList<>(limit2Template.size());
        // 根据用户id和状态查询优惠券记录
        List<Coupon> userUsableCoupons = findCouponsByStatus(userId, CouponStatus.USABLE.getCode());

        log.debug("当前用户拥有的可用优惠券数量:{},{}", userId, userUsableCoupons.size());
        // 将用户优惠券信息转换成map key:templateId
        Map<Integer, List<Coupon>> templateId2Coupons = userUsableCoupons.stream().collect(Collectors.groupingBy(Coupon::getTemplateId));

        // 根据template的rule来判断是否可以领取优惠券模板
        limit2Template.forEach((k,v) ->{
            int limitation = v.getLeft();
            CouponTemplateSDK templateSDK = v.getRight();

            // 如果当前用户已领取了该优惠券并且领取次数大于或等于限制次数则直接返回
            if (templateId2Coupons.containsKey(k) && templateId2Coupons.get(k).size() >= limitation) {
                return;
            }

            result.add(templateSDK);
        });
        return result;
    }

    /**
     * 用户领取优惠券
     * 1.从TemplateClient拿到对应的优惠券并检查是否过期
     * 2.根据limitation判断用户是否可以领取
     * 3.保存到数据库
     * 4.填充CouponTemplateSDK
     * 5.保存到缓存
     * @param request
     * @return
     * @throws CouponException
     */
    @Override
    public Coupon acquireTemplate(AcquireTemplateRequest request) throws CouponException {
        Map<Integer, CouponTemplateSDK> id2Template = templateClient.findIds2TemplateSDK(Collections.singletonList(request.getTemplateSDK().getId())).getData();

        // 优惠券模板是需要存在的
        if (id2Template.size() <= 0) {
            log.error("无法从模板服务端中获取优惠券模板:{}", request.getTemplateSDK().getId());
            throw new CouponException("无法从模板服务端中获取优惠券模板");
        }

        // 判断用户是否可以领取该优惠券
        List<Coupon> userUsableCoupons = findCouponsByStatus(request.getUserId(), CouponStatus.USABLE.getCode());
        Map<Integer, List<Coupon>> templateId2Coupons = userUsableCoupons.stream().collect(Collectors.groupingBy(Coupon::getTemplateId));
        // 如果当前用户已领取了该优惠券并且领取次数大于或等于限制次数则直接返回
        if (templateId2Coupons.containsKey(request.getTemplateSDK().getId()) && templateId2Coupons.get(request.getTemplateSDK().getId()).size() >= request.getTemplateSDK().getRule().getLimitation()) {
            log.error("优惠券领取上限:{}", request.getTemplateSDK().getId());
            throw new CouponException("优惠券领取上限");
        }

        // 尝试去获取优惠券码
        String couponCode = redisService.tryToAcquireCouponCodeFromCache(request.getTemplateSDK().getId());
        // 如果couponCode为空 则该优惠券已领取完毕
        if (StringUtils.isEmpty(couponCode)) {
            log.error("优惠券已领取完毕:{}", request.getTemplateSDK().getId());
        }

        Coupon newCoupon = new Coupon(request.getTemplateSDK().getId(), request.getUserId(), couponCode, CouponStatus.USABLE);
        newCoupon = couponDao.save(newCoupon);

        // 填充Coupon对象的CouponTemplateSDK 一定要在放入缓存之前去填充
        newCoupon.setTemplateSDK(request.getTemplateSDK());

        // 放入缓存中
        redisService.addCouponToCache(request.getUserId(), Collections.singletonList(newCoupon), CouponStatus.USABLE.getCode());

        return newCoupon;
    }

    @Override
    public SettlementInfo settlement(SettlementInfo info) throws CouponException {
        return null;
    }
}
