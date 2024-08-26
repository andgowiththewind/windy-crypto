package com.gust.cafe.windycrypto.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.constant.CommonConstants;
import com.gust.cafe.windycrypto.dto.core.Windy;
import com.gust.cafe.windycrypto.enums.WindyStatusEnum;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.vo.req.InsightTableReqVo;
import com.gust.cafe.windycrypto.vo.res.InsightTableResVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InsightTableService {
    private final WindyCacheService windyCacheService;

    public InsightTableService(WindyCacheService windyCacheService) {
        this.windyCacheService = windyCacheService;
    }

    public InsightTableResVo getInsightTableData(InsightTableReqVo reqVo) {
        // (0)校验
        verify(reqVo);
        // (1)只处理条件查询未分页
        List<Windy> all = handleConditional(reqVo);
        // (2)处理分页
        List<Windy> filter = handlePaging(reqVo, all);
        return InsightTableResVo.builder().list(filter).total(Long.valueOf(all.size())).build();
    }

    private void verify(InsightTableReqVo reqVo) {
        // 路径
        String path = reqVo.getParams().getPath();
        // 范围
        InsightTableReqVo.ScopeEnum scopeEnum = InsightTableReqVo.ScopeEnum.getByLabelEqualsIgnoreCase(reqVo.getParams().getSearchScope());
        // 路径校验
        WindyException.run((Void) -> Assert.isTrue(FileUtil.exist(path) && FileUtil.isDirectory(path), WindyLang.msg("i18n_1826891933163851776")));
        // 范围校验
        WindyException.run((Void) -> Assert.notNull(scopeEnum, WindyLang.msg("i18n_1826891933163851777")));
    }

    private List<Windy> handleConditional(InsightTableReqVo reqVo) {
        // 自定义临时大容量线程池
        ForkJoinPool customThreadPool = new ForkJoinPool(3000);
        log.debug("开启一个自定义线程池,线程数[{}]", customThreadPool.getParallelism());
        try {
            TimeInterval timer = DateUtil.timer();
            List<File> loopFiles = FileUtil.loopFiles(reqVo.getParams().getPath());
            // 取消ForkJoinPool
            List<Windy> windyList = getWindyList(loopFiles, BeanUtil.copyProperties(reqVo, InsightTableReqVo.class));
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

    private List<Windy> getWindyList(List<File> loopFiles, InsightTableReqVo reqVo) {
        List<Windy> collect = loopFiles.stream()
                .filter(getFilePredicate(reqVo))
                .map(getFileWindyFunction(reqVo))
                .collect(Collectors.toList());
        return collect;
    }

    // 谓词:从所有文件中筛选符合要求的文件
    private Predicate<File> getFilePredicate(InsightTableReqVo reqVo) {
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
            Windy windy = windyCacheService.lockGetOrDefault(absPath);
            // 只需要展示部分状态的文件
            ArrayList<Integer> showCodeList = ListUtil.toList(
                    WindyStatusEnum.FREE.getCode(),
                    WindyStatusEnum.WAITING.getCode(),
                    WindyStatusEnum.OUTPUTTING.getCode(),
                    WindyStatusEnum.ALMOST.getCode()
            );
            if (!showCodeList.contains(windy.getCode())) return false;
            // 符合要求则返回true
            return true;
        };
    }

    // 函数:将文件转换为Windy对象
    private Function<File, Windy> getFileWindyFunction(InsightTableReqVo reqVo) {
        return ff -> {
            String absolutePath = FileUtil.getAbsolutePath(ff);
            Windy windy = windyCacheService.lockGetOrDefault(absolutePath);
            return windy;
        };
    }

    private List<Windy> handlePaging(InsightTableReqVo reqVo, List<Windy> all) {
        TimeInterval timer = DateUtil.timer();
        if (CollectionUtil.isEmpty(all)) return new ArrayList<>();
        Integer pageNum = reqVo.getPage().getPageNum();
        Integer pageSize = reqVo.getPage().getPageSize();
        // 禁止使用 List.subList(start, end)
        List<Windy> after = new ArrayList<>();
        int[] startEnd = PageUtil.transToStartEnd(pageNum - 1, pageSize);
        for (int i = 0; i < all.size(); i++) {
            if (i >= startEnd[0] && i < startEnd[1]) {
                after.add(all.get(i));
            }
            if (i >= startEnd[1]) {
                break;
            }
        }
        log.debug("分页处理耗时[{}]ms", timer.intervalMs());
        return after;
    }
}
