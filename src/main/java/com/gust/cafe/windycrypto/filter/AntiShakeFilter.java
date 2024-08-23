package com.gust.cafe.windycrypto.filter;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.gust.cafe.windycrypto.constant.CacheConstants;
import com.gust.cafe.windycrypto.filter.core.CachedBodyHttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
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
    @Autowired
    private CacheManager cacheManager;// 一级缓存

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

        //
        String requestURI = request.getRequestURI();

        // 读取请求体
        String requestBody = cachedBodyHttpServletRequest.getRequestBodyString();
        Assert.notBlank(requestBody, "请求体不能为空");

        // 一级缓存处理防抖

        String cacheKey = DigestUtil.sha256Hex(requestBody);
        JSONObject jsonObjectInCache = cacheManager.getCache(CacheConstants.CAFFEINE_ANTI_SHAKE_LOCK).get(cacheKey, JSONObject.class);
        if (jsonObjectInCache != null) {
            // 说明处于防抖期间
            log.debug("一级缓存内容:{}", JSONUtil.toJsonPrettyStr(jsonObjectInCache));
            throw new RuntimeException("请求过于频繁,请稍后再试");
        } else {
            // 说明不处于防抖期间,缓存
            JSONObject cacheJson = JSONUtil.createObj()
                    .putOpt("msg", "接口正在防抖")
                    .putOpt("uri", requestURI)
                    .putOpt("body", JSONUtil.parseObj(requestBody));
            cacheManager.getCache(CacheConstants.CAFFEINE_ANTI_SHAKE_LOCK).put(cacheKey, cacheJson);
        }


        // 继续过滤器链
        filterChain.doFilter(cachedBodyHttpServletRequest, response);
    }
}
