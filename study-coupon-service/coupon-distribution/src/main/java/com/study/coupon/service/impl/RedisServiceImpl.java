package com.study.coupon.service.impl;

import com.alibaba.fastjson.JSON;
import com.study.coupon.constant.Constant;
import com.study.coupon.constant.CouponStatus;
import com.study.coupon.entity.Coupon;
import com.study.coupon.exception.CouponException;
import com.study.coupon.service.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis相关的操作服务接口实现
 */
@Slf4j
@Service
public class RedisServiceImpl implements IRedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 根据userId和status找到缓存的优惠券列表数据
     * @param userId
     * @param status
     * @return
     */
    @Override
    public List<Coupon> getCachedCoupons(Long userId, Integer status) {
        log.info("从缓存中得到优惠券数据:{},{}", userId, status);
        String redisKey = status2RedisKey(status, userId);

        // redisTemplate.opsForHash().values(redisKey).stream().map(o -> Objects.toString(o,null)).collect(Collectors.toList());
        // 1.通过redisTemplate.opsForHash()获取Redis中哈希数据结构的API
        // 2.使用values(redisKey)方法获取指定键（redisKey）下的所有值
        // 3.将获取到的值转换为Stream流
        // 4.使用map操作将每个值转换为字符串表示，如果值为null，则返回null
        // 5.使用collect操作将转换后的字符串收集到一个List中
        List<String> couponStrs = redisTemplate.opsForHash().values(redisKey).stream().map(o -> Objects.toString(o,null)).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(couponStrs)) {
            saveEmptyCouponListToCache(userId, Collections.singletonList(status));
            return Collections.emptyList();
        }
        return couponStrs.stream().map(cs -> JSON.parseObject(cs, Coupon.class)).collect(Collectors.toList());
    }

    /**
     * 保存空的优惠券列表到缓存中
     * @param userId
     * @param status
     */
    @Override
    @SuppressWarnings("all")
    public void saveEmptyCouponListToCache(Long userId, List<Integer> status) {
        log.info("保存用户空的优惠券列表到缓存中:{},状态:{}", userId, JSON.toJSONString(status));

        // key:coupon_id value:序列化的Coupon
        Map<String, String> invaildCouponMap = new HashMap<>();
        invaildCouponMap.put("-1", JSON.toJSONString(Coupon.invalidCoupon()));

        // 用户优惠券缓存信息
        // K:status -> redisKey V:{coupon_id:序列化的Coupon}

        // 使用SessionCallback 把数据命令放入到Redis的pipeline
        SessionCallback<Object> sessionCallback = new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                status.forEach(s -> {
                    String redisKey = status2RedisKey(s, userId);
                    operations.opsForHash().putAll(redisKey, invaildCouponMap);
                });
                return null;
            }
        };

        log.info("Pipeline:{}", JSON.toJSONString(redisTemplate.executePipelined(sessionCallback)));
    }

    /**
     * 尝试从Cache中获取一个优惠券码
     * @param templateId
     * @return
     */
    @Override
    public String tryToAcquireCouponCodeFromCache(Integer templateId) {
        String redisKey = String.format("%s%s", Constant.RedisPrefix.COUPON_TEMPLATE, templateId.toString());
        // 优惠券码不存在顺序关系 左边和右边pop没有影响
        String couponCode = redisTemplate.opsForList().leftPop(redisKey);
        log.info("获取优惠券编码:{},{},{}", templateId, redisKey, couponCode);
        return couponCode;
    }

    /**
     * 将优惠券保存到Cache中
     * @param userId
     * @param coupons
     * @param status
     * @return
     * @throws CouponException
     */
    @Override
    public Integer addCouponToCache(Long userId, List<Coupon> coupons, Integer status) throws CouponException {
        log.info("将优惠券保存到Cache中:{},{},{}", userId, JSON.toJSONString(coupons), status);
        Integer result = -1;
        CouponStatus couponStatus = CouponStatus.of(status);

        switch (couponStatus) {
            case USABLE:
                // 将新增加的优惠券加入到缓存中
                result = addCouponToCacheForUsable(userId, coupons);
                break;
            case USED:
                // 将已使用的优惠券加入到缓存中
                result = addCouponToCacheForUsed(userId, coupons);
                break;
            case EXPIRED:
                // 将已过期的优惠券加入到缓存中
                result = addCouponToCacheForExpired(userId, coupons);
                break;
        }
        return result;
    }

    /**
     * 根据status获取到对应的Redis Key
     * @param status
     * @param userId
     * @return
     */
    private String status2RedisKey(Integer status, Long userId) {
        String redisKey = null;
        CouponStatus couponStatus = CouponStatus.of(status);

        switch (couponStatus){
            case USABLE:
                redisKey = String.format("%s%s", Constant.RedisPrefix.USER_COUPON_USABLE, userId);
                break;
            case USED:
                redisKey = String.format("%s%s", Constant.RedisPrefix.USER_COUPON_USED, userId);
                break;
            case EXPIRED:
                redisKey = String.format("%s%s", Constant.RedisPrefix.USER_COUPON_EXPIRED, userId);
                break;
        }
        return redisKey;
    }

    /**
     * 获取一个随机的过期时间
     * @param min
     * @param max
     * @return
     */
    private Long getRandomExpirationTime(Integer min, Integer max) {
        return RandomUtils.nextLong(min * 60 * 60, max * 60 * 60);
    }

    /**
     * 将新增加的优惠券加入到缓存中
     * @param userId
     * @param coupons
     * @return
     */
    private Integer addCouponToCacheForUsable(Long userId, List<Coupon> coupons) {
        // 如果status是USABLE 代表是新增加的优惠券
        // 只会影响一个Cache:USER_COUPON_USABLE
        log.debug("添加新增加的优惠券到内存中");

        Map<String, String> needCacheObject = new HashMap<>();
        coupons.forEach(c -> needCacheObject.put(c.getId().toString(), JSON.toJSONString(c)));

        String redisKey = status2RedisKey(CouponStatus.USABLE.getCode(), userId);
        redisTemplate.opsForHash().putAll(redisKey, needCacheObject);
        log.info("添加{}优惠券到内存:{},{}", needCacheObject.size(), userId, redisKey);

        // 设置过期时间 防止缓存雪崩
        redisTemplate.expire(redisKey, getRandomExpirationTime(1, 2), TimeUnit.SECONDS);

        return needCacheObject.size();
    }

    /**
     * 将已使用的优惠券加入到缓存中
     * @param userId
     * @param coupons
     * @return
     */
    @SuppressWarnings("all")
    private Integer addCouponToCacheForUsed(Long userId, List<Coupon> coupons) throws CouponException {
        // 如果status是USED 代表用户操作时使用当前的优惠券 影响到USABLE和USED的Cache
        log.debug("添加已使用的优惠券到内存中");

        Map<String, String> needCacheForUsed = new HashMap<>();

        String redisKeyForUsable = status2RedisKey(CouponStatus.USABLE.getCode(), userId);
        String redisKeyForUsed = status2RedisKey(CouponStatus.USED.getCode(), userId);

        // 获取当前用户可用的优惠券
        List<Coupon> curUsableCoupons = getCachedCoupons(userId, CouponStatus.USABLE.getCode());
        // 当前可用的优惠券个数一定大于1个
        assert curUsableCoupons.size() > coupons.size();

        coupons.forEach(c -> needCacheForUsed.put(c.getId().toString(), JSON.toJSONString(c)));

        // 校验当前的优惠券参数是否与Cache中的匹配
        // curUsableIds当前优惠券id列表 paramIds缓存中的优惠券id列表
        List<Integer> curUsableIds = curUsableCoupons.stream().map(Coupon::getId).collect(Collectors.toList());
        List<Integer> paramIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());

        // CollectionUtils.isSubCollection(a, b) 判断a是不是b的子集
        if (!CollectionUtils.isSubCollection(paramIds, curUsableIds)) {
            log.error("缓存中没有与当前优惠券参数相同的:{},{},{}", userId, JSON.toJSONString(curUsableIds), JSON.toJSONString(paramIds));
            throw new CouponException("缓存中没有与当前优惠券参数相同的");
        }

        List<String> needCleanKey = paramIds.stream().map(i -> i.toString()).collect(Collectors.toList());
        SessionCallback<Objects> sessionCallback = new SessionCallback<Objects>() {
            @Override
            public Objects execute(RedisOperations operations) throws DataAccessException {
                // 1.已使用的优惠券Cache缓存添加
                operations.opsForHash().putAll(redisKeyForUsed, needCacheForUsed);
                // 2.可用的优惠券Cache缓存清理
                operations.opsForHash().delete(redisKeyForUsable, needCleanKey.toArray());
                // 3.重置redisKeyForUsable和redisKeyForUsed的过期时间
                operations.expire(redisKeyForUsable, getRandomExpirationTime(1, 2), TimeUnit.SECONDS);
                operations.expire(redisKeyForUsed, getRandomExpirationTime(1, 2), TimeUnit.SECONDS);

                return null;
            }
        };
        log.info("Pipeline:{}", JSON.toJSONString(redisTemplate.executePipelined(sessionCallback)));

        return coupons.size();
    }

    /**
     * 将已过期的优惠券加入到缓存中
     * @param userId
     * @param coupons
     * @return
     */
    @SuppressWarnings("all")
    private Integer addCouponToCacheForExpired(Long userId, List<Coupon> coupons) throws CouponException {
        // 如果status是EXPIRED 代表是已有的优惠券过期了 影响到两个Cache
        log.debug("添加已使用的优惠券到内存中");

        // 最终需要保存的Cache
        Map<String, String> needCacheForExpired = new HashMap<>(coupons.size());

        String redisKeyForUsable = status2RedisKey(CouponStatus.USABLE.getCode(), userId);
        String redisKeyForExpired = status2RedisKey(CouponStatus.EXPIRED.getCode(), userId);

        List<Coupon> curUsableCoupons = getCachedCoupons(userId, CouponStatus.USABLE.getCode());
        List<Coupon> curExpiredCoupons = getCachedCoupons(userId, CouponStatus.EXPIRED.getCode());

        // 当前可用的优惠券个数一定是大于1的
        assert curUsableCoupons.size() > coupons.size();

        coupons.forEach(c -> needCacheForExpired.put(c.getId().toString(), JSON.toJSONString(c)));

        // 校验当前的优惠券参数是否与Cache中的匹配
        List<Integer> curUsableIds = curUsableCoupons.stream().map(Coupon::getId).collect(Collectors.toList());
        List<Integer> paramIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        if (!CollectionUtils.isSubCollection(paramIds, curUsableIds)) {
            log.error("缓存中没有与当前优惠券参数相同的:{},{},{}", userId, JSON.toJSONString(curUsableIds), JSON.toJSONString(paramIds));
            throw new CouponException("缓存中没有与当前优惠券参数相同的");
        }

        List<String> needCleanKey = paramIds.stream().map(i -> i.toString()).collect(Collectors.toList());
        SessionCallback<Objects> sessionCallback = new SessionCallback<Objects>() {
            @Override
            public Objects execute(RedisOperations operations) throws DataAccessException {
                // 1.已过期的优惠券Cache缓存添加
                operations.opsForHash().putAll(redisKeyForExpired, needCacheForExpired);
                // 2.可用的优惠券Cache缓存清理
                operations.opsForHash().delete(redisKeyForUsable, needCleanKey.toArray());
                // 3.重置redisKeyForUsable和redisKeyForExpired的过期时间
                operations.expire(redisKeyForUsable, getRandomExpirationTime(1, 2), TimeUnit.SECONDS);
                operations.expire(redisKeyForExpired, getRandomExpirationTime(1, 2), TimeUnit.SECONDS);

                return null;
            }
        };
        log.info("Pipeline:{}", JSON.toJSONString(redisTemplate.executePipelined(sessionCallback)));

        return coupons.size();
    }
}
