package com.gust.cafe.windycrypto;

import com.gust.cafe.windycrypto.util.RunUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import tk.mybatis.spring.annotation.MapperScan;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.gust.cafe.windycrypto.mapper")
public class WindyCryptoApplication {

    public static void main(String[] args) {
        RunUtils.check();
        SpringApplication.run(WindyCryptoApplication.class, args);
    }

}
