<template>
  <div>
    <div class="view-progress-btn" @click="drawerVo.show=true">
      <el-tooltip effect="dark" :content="$t('i18n_1829433519491911680')" placement="top">
        <span><i class="fa fa-table"></i></span>
      </el-tooltip>
    </div>
  </div>
</template>

<script>
// import {Notification, MessageBox, Message, Loading} from 'element-ui';
// import request from '@/utils/request';
// import * as CommonConsts from '@/config/CommonConsts';

export default {
  name: "ProcessTable",
  components: {},
  data() {
    return {
      drawerVo: {
        show: false,
      }
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
.view-progress-btn {
  opacity: 65%;
  color: rgb(0, 133, 125);
  position: fixed;
  left: 10px;
  bottom: 10px;
  font-size: 70px;
}

.view-progress-btn:hover {
  opacity: 100%;
  color: rgb(0, 133, 125);
}
</style>