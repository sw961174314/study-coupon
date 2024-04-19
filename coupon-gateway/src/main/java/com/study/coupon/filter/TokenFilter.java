package com.study.coupon.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 自定义Token过滤器
 */
@Slf4j
@Component
public class TokenFilter extends AbstractPreZuulFilter {

    @Override
    protected Object cRun() {
        HttpServletRequest request = context.getRequest();
        log.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));
        Object token = request.getParameter("token");
        if (null == token) {
            log.error("token为空");
            return fail(401, "token为空");
        }
        return success();
    }

    @Override
    public int filterOrder() {
        // 数字越先越先被执行
        return 1;
    }
}
