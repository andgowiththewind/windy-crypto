package com.gust.cafe.windycrypto.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.constant.CommonConstants;
import com.gust.cafe.windycrypto.dto.core.Windy;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.vo.req.InsightTableReqVo;
import com.gust.cafe.windycrypto.vo.res.InsightTableResVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InsightTableService {
    public InsightTableResVo getInsightTableData(InsightTableReqVo reqVo) {
        // 路径
        String path = reqVo.getParams().getPath();
        // 范围
        InsightTableReqVo.ScopeEnum scopeEnum = InsightTableReqVo.ScopeEnum.getByLabelEqualsIgnoreCase(reqVo.getParams().getSearchScope());
        // 路径校验
        WindyException.run((Void) -> Assert.isTrue(FileUtil.exist(path) && FileUtil.isDirectory(path), WindyLang.msg("i18n_1826891933163851776")));
        // 范围校验
        WindyException.run((Void) -> Assert.notNull(scopeEnum, WindyLang.msg("i18n_1826891933163851777")));
        // (1)只处理条件查询未分页
        List<Windy> all = handleConditional(reqVo);
        // (2)处理分页
        List<Windy> filter = handlePaging(reqVo, all);
        return InsightTableResVo.builder().list(all).total(Long.valueOf(all.size())).build();
    }

    private List<Windy> handleConditional(InsightTableReqVo reqVo) {
        // 自定义临时大容量线程池
        ForkJoinPool customThreadPool = new ForkJoinPool(3000);
        log.debug("开启一个自定义线程池,线程数[{}]", customThreadPool.getParallelism());
        try {
            TimeInterval timer = DateUtil.timer();
            List<File> loopFiles = FileUtil.loopFiles(reqVo.getParams().getPath());
            InsightTableReqVo reqVoCopy = BeanUtil.copyProperties(reqVo, InsightTableReqVo.class);
            // TODO 通过 `ForkJoinPool` 的 `submit` 方法来执行 `parallelStream` 操作，`get()`方法会阻塞直到所有任务完成
            List<Windy> windyList = customThreadPool.submit(getListCallable(loopFiles, reqVoCopy)).get();
            log.debug("线程池并行性[parallelism={}],从[{}]文件筛查出[{}]对象,耗时[{}]ms",
                    customThreadPool.getParallelism(), loopFiles.size(), windyList.size(), timer.intervalMs());
            return windyList;
        } catch (Exception e) {
            throw new WindyException(e.getMessage());
        } finally {
            log.debug("关闭自定义线程池");
            customThreadPool.shutdown();
        }
    }

    // `customThreadPool.submit`的参数是一个`Callable`对象，这里使用lambda表达式实现了`Callable`接口的`call`方法
    private static Callable<List<Windy>> getListCallable(List<File> loopFiles, InsightTableReqVo reqVo) {
        return () -> loopFiles.parallelStream()
                .filter(getFilePredicate(reqVo))
                .map(getFileWindyFunction(reqVo))
                .collect(Collectors.toList());
    }

    // 谓词:从所有文件中筛选符合要求的文件
    private static Predicate<File> getFilePredicate(InsightTableReqVo reqVo) {
        return f -> {
            // 基础信息
            String name = FileUtil.getName(f);
            String extName = FileUtil.extName(f);
            String absPath = FileUtil.getAbsolutePath(f);
            // 期望的搜索范围
            InsightTableReqVo.ScopeEnum scopeEnum = InsightTableReqVo.ScopeEnum.getByLabelEqualsIgnoreCase(reqVo.getParams().getSearchScope());
            // 根据文件名前缀判断是否曾经由当前系统加密
            boolean cryptoByCurrentSys = StrUtil.isNotBlank(name) && StrUtil.startWithIgnoreCase(name, CommonConstants.ENCRYPTED_PREFIX);
            //
            // 排除文件夹
            if (FileUtil.isDirectory(f)) return false;
            // 排除自定义的临时文件,不需要在table中展示
            if (StrUtil.isNotBlank(extName) && StrUtil.equalsIgnoreCase(extName, CommonConstants.TMP_EXT_NAME)) return false;
            // 根据期望搜索范围结合是否加密过滤
            if (scopeEnum.equals(InsightTableReqVo.ScopeEnum.ALL)) {
                // 期望查看所有文件,无需过滤
            } else if (scopeEnum.equals(InsightTableReqVo.ScopeEnum.ENCRYPTED)) {
                // 期望查看加密文件,如果不是加密文件则过滤
                if (!cryptoByCurrentSys) return false;
            } else if (scopeEnum.equals(InsightTableReqVo.ScopeEnum.NOT_ENCRYPTED)) {
                // 期望查看未加密文件,如果是加密文件则过滤
                if (cryptoByCurrentSys) return false;
            }
            // 模拟`mybatis.findByAll`,根据对象的各个属性进行筛选
            if (reqVo.getModel() != null) {
                if (StrUtil.isNotBlank(reqVo.getModel().getName())) {
                    // 如果用户输入了文件名,则进行模糊查询
                    if (!StrUtil.containsIgnoreCase(name, reqVo.getModel().getName())) return false;
                }
                // 其他字段略...
            }

            // 结合当前系统设计的加解密生命周期进行筛选
            // TODO
            // TODO
            // TODO
            // TODO
            // TODO
            // TODO

            return true;
        };
    }

    // 函数:将文件转换为Windy对象
    private static Function<File, Windy> getFileWindyFunction(InsightTableReqVo reqVo) {
        return ff -> Windy.builder().build();
    }

    private List<Windy> handlePaging(InsightTableReqVo reqVo, List<Windy> all) {
        return null;
    }
}
