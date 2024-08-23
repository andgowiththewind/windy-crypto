package com.gust.cafe.windycrypto.controller.core;

import com.gust.cafe.windycrypto.exception.GlobalExceptionHandler;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局ERROR中转处理器
 *
 * @author Dororo
 * @date 2023-11-20 15:54
 * @see GlobalExceptionHandler
 */
@RestController
public class GlobalCommonErrorController implements ErrorController {
    @RequestMapping("/error")
    public void handleError(HttpServletRequest request) throws Throwable {
        String key = "javax.servlet.error.exception";
        if (request.getAttribute(key) != null) {
            // 再抛出以便让自定义的"全局异常处理器"(@RestControllerAdvice)捕获到并处理
            throw (Throwable) request.getAttribute(key);
        }
    }
}
