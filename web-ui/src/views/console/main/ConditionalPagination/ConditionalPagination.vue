<template>
  <div v-loading="loading">
    <!--form-->
    <div style="text-align: right;">
      <el-form @submit.native.prevent size="mini" :inline="true" :model="payload" ref="payloadRef">
        <el-form-item :label="$t('i18n_1826861420642439168')" prop="payloadParamsScope">
          <el-radio-group v-model="payload.params.searchScope">
            <el-radio-button label="unencrypted">{{ $t('i18n_1826862190838288384') }}</el-radio-button>
            <el-radio-button label="encrypted">{{ $t('i18n_1826862378374008832') }}</el-radio-button>
            <el-radio-button label="all">{{ $t('i18n_1826862378374008833') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('i18n_1826863045788438528')" prop="payloadModelName">
          <el-input :style="{minWidth:'215px'}" v-model="payload.model.name" :placeholder="$t('i18n_1826863045788438529')"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button-group>
            <el-button
                :data-title="$t('i18n_1826863045788438530')"
                :data-intro="$t('i18n_1829981599945715712')"
                data-step="6"
                type="primary"
                icon="el-icon-arrow-left"
                style="width: 15vw;"
                @click="submitConditionPagingQuery">
              {{ $t('i18n_1826863045788438530') }}
            </el-button>
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
              layout="total, sizes,prev, pager, next, jumper">
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
import * as Methods from '@/config/Methods';

export default {
  name: "ConditionalPagination",
  components: {},
  data() {
    return {
      loading: false,
      payload: {
        model: {name: ''},
        page: {pageNum: 1, pageSize: 30, total: 0},
        params: {searchScope: 'all', path: ''},
      },
    }
  },// data
  methods: {
    resetConditionalForm() {
      this.payload.params.searchScope = 'all';
      this.payload.model.name = '';
      Message.success(this.$t('i18n_1829773338935959552'));
    },
    submitConditionPagingQuery() {
      devConsoleLog('submitConditionPagingQuery');
      devConsoleLog('接收合并之前的payload', this.payload);
      // 激活其他多个组件的合约,要求其向当前组件传递数据更新payload
      this.$bus.$emit(Methods.FN_CONTRACT_PAYLOAD);
/*      if (this.payload.params.path) {
        devConsoleLog('路径不为空，不允许查询');
        return false;
      }*/
      this.loading = true;
      getInsightTableData(this.payload).then(response => {
        devConsoleLog(response);
        this.payload.page.total = response.data.total;
        this.$bus.$emit(Methods.FN_UPDATE_INSIGHT_TABLE_DATA, response.data.list);
      }).finally(() => this.loading = false).catch(error => devConsoleLog(error));
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
    this.$bus.$on(Methods.FN_UPDATE_CONDITION_PAGING_QUERY_PAYLOAD, (payloadPart) => {
      devConsoleLog('接收其他组件的部分参数合并入最终payload', payloadPart);
      let mergedObject = {
        ...this.payload,
        params: {
          ...this.payload.params,
          ...payloadPart.params
        }
      };
      this.payload = mergedObject;
      devConsoleLog('更新后的payload', this.payload);
    });


    this.submitConditionPagingQuery();

  },// mounted
}
</script>

<style scoped>
</style>