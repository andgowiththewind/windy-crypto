<template>
  <div>
    <div v-if="isShowLogo==false">
      <Console>
      </Console>
    </div>
    <div v-if="isShowLogo==true">
      <LogoCover @unCoverLogo="isShowLogo=false"></LogoCover>
    </div>
  </div>
</template>

<script>
// import {Notification, MessageBox, Message, Loading} from 'element-ui';
// import request from '@/utils/request';
// import * as CommonConsts from '@/config/CommonConsts';
import LogoCover from '@/views/logo/LogoCover.vue';
import Console from '@/views/console/Console.vue';

export default {
  name: "AppWrapper",
  components: {LogoCover, Console},
  data() {
    return {
      isShowLogo: true,
    }
  },// data
  methods: {},// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    // this.init();
  },// mounted
}
</script>

<style scoped>
</style>