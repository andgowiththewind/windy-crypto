<template>
  <div>
    <el-table
        size="mini"
        :fit="true"
        :stripe="true"
        :border="true"
        :row-style="rowStyleFn"
        :cell-style="cellStyleFn"
        :header-cell-style="headerCellStyleFn"
        :max-height="900"
        :data="processTableData"
        style="width: 100%;min-height:200px;">
      <el-table-column label="ID" prop="id" width="170" :show-overflow-tooltip="true"></el-table-column>
      <!--进度-->
      <el-table-column :label="$t('i18n_1827913996980850688')" prop="percentage">
        <template v-slot="scope">
          <div v-if="scope.row.code>=20 && scope.row.code<=40">
            <el-progress v-if="scope.row.percentage" :percentage="scope.row.percentage||0"></el-progress>
          </div>
          <span v-else style="opacity: 25%">/</span>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty
            class="shake-on-hover"
            :description="$t('i18n_1829935506197274624')">
          <template #image>
            <img :src="logoLeft" alt="Custom Image"/>
          </template>
        </el-empty>
      </template>
    </el-table>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import * as Methods from '@/config/Methods';
import logoLeft from '@/assets/logo_left.png';

export default {
  name: "ProcessPartMain",
  components: {},
  data() {
    return {
      processTableData: [],
      logoLeft: logoLeft,
    }
  },// data
  methods: {
    rowStyleFn({row, rowIndex}) {
      return {backgroundColor: (rowIndex % 2 === 0 ? '#f0f9eb' : '#b7a476'), color: 'black'};
    },
    cellStyleFn({row, column, rowIndex, columnIndex}) {
      if (['name', 'absPath'].includes(column.property)) {
        return {textAlign: 'left'};
      }
      if (['status', 'percentage', 'readableFileSize', 'extName'].includes(column.property)) {
        return {textAlign: 'center'};
      }
    },
    headerCellStyleFn({row, column, rowIndex, columnIndex}) {
      return {backgroundColor: '#01847f', textAlign: 'center', color: 'yellow', fontSize: '10px'};
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    this.$bus.$on(Methods.FN_OBJECT_ASSIGN_PROCESS_TABLE_DATA, (data) => this.processTableData = data);
  },// mounted
}
</script>

<style scoped>
</style>