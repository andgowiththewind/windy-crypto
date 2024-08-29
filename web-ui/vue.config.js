const {defineConfig} = require('@vue/cli-service')
module.exports = defineConfig({
    transpileDependencies: true,
    lintOnSave: false,
    // 解决页面覆盖报错信息 Uncaught runtime errors
    configureWebpack: {
        devServer: {
            client: {overlay: false},
        },
    },
    // 代理服务器解决跨域问题,当前项目不需要部署到Nginx上,将打包入SpringBoot
    devServer: {
        open: true,
        proxy: 'http://localhost:8899',
    },
})
