package com.study.coupon.filter;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 自定义限流过滤器
 */
@Slf4j
@Component
@SuppressWarnings("all")
public class RateLimiterFilter extends AbstractPreZuulFilter {

    // 限流器 RateLimiter.create(2.0)即每秒只能获取到两个令牌
    RateLimiter rateLimiter = RateLimiter.create(2.0);

    @Override
    protected Object cRun() {
        HttpServletRequest request = context.getRequest();
        if (rateLimiter.tryAcquire()) {
            log.info("成功获取令牌");
            return success();
        } else {
            log.error("限流达到上限:{}", request.getRequestURI());
            return fail(402, "限流达到上限");
        }
    }

    @Override
    public int filterOrder() {
        return 2;
    }
}
