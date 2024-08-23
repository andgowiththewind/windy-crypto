package com.gust.cafe.windycrypto.filter;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import com.gust.cafe.windycrypto.filter.core.CachedBodyHttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 接口防抖过滤器
 *
 * @author Dororo
 * @date 2024-08-15 13:58
 */
@Slf4j
@Component
@Order(2)  // 数值越小，优先级越高
public class AntiShakeFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String requestURI = request.getRequestURI();
        // 需要过滤的接口,模式:equal
        List<String> equalList = ListUtil.toList("/crypto/cryptoSubmit", "/sys/checkSecretKey");
        boolean equalAnyMatch = equalList.stream().anyMatch(s -> s.equals(requestURI));

        // 需要过滤的接口,模式:startWith
        List<String> startWithList = ListUtil.toList("/sys/");
        boolean startWithAnyMatch = startWithList.stream().anyMatch(s -> requestURI.startsWith(s));

        // 两种模式都不匹配才过滤
        return !equalAnyMatch && !startWithAnyMatch;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 已经在之前的filter中包装过了
        CachedBodyHttpServletRequest cachedBodyHttpServletRequest = (CachedBodyHttpServletRequest) request;
        // 读取请求体
        String requestBody = cachedBodyHttpServletRequest.getRequestBodyString();
        Assert.notBlank(requestBody, "请求体不能为空");
        // 一级缓存处理防抖


        // 继续过滤器链
        filterChain.doFilter(cachedBodyHttpServletRequest, response);
    }
}
