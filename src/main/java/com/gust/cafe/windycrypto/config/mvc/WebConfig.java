package com.gust.cafe.windycrypto.config.mvc;

import com.gust.cafe.windycrypto.interceptor.I18nInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Dororo
 * @date 2024-08-12 17:19
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private I18nInterceptor i18nInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(i18nInterceptor);
    }
}
