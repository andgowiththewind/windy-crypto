<template>
  <div>
    <div class="windy-path-input">
      <el-input size="mini" v-model="topFolderPath" :placeholder="$t('i18n.header.topFolderPathPlaceholder')" @keyup.enter.native="handleTopFolderEnter"></el-input>
    </div>
    <div></div>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import * as Methods from '@/config/Methods';


export default {
  name: "Header",
  components: {},
  data() {
    return {
      topFolderPath: process.env.VUE_APP_DEV_TEST_FOLDER_PATH || '',
    }
  },// data
  methods: {
    handleTopFolderEnter() {
      this.$bus.$emit(Methods.FN_GET_TREE_DATA, this.topFolderPath);
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    // this.init();
  },// mounted
}
</script>

<style scoped>
.windy-path-input {
  position: relative;
  width: 60%;
  left: 0px;
  top: 10px;
}
</style>