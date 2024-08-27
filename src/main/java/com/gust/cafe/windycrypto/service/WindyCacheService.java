package com.gust.cafe.windycrypto.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.gust.cafe.windycrypto.components.RedisMasterCache;
import com.gust.cafe.windycrypto.constant.CacheConstants;
import com.gust.cafe.windycrypto.constant.CommonConstants;
import com.gust.cafe.windycrypto.dto.core.Windy;
import com.gust.cafe.windycrypto.enums.WindyStatusEnum;
import com.gust.cafe.windycrypto.exception.WindyException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 处理文件缓存
 *
 * @author Dororo
 * @date 2024-08-23 17:06
 */
@Slf4j
@Service
public class WindyCacheService {
    private final RedisMasterCache redisMasterCache;
    private final RedissonClient redissonClient;

    public WindyCacheService(RedisMasterCache redisMasterCache, RedissonClient redissonClient) {
        this.redisMasterCache = redisMasterCache;
        this.redissonClient = redissonClient;
    }

    // 通过绝对路径获取Windy对象
    public Windy lockGetOrDefault(String absPath) {
        TimeInterval timer = DateUtil.timer();
        // 定义业务操作
        Supplier<Windy> supplier = getGetOrDefaultSupplier(absPath);
        // 提交业务操作到分布式锁代码结构
        Windy windy = handleGetOrDefaultLockExecute(absPath, supplier);
        // log.debug("获取或新建Windy对象,耗时[{}ms]", timer.intervalMs());
        return windy;
    }

    private Supplier<Windy> getGetOrDefaultSupplier(String absPath) {
        // Supplier主要内容:如果缓存中有,则返回缓存中的Windy对象,否则新建一个Windy对象返回
        return () -> {
            Windy cacheVo = redisMasterCache.getCacheMapValue(CacheConstants.WINDY_MAP, parseId(absPath));
            if (cacheVo != null) return cacheVo;
            // 如果缓存中不存在对应的文件信息,则新建并返回
            Integer code = (FileUtil.exist(FileUtil.file(absPath)) ? WindyStatusEnum.FREE.getCode() : WindyStatusEnum.NOT_EXIST.getCode());
            long size = FileUtil.size(FileUtil.file(absPath));
            String sizeLabel = FileUtil.readableFileSize(size);
            // 根据文件名判断是否已加密
            boolean hadEncrypted = StrUtil.startWithIgnoreCase(FileUtil.getName(absPath), CommonConstants.ENCRYPTED_PREFIX);
            //
            Windy insertVo = Windy.builder()
                    .id(parseId(absPath))
                    // 统一转正斜杠"/"
                    .absPath(FileUtil.file(absPath).getAbsolutePath().replace("\\", "/"))
                    .mainName(FileUtil.mainName(absPath))
                    .extName(FileUtil.extName(absPath))
                    .name(FileUtil.getName(absPath))
                    .code(code)
                    .label(WindyStatusEnum.getByCode(code).getLabel())
                    .desc(WindyStatusEnum.getByCode(code).getRemark())
                    .latestMsg("create just 1 second")
                    .percentage(null)
                    .percentageLabel(null)
                    .size(size)
                    .sizeLabel(sizeLabel)
                    .hadEncrypted(hadEncrypted)
                    .createTime(DateUtil.now())
                    .updateTime(DateUtil.now())
                    .build();
            redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, insertVo.getId(), insertVo);
            return insertVo;
        };
    }

    // 分布式锁执行业务操作
    // 防止并发操作,保证数据一致性;即防止同一个绝对路径的文件被多次创建
    @SneakyThrows // `tryLock()`方法抛出`InterruptedException`异常
    private Windy handleGetOrDefaultLockExecute(String absPath, Supplier<Windy> supplier) {
        String threadName = Thread.currentThread().getName();
        Windy result = null;
        // 绝对路径转ID
        String id = parseId(absPath);
        // 拼接分布式锁KEY
        String lockKey = StrUtil.format("{}:{}", CacheConstants.WINDY_GET_OR_DEFAULT_LOCK, id);
        // 定义锁对象
        RLock lock = redissonClient.getLock(lockKey);
        // 根据当前业务设计的超时时间、过期时间,尝试获取锁
        // tryLock(最多等待锁的时间,获取锁成功后锁的过期时间,时间单位)
        if (lock.tryLock(5, 15, TimeUnit.SECONDS)) {
            // 当前线程加锁成功,执行业务操作
            try {
                result = supplier.get();
            } finally {
                // 确保释放锁
                lock.unlock();
            }
        } else {
            // 如果当前线程获取锁失败,说明有其他线程正在处理相同的绝对路径,但是耗时过长
            log.debug("其他线程`lockGetOrDefault`耗时过长,线程[{}]获取锁失败", threadName);
            throw new WindyException("获取锁失败");
        }
        return result;
    }

    /**
     * 根据绝对路径获取全局ID,缓存此对应关系
     *
     * @param absPath 绝对路径
     * @return 简化, 雪花算法生成的ID
     */
    public String parseId(String absPath) {
        Assert.notBlank(absPath);
        // 统一转正斜杠"/"
        absPath = FileUtil.file(absPath).getAbsolutePath();
        absPath = absPath.replace("\\", "/");
        // 先查缓存
        String id = redisMasterCache.getCacheMapValue(CacheConstants.PATH_ID_MAP, absPath);
        if (StrUtil.isNotBlank(id)) return id;
        // 如果缓存中没有,新生成并缓存
        // 使用雪花算法生成ID
        String nextIdStr = IdUtil.getSnowflakeNextIdStr();
        // 缓存
        redisMasterCache.setCacheMapValue(CacheConstants.PATH_ID_MAP, absPath, nextIdStr);
        return nextIdStr;
    }


    @SneakyThrows
    public void lockUpdate(String absPath, Consumer<String> consumer, Long waitTime, Long leaseTime, TimeUnit unit) {
        waitTime = Optional.ofNullable(waitTime).orElse(5L);
        leaseTime = Optional.ofNullable(leaseTime).orElse(15L);
        unit = Optional.ofNullable(unit).orElse(TimeUnit.SECONDS);
        //
        Assert.isTrue(StrUtil.isNotBlank(absPath), "绝对路径不能为空");
        String id = parseId(absPath);
        Windy currentCache = redisMasterCache.getCacheMapValue(CacheConstants.WINDY_MAP, id);// 需要通过`redisMasterCache`来查
        Assert.notNull(currentCache, "当前缓存对象不能为空,不能执行更新");
        // 拼接分布式锁KEY
        String lockKey = StrUtil.format("{}:{}", CacheConstants.WINDY_UPDATE_LOCK, id);
        // 定义锁对象
        RLock lock = redissonClient.getLock(lockKey);
        // 5, 15, TimeUnit.SECONDS
        if (lock.tryLock(waitTime, leaseTime, unit)) {
            // 当前线程加锁成功,执行业务操作
            try {
                consumer.accept(absPath);
            } finally {
                // 确保释放锁
                lock.unlock();
            }
        } else {
            // 没抢到锁就不要更新
            throw new WindyException("获取锁失败,无法更新");
        }
    }

    public void lockUpdate(String absPath, Consumer<String> consumer) {
        lockUpdate(absPath, consumer, null, null, null);
    }
}
