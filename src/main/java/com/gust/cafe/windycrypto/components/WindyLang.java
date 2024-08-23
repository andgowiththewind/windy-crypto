package com.gust.cafe.windycrypto.components;

import cn.hutool.extra.spring.SpringUtil;
import com.gust.cafe.windycrypto.config.threadlocal.LocaleHolder;
import org.springframework.context.MessageSource;

public class WindyLang {

    public static String msg(String key) {
        MessageSource messageSource = SpringUtil.getBean(MessageSource.class);
        String message = messageSource.getMessage(key, null, LocaleHolder.getLocale());
        return message;
    }
}
