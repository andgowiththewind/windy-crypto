<template>
  <div>
    <h1>WindyWebsocket</h1>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import {devConsoleLog} from '@/utils/commonUtils';

export default {
  name: "WindyWebsocket",
  components: {},
  data() {
    return {
      wsUrlPrefix: '',// 参考`ws://localhost:8080/windyCryptoWebsocket/{sessionId}`,但需要由后端拼接端口号后传递过来
    }
  },// data
  methods: {
    // 如果ws地址不存在则发送一次请求
    wsUrlPrefixInit() {
      if (!this.wsUrlPrefix) {
/*        this.$axios.get('/windyWebsocket/wsUrlPrefix').then(res => {
          this.wsUrlPrefix = res.data;
        });*/
      }
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    devConsoleLog('WindyWebsocket mounted');
    this.wsUrlPrefixInit();
  },// mounted
}
</script>

<style scoped>
</style>