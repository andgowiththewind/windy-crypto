package com.gust.cafe.windycrypto.config.threadpool;

import cn.hutool.core.util.StrUtil;
import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 * <p>基于业务设计,准备两个线程池,一个线程池负责任务分发,一般耗时较短,另一个线程池辅助加解密的主要业务,耗时较长;</p>
 *
 * @author Dororo
 * @date 2023-12-03 23:41
 */
@Slf4j
@Configuration
public class ThreadPoolConfiguration {
    @Autowired
    private ThreadPoolProperties threadPoolProperties;

    @Bean(value = ThreadPoolConstants.DISPATCH)
    public ThreadPoolTaskExecutor dispatchExecutor() {
        return init(threadPoolProperties.getDispatcher(), ThreadPoolConstants.DISPATCH);
    }

    @Bean(value = ThreadPoolConstants.CRYPTO)
    public ThreadPoolTaskExecutor cryptoExecutor() {
        return init(threadPoolProperties.getCrypto(), ThreadPoolConstants.CRYPTO);
    }

    @Bean(value = ThreadPoolConstants.STAT)
    public ThreadPoolTaskExecutor statExecutor() {
        return init(threadPoolProperties.getStat(), ThreadPoolConstants.STAT);
    }


    private ThreadPoolTaskExecutor init(ThreadPoolProperties.PropDTO paramsFromYml, String threadNamePrefix) {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        // 核心线程数
        pool.setCorePoolSize(paramsFromYml.getCorePoolSize());
        // 最大线程数
        pool.setMaxPoolSize(paramsFromYml.getMaxPoolSize());
        // 队列大小,当线程预估处理时间可能非常长时,队列长度应当比任务数多,避免出现任务丢失的问题;举例:当前处理文件数在6500左右,队列设置12000;
        pool.setQueueCapacity(paramsFromYml.getQueueCapacity());
        // 线程名称前缀,方便日志快速定位问题
        pool.setThreadNamePrefix(StrUtil.format("[{}]", threadNamePrefix));
        // 线程最大空闲时间(秒)
        pool.setKeepAliveSeconds(paramsFromYml.getKeepAliveSeconds());
        // 拒绝策略,当任务添加到线程失败时,采取什么操作?比如`CallerRunsPolicy`代表"重试添加当前的任务直至成功";
        // [AbortPolicy]:对拒绝任务抛弃处理,并且抛出异常。
        // [DiscardPolicy]:对拒绝任务直接无声抛弃,没有异常信息。
        // [CallerRunsPolicy]:这个策略重试添加当前的任务,他会自动重复调用 execute() 方法,直到成功。
        // [DiscardOldestPolicy]:对拒绝任务不抛弃,而是抛弃队列里面等待最久的一个线程,然后把拒绝任务加到队列。
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 如果`@Bean`就不需手动,会自动`InitializingBean`的`afterPropertiesSet`来调`initialize`
        // pool.initialize();
        return pool;
    }
}