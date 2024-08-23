package com.gust.cafe.windycrypto.dto.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 控制层基础响应体
 *
 * @date 2023-06-26 23:04:39  更名为`R`
 * @date 2024-07-17 15:08:45 参考其他项目改为map形式便于扩展
 */
public class R extends HashMap<String, Object> {
    // 默认成功状态码
    public final static Integer DEFAULT_SUCCESS_CODE = HttpStatus.HTTP_OK;
    // 默认失败状态码
    public final static Integer DEFAULT_ERROR_CODE = HttpStatus.HTTP_INTERNAL_ERROR;
    // 默认成功消息
    public final static String DEFAULT_SUCCESS_MSG = "success";
    // 默认失败消息
    public final static String DEFAULT_ERROR_MSG = "error";
    // 时间戳作为序列号ID(`System.currentTimeMillis()`),避免序列化问题
    private static final long serialVersionUID = 1721200291170L;
    // KEY
    private static final String CODE = "code";
    // KEY
    private static final String MSG = "msg";
    // KEY
    private static final String DATA = "data";

    public R() {
        put(CODE, DEFAULT_SUCCESS_CODE);
        put(MSG, DEFAULT_SUCCESS_MSG);
    }

    public static R error(int code, String msg) {
        R r = new R();
        r.put(CODE, code);
        r.put(MSG, msg);
        return r;
    }

    public static R error() {
        return error(DEFAULT_ERROR_CODE, DEFAULT_ERROR_MSG);
    }

    public static R error(String msg) {
        return error(DEFAULT_ERROR_CODE, msg);
    }

    public static R ok() {
        return new R();
    }

    public static R ok(String msg) {
        R r = new R();
        r.put(MSG, msg);
        return r;
    }

    public static R ok(Map<String, Object> map) {
        R r = new R();
        r.putAll(map);
        return r;
    }

    public static R data(Object obj) {
        R r = new R();
        r.put(DATA, obj);
        return r;
    }

    // 如果接入了分页插件,则使用此方法返回分页数据
    public static R page(List<?> list) {
        PageInfo<?> pageInfo = new PageInfo<>(list);
        JSONObject data = JSONUtil.createObj()
                .putOpt("list", list)
                .putOpt("pageNum", pageInfo.getPageNum())
                .putOpt("pageSize", pageInfo.getPageSize())
                .putOpt("total", pageInfo.getTotal())
                .putOpt("totalPage", pageInfo.getPages());
        return data(data);
    }

    public R putOpt(String key, Object value) {
        if (StrUtil.isNotBlank(key)) {
            this.put(key, value);
        }
        return this;
    }
}