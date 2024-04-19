package com.study.coupon.service.impl;

import com.google.common.base.Stopwatch;
import com.study.coupon.constant.Constant;
import com.study.coupon.dao.CouponTemplateDao;
import com.study.coupon.entity.CouponTemplate;
import com.study.coupon.service.IAsyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 异步服务接口实现
 */
@Slf4j
@Service
public class AsyncServiceImpl implements IAsyncService {

    @Autowired
    private CouponTemplateDao templateDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 根据模板异步的创建优惠券码
     * @param template {@link CouponTemplate} 优惠券模板实体
     */
    @Override
    @SuppressWarnings("all")
    @Async("getAsyncExecutor")
    public void asyncConstructCouponByTemplate(CouponTemplate template) {
        // 创建一个计时器
        Stopwatch watch = Stopwatch.createStarted();

        Set<String> couponCodes = buildCouponCode(template);

        // study_coupon_template_code_1
        String redisKey = String.format("%s%s", Constant.RedisPrefix.COUPON_TEMPLATE, template.getId().toString());
        log.info("将优惠券码注入到Redis中:{}", redisTemplate.opsForList().rightPushAll(redisKey, couponCodes));
        template.setAvailable(true);
        templateDao.save(template);

        watch.stop();
        log.info("花费了:{}ms",watch.elapsed(TimeUnit.MILLISECONDS));

        // todo 发送短信或者邮件通知运营人员优惠券模板已经可用
        log.info("优惠券模板{}已经可用", template.getId());
    }

    /**
     * 构造优惠券码
     * 优惠券码(对应于每一张优惠券 18位)
     * 前面四位:产品线+类型 中间六位:日期随机 后面八位:0~9随机数
     * @param template
     * @return Set<String>与template.count相同个数的优惠券码
     */
    @SuppressWarnings("all")
    private Set<String> buildCouponCode(CouponTemplate template) {
        // 创建一个计时器
        Stopwatch watch = Stopwatch.createStarted();

        Set<String> result = new HashSet<>(template.getCount());

        // 前面四位
        String prefix4 = template.getProductLine().getCode().toString() + template.getCategory().getCode();
        // 时间编码
        String date = new SimpleDateFormat("yyMMdd").format(template.getCreateTime());
        for (int i = 0; i != template.getCount(); ++i) {
            result.add(prefix4 + buildCouponCodeSuffix14(date));
        }
        while (result.size() < template.getCount()) {
            result.add(prefix4 + buildCouponCodeSuffix14(date));
        }
        assert result.size() == template.getCount();

        watch.stop();
        log.info("花费了:{}ms",watch.elapsed(TimeUnit.MILLISECONDS));

        return result;
    }

    /**
     * 构造优惠券码的后14位
     * @param date 创建优惠券日期
     * @return 14位优惠券码
     */
    private String buildCouponCodeSuffix14(String date) {
        char[] bases = new char[]{'1', '2', '3', '4', '5', '6', '7', '8', '9'};
        // 中间6位
        List<Character> chars = date.chars().mapToObj(e -> (char) e).collect(Collectors.toList());
        Collections.shuffle(chars);
        String mid6 = chars.stream().map(Objects::toString).collect(Collectors.joining());
        // 后面8位
        String suffix8 = RandomStringUtils.random(1, bases) + RandomStringUtils.randomNumeric(7);
        return mid6 + suffix8;
    }
}
