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
    </el-table>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import * as Methods from '@/config/Methods';
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
  methods: {},// methods
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