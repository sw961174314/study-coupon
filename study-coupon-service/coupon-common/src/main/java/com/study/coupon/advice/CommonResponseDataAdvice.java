package com.study.coupon.advice;

import com.study.coupon.annotation.IgnoreResponseAdvice;
import com.study.coupon.vo.CommonResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应
 */
@RestControllerAdvice
public class CommonResponseDataAdvice implements ResponseBodyAdvice {

    /**
     * 判断是否需要对响应进行处理
     * @param methodParameter
     * @param aClass
     * @return
     */
    @Override
    @SuppressWarnings("all")
    public boolean supports(MethodParameter methodParameter, Class aClass) {
        // 如果当前方法所在的类标识了@IgnoreResponseAdvice注解 则不需要处理
        if (methodParameter.getDeclaringClass().isAnnotationPresent(IgnoreResponseAdvice.class)) {
            return false;
        }
        // 如果当前方法标识了@IgnoreResponseAdvice注解 则不需要处理
        if (methodParameter.getMethod().isAnnotationPresent(IgnoreResponseAdvice.class)) {
            return false;
        }
        // 对响应进行处理 执行beforeBodyWrite方法
        return true;
    }


    /**
     * 响应返回之前的处理
     * @param o
     * @param methodParameter
     * @param mediaType
     * @param aClass
     * @param serverHttpRequest
     * @param serverHttpResponse
     * @return
     */
    @Override
    @SuppressWarnings("all")
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        // 定义最终的返回对象
        CommonResponse<Object> response = new CommonResponse<>(0, "");
        if (null == o){
            // 如果o是null response不需要设置data
            return response;
        } else if (o instanceof CommonResponse) {
            // 如果o已经是CommonResponse 则不需要再次处理
            response = (CommonResponse<Object>) o;
        } else {
            // 把响应对象作为CommonResponse的data部分
            response.setData(o);
        }
        return null;
    }
}
