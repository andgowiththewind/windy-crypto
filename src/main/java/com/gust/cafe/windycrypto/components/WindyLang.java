package com.gust.cafe.windycrypto.components;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.gust.cafe.windycrypto.config.threadlocal.LocaleHolder;
import org.springframework.context.MessageSource;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class WindyLang {

    public static String msg(String key) {
        MessageSource messageSource = SpringUtil.getBean(MessageSource.class);
        String message = messageSource.getMessage(key, null, LocaleHolder.getLocale());
        return message;
    }

    public static String msg(String... keys) {
        if (keys.length == 0) {
            return "";
        }
        Locale locale = LocaleHolder.getLocale();
        // 中文环境下不需要空格
        if (locale.getLanguage().equals("zh")) {
            return Arrays.stream(keys).map(WindyLang::msg).collect(Collectors.joining());
        } else {
            return Arrays.stream(keys).map(WindyLang::msg).collect(Collectors.joining(" "));
        }
    }


}
