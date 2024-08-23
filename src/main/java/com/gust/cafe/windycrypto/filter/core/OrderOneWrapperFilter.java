package com.gust.cafe.windycrypto.filter.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 在第一个filter中对`request`进行包装实现流多次读取的要求
 *
 * @author Dororo
 * @date 2024-08-15 15:02
 */
@Slf4j
@Component
@Order(1) // 数值越小，优先级越高
public class OrderOneWrapperFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new CachedBodyHttpServletRequest(request), response);
    }
}
