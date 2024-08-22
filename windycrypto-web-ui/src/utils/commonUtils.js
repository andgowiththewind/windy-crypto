// 如果是开发环境则执行日志打印
export function devConsoleLog(...data) {
    if (process.env.NODE_ENV === 'development' || process.env.VUE_APP_ENV === 'development') {
        console.log(...data);
    }
}