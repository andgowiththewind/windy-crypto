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
    return {}
  },// data
  methods: {},// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    devConsoleLog('WindyWebsocket mounted');
  },// mounted
}
</script>

<style scoped>
</style>