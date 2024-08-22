import Vue from 'vue'
import App from './App.vue'
//
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
//
import moment from "moment"
//
import drag from './utils/drag.js'

Vue.config.productionTip = false
Vue.use(ElementUI)
Vue.use(drag)

new Vue({
    render: h => h(App),
    beforeCreate() {
        moment.locale('zh-cn');//全局使用moment.js,使用时示例:`this.$moment(new Date()).format('YYYY-MM-DD HH:mm:ss')`
        Vue.prototype.$moment = moment;
        // 安装全局事件总线
        Vue.prototype.$bus = this;
    },
}).$mount('#app')
