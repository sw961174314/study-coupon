package com.study.coupon.service.impl;

import com.alibaba.fastjson.JSON;
import com.study.coupon.constant.Constant;
import com.study.coupon.constant.CouponStatus;
import com.study.coupon.dao.CouponDao;
import com.study.coupon.entity.Coupon;
import com.study.coupon.service.IKafkaService;
import com.study.coupon.vo.CouponKafkaMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Kafka相关的服务接口实现
 * 核心思想:是将Cache中的Coupon的状态变化同步到DB中
 */
@Slf4j
@Service
public class KafkaServiceImpl implements IKafkaService {

    @Autowired
    private CouponDao couponDao;

    /**
     * 消息优惠券Kafka消息
     * @param record
     */
    @Override
    @KafkaListener(topics = {Constant.TOPIC},groupId = "study-coupon-1")
    public void consumeCouponKafkaMessage(ConsumerRecord<?, ?> record) {
        Optional<?> kafkaMessage = Optional.ofNullable(record.value());
        if (kafkaMessage.isPresent()) {
            Object messange = kafkaMessage.get();
            CouponKafkaMessage couponInfo = JSON.parseObject(messange.toString(), CouponKafkaMessage.class);

            log.info("在Kafka中获取优惠券信息:{}", messange.toString());

            CouponStatus status = CouponStatus.of(couponInfo.getStatus());
            switch (status) {
                case USABLE:
                    break;
                case USED:
                    processUsedCoupons(couponInfo, status);
                    break;
                case EXPIRED:
                    processExpiredCoupons(couponInfo, status);
                    break;
            }
        }
    }

    /**
     * 处理已使用的用户优惠券
     * @param kafkaMessage
     * @param status
     */
    private void processUsedCoupons(CouponKafkaMessage kafkaMessage, CouponStatus status) {
        processCouponsByStatus(kafkaMessage, status);
    }

    /**
     * 处理已过期的用户优惠券
     * @param kafkaMessage
     * @param status
     */
    private void processExpiredCoupons(CouponKafkaMessage kafkaMessage, CouponStatus status) {
        processCouponsByStatus(kafkaMessage, status);
    }

    /**
     * 根据状态处理优惠券信息
     * @param kafkaMessage
     * @param status
     */
    private void processCouponsByStatus(CouponKafkaMessage kafkaMessage, CouponStatus status) {
        List<Coupon> coupons = couponDao.findAllById(kafkaMessage.getIds());
        if (CollectionUtils.isEmpty(coupons) || coupons.size() != kafkaMessage.getIds().size()) {
            log.error("没有找到正确的优惠券信息:{}", JSON.toJSONString(kafkaMessage));
            return;
        }

        coupons.forEach(c -> c.setStatus(status));
        log.info("Kafka优惠券信息中优惠券操作数量:{}", couponDao.saveAll(coupons).size());
    }
}
