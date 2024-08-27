<template>
  <div v-loading="loading">
    <div class="business-btn-zone">
      <el-form @submit.native.prevent :inline="true" size="small">
        <el-form-item label="">
          <el-input v-model="userPassword" placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="">
          <el-button-group>
            <el-button size="mini" type="info" @click="encryptAll" :style="{minWidth:'25vw'}"><span><i class="fa fa-lock"></i>&nbsp;目录下全部加密</span></el-button>
            <el-button size="mini" type="danger" plain @click="decryptAll"><span>全部解密&nbsp;<i class="fa fa-wrench"></i></span></el-button>
          </el-button-group>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import * as Methods from '@/config/Methods';
import {cryptoSubmitFn} from "@/api/insightTableApi";

export default {
  name: "Password2FA",
  components: {},
  data() {
    return {
      loading: false,
      userPassword: process.env.VUE_APP_DEV_TEST_PASSWORD || '',
      topFolderPathCopy: '',
    }
  },// data
  methods: {
    encryptAll() {
      this.loading = true;
      // 要求更新`topFolderPathCopy`
      this.$bus.$emit(Methods.FN_CONTRACT_TOP_FOLDER_PATH_COPY);
      let cryptoSubmitPayload = {
        windyPathList: [],
        dirPathList: [this.topFolderPathCopy],
        askEncrypt: true,
        userPassword: this.userPassword,
      };
      cryptoSubmitFn(cryptoSubmitPayload).then(res => {
        Notification.success({title: this.$t('i18n_1827961313217875968'), message: res.msg, position: 'bottom-right'});
      }).finally(() => this.loading = false);
    },
    decryptAll() {
      this.loading = true;
      // 要求更新`topFolderPathCopy`
      this.$bus.$emit(Methods.FN_CONTRACT_TOP_FOLDER_PATH_COPY);
      let cryptoSubmitPayload = {
        windyPathList: [],
        dirPathList: [this.topFolderPathCopy],
        askEncrypt: false,
        userPassword: this.userPassword,
      };
      cryptoSubmitFn(cryptoSubmitPayload).then(res => {
        Notification.success({title: this.$t('i18n_1827961313217875969'), message: res.msg, position: 'bottom-right'});
      }).finally(() => this.loading = false);
    },
    // 被通知
    // 收到通知后向目标组件传递数据
    contractUserPassword() {
      this.$bus.$emit(Methods.FN_UPDATE_USER_PASSWORD, this.userPassword);
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    this.$bus.$on(Methods.FN_CONTRACT_USER_PASSWORD, () => this.contractUserPassword());
    this.$bus.$on(Methods.FN_UPDATE_TOP_FOLDER_PATH_COPY, (_topFolderPathCopy) => this.topFolderPathCopy = _topFolderPathCopy);
  },// mounted
}
</script>

<style scoped>
.business-btn-zone {
  text-align: right;
  margin-top: -15px;
}
</style>