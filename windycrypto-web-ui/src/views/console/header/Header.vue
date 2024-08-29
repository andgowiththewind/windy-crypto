<template>
  <div>
    <div class="windy-path-input">
      <el-input size="mini" v-model="topFolderPath" :placeholder="$t('i18n_1826859557616488448')" @keyup.enter.native="handleTopFolderEnter"></el-input>
    </div>
    <div class="windy-change-language" @click="changeLanguage">
      <i>{{ 'zh' === this.$i18n.locale ? 'Switch English' : '切换中文' }}</i>
    </div>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import * as Methods from '@/config/Methods';
import {devConsoleLog} from '@/utils/commonUtils';

export default {
  name: "Header",
  components: {},
  data() {
    return {
      topFolderPath: process.env.VUE_APP_DEV_TEST_FOLDER_PATH || '',
    }
  },// data
  methods: {
    langInit() {
      // 从`localStorage`中读取上次设置的语言
      let lang = localStorage.getItem('lang');
      if (lang) {
        this.$i18n.locale = lang;
      }
    },
    changeLanguage() {
      let expectLang = 'zh' === this.$i18n.locale ? 'en' : 'zh';
      this.$i18n.locale = expectLang // 设置给本地的i18n插件
      // 存储在localStorage
      localStorage.setItem('lang', expectLang);
      Notification({
        title: expectLang === 'zh' ? '切换语言' : 'Switch Language',
        message: expectLang === 'zh' ? '切换中文成功' : 'Switch English successfully',
        type: 'success',
        duration: 2000,
        position: 'bottom-right'
      });
    },
    handleTopFolderEnter() {
      devConsoleLog('目录地址框回车事件EMIT:', this.topFolderPath);
      this.$bus.$emit(Methods.FN_GET_TREE_DATA, this.topFolderPath);
    },
    // 被通知
    contractPayload() {
      // 收到通知后向目标组件传递数据
      this.$bus.$emit(Methods.FN_UPDATE_CONDITION_PAGING_QUERY_PAYLOAD, {params: {path: this.topFolderPath}});
    },
    contractTopFolderPathCopy() {
      // 收到通知后向目标组件传递数据
      this.$bus.$emit(Methods.FN_UPDATE_TOP_FOLDER_PATH_COPY, this.topFolderPath);
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    this.$bus.$on(Methods.FN_UPDATE_TOP_FOLDER_PATH, (_topFolderPath) => this.topFolderPath = _topFolderPath);
    this.$bus.$on(Methods.FN_CONTRACT_PAYLOAD, () => this.contractPayload());
    this.$bus.$on(Methods.FN_CONTRACT_TOP_FOLDER_PATH_COPY, () => this.contractTopFolderPathCopy());
    // 从`localStorage`中读取上次设置的语言
    this.langInit();
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