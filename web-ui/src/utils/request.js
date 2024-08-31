// ###########################################################################################################
// #                                             简化版                                                       #
// ###########################################################################################################
import axios from 'axios'
import {Notification, MessageBox, Message, Loading} from 'element-ui'
import {handleRequestAntiShake, handleGetRequest} from './requestHelper';


// 创建axios实例
const service = axios.create({
    timeout: 10 * 1000,
    headers: {
        Accept: 'application/json; charset=utf-8',
        'Content-Type': 'application/json; charset=utf-8',
        'devIng': process.env.VUE_APP_ENV === 'development',
    },
    responseType: 'json',
})

// request拦截器
service.interceptors.request.use(config => {
    // 1.0 处理防抖:如果请求头中发现本次请求要求检查防抖
    const enableAntiShake = config.headers.enableAntiShake;
    delete config.headers.enableAntiShake; // 从headers中移除,不需要传递给后端
    // 1.1 如果需要防抖,则检查是否需要防抖
    if (enableAntiShake && !handleRequestAntiShake(config)) {
        return Promise.reject(new Error('请求过于频繁'));
    }

    // X.0 设置语言,从 localStorage 中获取
    const lang = localStorage.getItem('lang') || 'zh'; // 默认语言设置为 'en'
    config.headers['Accept-Language'] = lang;

    // 动态设置请求超时时间
    if (config.headers.timeout) {
        config.timeout = config.headers.timeout;
        delete config.headers.timeout; // 删除自定义的timeout设置，以防传递给后端
    }

    // 2.0 如果是GET请求,且有参数,则将参数拼接到url后面
    config = handleGetRequest(config);

    // 3.0 返回配置
    return config;
}, error => {
    console.log(error)
    Promise.reject(error)
})

// 响应拦截器
service.interceptors.response.use(
    // 请求成功时(res.status === 200)
    res => {
        // 未设置状态码则默认成功状态
        const code = res.data.code || 200;
        // 获取错误提示信息
        const msg = res.data.msg || '未知错误';
        // 但凡不是200的状态码,都认为是错误
        if (code !== 200) {
            // 弹框信息
            Notification.error({title: '错误', message: msg, position: 'bottom-right'});
            // 返回错误
            return Promise.reject(new Error(msg));
        }
        // 如果是200,则返回数据
        return res.data;
    },
    error => {
        // 结构赋值：从error中取出message属性,并创建一个同名的变量,并赋值给它
        let {message} = error;
        if (message == "Network Error") {
            message = "后端接口连接异常";
        } else if (message.includes("timeout")) {
            message = "系统接口请求超时";
        } else if (error.response && error.response.data && error.response.data.msg) {
            message = error.response.data.msg;
        } else if (message.includes("Request failed with status code")) {
            message = "系统接口 " + message.substr(message.length - 3) + " 异常";
        }
        Message({message: message, type: 'error', duration: 5 * 1000})
        return Promise.reject(error)
    }
)


export default service