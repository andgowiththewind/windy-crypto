<template>
  <div v-loading="loading">
    <!--form-->
    <div style="text-align: right;">
      <el-form @submit.native.prevent size="mini" :inline="true" :model="payload" ref="payloadRef">
        <el-form-item :label="$t('i18n_1826861420642439168')" prop="payloadParamsScope">
          <el-radio-group v-model="payload.params.scope">
            <el-radio-button label="notEncrypted">{{ $t('i18n_1826862190838288384') }}</el-radio-button>
            <el-radio-button label="encrypted">{{ $t('i18n_1826862378374008832') }}</el-radio-button>
            <el-radio-button label="all">{{ $t('i18n_1826862378374008833') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('i18n_1826863045788438528')" prop="payloadModelName">
          <el-input :style="{minWidth:'215px'}" v-model="payload.model.name" :placeholder="$t('i18n_1826863045788438529')"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button-group>
            <el-button type="primary" icon="el-icon-arrow-left" style="width: 15vw;" @click="submitConditionPagingQuery">{{ $t('i18n_1826863045788438530') }}</el-button>
            <el-button type="primary" plain style="width: 5vw;" @click="resetConditionalForm">{{ $t('i18n_1826863045788438531') }}<i class="el-icon-arrow-right el-icon--right"></i></el-button>
          </el-button-group>
        </el-form-item>
      </el-form>
    </div>
    <!--pagination-->
    <div style="text-align: right; margin-top: -15px;" v-cloak>
      <el-form @submit.native.prevent size="mini">
        <el-form-item label="">
          <el-pagination
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
              :current-page.sync="payload.page.pageNum"
              :page-size="payload.page.pageSize"
              :total="payload.page.total"
              layout="total, prev, pager, next, jumper">
          </el-pagination>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import {devConsoleLog} from "@/utils/commonUtils";
import {getInsightTableData} from "@/api/conditionalPaginationApi";

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
  methods: {
    resetConditionalForm() {
    },
    submitConditionPagingQuery() {
      devConsoleLog('submitConditionPagingQuery');
      getInsightTableData(this.payload).then(response => {
        devConsoleLog(response);
        // this.payload.page.total = response.data.total;
      }).catch(error => {
        devConsoleLog(error);
      });
    },
    handleSizeChange(val) {
      this.payload.page.pageSize = val;
      this.submitConditionPagingQuery();
    },
    handleCurrentChange(val) {
      this.payload.page.pageNum = val;
      this.submitConditionPagingQuery();
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
</style>