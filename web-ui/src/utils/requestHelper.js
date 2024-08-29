import CryptoJS from 'crypto-js';

/**
 * 处理请求防抖
 * @param config 请求配置
 * @param interval 防抖时间间隔
 * @returns {boolean} 是否可以继续请求
 */
export function handleRequestAntiShake(config, interval = 2000) {
    const {method, url, params, data} = config;
    const paramsString = params ? Object.keys(params).sort().map(key => `${key}:${params[key]}`).join('&') : '';
    const dataString = method === 'post' && data ? JSON.stringify(data) : '';
    const rawKey = `${method.toUpperCase()} ${url}?${paramsString}&${dataString}`;
    const requestKey = CryptoJS.MD5(rawKey).toString();

    const lastCallTime = sessionStorage.getItem(requestKey);
    const currentCallTime = Date.now();

    if (lastCallTime && currentCallTime - parseInt(lastCallTime, 10) < interval) {
        return false; // 请求应该被阻止
    }

    sessionStorage.setItem(requestKey, currentCallTime.toString());
    return true; // 请求可以继续
}

/**
 * 转换请求参数
 * @param params 参数
 * @returns {string} 转换后的参数
 */
export function tansParams(params) {
    let result = ''
    for (const propName of Object.keys(params)) {
        const value = params[propName];
        var part = encodeURIComponent(propName) + "=";
        if (value !== null && value !== "" && typeof (value) !== "undefined") {
            if (typeof value === 'object') {
                for (const key of Object.keys(value)) {
                    if (value[key] !== null && value[key] !== "" && typeof (value[key]) !== 'undefined') {
                        let params = propName + '[' + key + ']';
                        var subPart = encodeURIComponent(params) + "=";
                        result += subPart + encodeURIComponent(value[key]) + "&";
                    }
                }
            } else {
                result += part + encodeURIComponent(value) + "&";
            }
        }
    }
    return result;
}

export function handleGetRequest(config) {
    if (config.method === 'get' && config.params) {
        let url = config.url + '?' + tansParams(config.params);
        url = url.slice(0, -1); // 移除最后一个字符（可能是多余的&）
        config.params = {};
        config.url = url;
    }
    return config;
}
