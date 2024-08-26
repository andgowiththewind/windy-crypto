<template>
  <div>
    <!--table-->
    <el-table
        size="mini"
        :fit="true"
        :stripe="true"
        :border="true"
        :row-style="rowStyleFnVal"
        :cell-style="cellStyleFnVal"
        :header-cell-style="headerCellStyleFnVal"
        :data="tableData"
        style="width: 100%;min-height:500px;">
      <el-table-column label="ID" width="170" prop="id" :show-overflow-tooltip="true"></el-table-column>
      <el-table-column :label="$t('i18n_1826863045788438528')" width="222" prop="name" :show-overflow-tooltip="true"></el-table-column>
      <el-table-column :label="$t('i18n_1827913996976656384')" width="100" prop="sizeLabel" :show-overflow-tooltip="true"></el-table-column>
      <el-table-column :label="$t('i18n_1827913996976656385')" width="250" prop="latestMsg" :show-overflow-tooltip="true"></el-table-column>
      <!--进度-->
      <el-table-column :label="$t('i18n_1827913996980850688')" width="350" prop="percentage">
        <template v-slot="scope">
          <div v-if="scope.row.code>=20 && scope.row.code<=40">
            <el-progress v-if="scope.row.percentage" :percentage="scope.row.percentage||0"></el-progress>
          </div>
          <span v-else style="opacity: 25%">/</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('i18n_1827913996980850689')" :show-overflow-tooltip="true">
        <template v-slot="scope">
          <span @click="absPathClickCopy(scope.row.absPath)">{{ scope.row.absPath }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import * as Methods from '@/config/Methods';
import {devConsoleLog} from "@/utils/commonUtils";
import {rowStyleFn, cellStyleFn, headerCellStyleFn} from "@/api/insightTableApi";

export default {
  name: "InsightTable",
  components: {},
  data() {
    return {
      rowStyleFnVal: rowStyleFn,
      cellStyleFnVal: cellStyleFn,
      headerCellStyleFnVal: headerCellStyleFn,
      tableData: [],
    }
  },// data
  methods: {
    absPathClickCopy(absPath) {
      devConsoleLog('absPathClickCopy', absPath);
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    // 收到tableData
    this.$bus.$on(Methods.FN_UPDATE_INSIGHT_TABLE_DATA, (data) => {
      this.tableData = data;
    });


  },// mounted
}
</script>

<style scoped>
</style>