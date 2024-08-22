package com.gust.cafe.windycrypto.config.threadlocal;

import java.util.Locale;

/**
 * 国际化
 *
 * @author Dororo
 * @date 2024-08-12 17:11
 */
public class LocaleHolder {
    private static final ThreadLocal<Locale> userThreadLocal = new ThreadLocal<>();

    public static Locale getLocale() {
        Locale locale = userThreadLocal.get();
        if (locale == null) {
            return Locale.forLanguageTag("zh-CN");
        }
        return locale;
    }

    public static void setLocale(Locale locale) {
        userThreadLocal.set(locale);
    }

    public static void clear() {
        userThreadLocal.remove();
    }
}
