package com.study.coupon.constant;

/**
 * 通用常量定义
 */
public class Constant {

    // Kafka消息的topic
    public static final String TOPIC = "study_user_coupon_op";

    /**
     * Redis Key前缀定义
     */
    public static class RedisPrefix {

        // 优惠券码key前缀
        public static final String COUPON_TEMPLATE = "study_coupon_template_code_";

        // 用户当前所有可用的优惠券key前缀
        public static final String USER_COUPON_USABLE = "study_user_coupon_usable_";

        // 用户当前所有已使用的优惠券key前缀
        public static final String USER_COUPON_USED = "study_user_coupon_used_";

        // 用户当前所有已过期的优惠券key前缀
        public static final String USER_COUPON_EXPIRED = "study_user_coupon_expired_";
    }
}
