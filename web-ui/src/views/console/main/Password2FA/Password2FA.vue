<template>
  <div v-loading="loading">
    <div class="business-btn-zone">
      <el-form @submit.native.prevent :inline="true" size="mini">
        <el-form-item label="">
          <el-input
              :data-title="$t('i18n_1829983741548851200')"
              :data-intro="$t('i18n_1829984384175005696')"
              data-step="8"
              v-model="userPassword" :placeholder="$t('i18n_1828973612548362244')" show-password></el-input>
        </el-form-item>
        <el-form-item label="" prop="isRequireCoverName">
          <el-radio-group
              :data-title="$t('i18n_1829984895003422720')"
              :data-intro="$t('i18n_1829986041721913344')"
              data-step="9"
              v-model="isRequireCoverName" size="mini">
            <el-radio-button label="hide">{{ $t('i18n_1828966632739966976') }}</el-radio-button>
            <el-radio-button label="notHide">{{ $t('i18n_1828966632739966977') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="">
          <el-button size="mini" type="warning" @click="encryptAll" :style="{minWidth:'10vw'}"><span><i class="fa fa-lock"></i>&nbsp;{{ $t('i18n_1828973612548362240') }}</span></el-button>
        </el-form-item>
        <el-form-item label="" prop="ignoreMissingHiddenFilename">
          <el-radio-group v-model="ignoreMissingHiddenFilename" size="mini">
            <el-radio-button label="ignore">{{ $t('i18n_1828982632629800960') }}</el-radio-button>
            <el-radio-button label="notIgnore">{{ $t('i18n_1828982632629800961') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="">
          <el-button size="mini" type="danger" plain @click="decryptAll"><span>{{ $t('i18n_1828973612548362241') }}&nbsp;<i class="fa fa-wrench"></i></span></el-button>
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
      isRequireCoverName: 'notHide',
      ignoreMissingHiddenFilename: 'notIgnore',
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
        isRequireCoverName: this.isRequireCoverName === 'hide',
        ignoreMissingHiddenFilename: this.ignoreMissingHiddenFilename === 'ignore',
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
        isRequireCoverName: this.isRequireCoverName === 'hide',
        ignoreMissingHiddenFilename: this.ignoreMissingHiddenFilename === 'ignore',
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