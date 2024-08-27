package com.gust.cafe.windycrypto.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.function.Consumer;

/**
 * 自定义业务异常
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WindyException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private Integer code;
    private String message;

    public WindyException(String message) {
        super(message);
        this.message = message;
    }


    /**
     * 调用此方法运行的代码块，如果有异常则抛出指定的自定义异常{@link WindyException}
     */
    public static void run(Consumer<Void> consumer) {
        try {
            consumer.accept(null);
        } catch (Exception e) {
            throw new WindyException(e.getMessage());
        }
    }

}
