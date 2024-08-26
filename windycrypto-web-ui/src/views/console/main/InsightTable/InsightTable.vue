<template>
  <div>
    <h1>InsightTable</h1>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import * as Methods from '@/config/Methods';


export default {
  name: "InsightTable",
  components: {},
  data() {
    return {
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