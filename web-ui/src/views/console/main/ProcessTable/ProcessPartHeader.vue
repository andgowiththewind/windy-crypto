<template>
  <div>
    <el-alert class="shake-on-hover" title="(ง •̀_•́)ง" center type="success" :closable="false"></el-alert>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';

export default {
  name: "ProcessPartHeader",
  components: {},
  data() {
    return {
      alertStyle: "alert-style",
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