<template>
  <div>
    <h1>WindyWebsocket</h1>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import {devConsoleLog} from '@/utils/commonUtils';
import {getWsUrlPrefix} from '@/api/windyWebsocketApi';
// import {nanoid} from 'nanoid';
import {customAlphabet} from 'nanoid'


export default {
  name: "WindyWebsocket",
  components: {},
  data() {
    return {
      wsUrlPrefix: '',// 参考`ws://localhost:8080/windyCryptoWebsocket/{sessionId}`,但需要由后端拼接端口号后传递过来
      wsSessionId: '',// 自定义的sessionId,记录当前连接
      wsInstanceVo: null,// websocket实例
      wsPromise: null,// 用于跟踪WebSocket连接状态的Promise,防止重复交叉构建
    }
  },// data
  methods: {
    // 如果ws地址不存在则发送一次请求
    wsUrlPrefixInit() {
      if (!this.wsUrlPrefix) {
        getWsUrlPrefix().then(res => this.wsUrlPrefix = res.data).catch(err => devConsoleLog(err));
      }
    },
    wsIntervalStartUp(interval) {
      setInterval(() => this.buildWsConnect(), interval);
    },
    // 检查,如果ws连接不存在则创建
    buildWsConnect() {
      if (this.wsInstanceVo != null && this.wsInstanceVo != undefined && this.wsInstanceVo.readyState === WebSocket.OPEN) {
        // devConsoleLog('WS连接状态正常,无需重复创建');
        return true;
      }
      if (!this.wsUrlPrefix) {
        devConsoleLog('前置条件未满足:`WS_URL_PREFIX`为空,无法创建WS连接');
        this.wsUrlPrefixInit();
        return false;
      }
      if (this.wsPromise) {
        devConsoleLog('前置条件未满足:`WS_PROMISE`存在,有其他线程正在创建WS连接,当前线程终止防止交叉构建');
        return false;
      }
      // 可以构建
      // 自定义会话ID
      // let sessionId = nanoid();
      const nanoid = customAlphabet('1234567890', 18)
      let sessionId = nanoid();
      const url = `${this.wsUrlPrefix}/${sessionId}`;
      // 构建Promise
      this.wsPromise = new Promise((resolve, reject) => {
        try {
          devConsoleLog(`[${sessionId}]-开始构建WS连接...`);
          // 此时是异步的,不会阻塞
          this.wsInstanceVo = new WebSocket(url);
          // 定义future:定义连接成功时的future
          this.wsInstanceVo.onopen = () => {
            devConsoleLog(`[${sessionId}]-WS连接创建成功`);
            this.wsSessionId = sessionId;
            resolve(this.wsInstanceVo);
          };
          // 定义future:定义连接失败时的future
          this.wsInstanceVo.onerror = (e) => {
            devConsoleLog(`[${sessionId}]-WS连接创建失败`, e);
            reject(e);
          };
          // 定义future:定义连接关闭时的future
          this.wsInstanceVo.onclose = (e) => {
            devConsoleLog(`[${sessionId}]-WS连接关闭`, e);
            this.wsPromise = null;
            this.wsSessionId = '';
            reject(e);
          };
          // 定义future:接收到消息时的future
          this.wsInstanceVo.onmessage = (event) => {
            devConsoleLog(`[${sessionId}]-WS接收到消息`, event);
            this.wsMsgDispatch(event.data);
          };
        } catch (e) {
          devConsoleLog('WS连接创建失败', e);
          reject(e);
        }
      });
      this.wsPromise.then(() => {
      }).catch((error) => {
        this.wsPromise = null;
      });
    },
    // ws消息分发
    wsMsgDispatch(msg) {
      devConsoleLog('WS消息分发', msg);
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    devConsoleLog('WindyWebsocket mounted');
    this.wsUrlPrefixInit();
    this.wsIntervalStartUp(2000);// 每个N毫秒检查一次ws连接
  },// mounted
}
</script>

<style scoped>
</style>