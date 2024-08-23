<template>
  <div v-loading="loading">
    <div style="text-align: right;">
    </div>
    <div></div>
  </div>
</template>

<script>
// import {Notification, MessageBox, Message, Loading} from 'element-ui';
// import request from '@/utils/request';
// import * as CommonConsts from '@/config/CommonConsts';

export default {
  name: "ConditionalPagination",
  components: {},
  data() {
    return {
      loading: false,
      payload: {
        model: {name: ''},
        page: {pageNum: 1, pageSize: 10, total: 0},
        params: {scope: 'all', path: process.env.VUE_APP_DEV_TEST_FOLDER_PATH || ''},
      },
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