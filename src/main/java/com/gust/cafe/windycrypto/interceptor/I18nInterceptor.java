package com.gust.cafe.windycrypto.interceptor;

import com.gust.cafe.windycrypto.config.threadlocal.LocaleHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Dororo
 * @date 2024-08-12 17:16
 */
@Component
public class I18nInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        LocaleHolder.setLocale(request.getLocale());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        LocaleHolder.clear();
    }
}
