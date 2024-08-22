<template>
  <div>
    <div class="windy-path-input">
      <el-input size="mini" v-model="topFolderPath" :placeholder="$t('windy.header.topFolderPathPlaceholder')" @keyup.enter.native="handleTopFolderEnter"></el-input>
    </div>
    <div class="windy-change-language" @click="changeLanguage">
      <i>{{ 'zh' === this.$i18n.locale ? 'Switch English' : '切换中文' }}</i>
    </div>
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
    changeLanguage() {
      let expectLang = 'zh' === this.$i18n.locale ? 'en' : 'zh';
      this.$i18n.locale = expectLang // 设置给本地的i18n插件
      // 存储在localStorage
      localStorage.setItem('lang', expectLang);
      Notification({title: '提示', message: expectLang === 'zh' ? '切换中文成功' : 'Switch English successfully', type: 'success', duration: 2000, position: 'bottom-right'});

    },
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

.windy-change-language {
  position: fixed;
  right: 10px;
  top: 10px;
}
</style>