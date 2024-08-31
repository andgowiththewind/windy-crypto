<template>
  <div>
    <ConditionalPagination></ConditionalPagination>
    <Password2FA></Password2FA>
    <InsightTable></InsightTable>
    <ProcessTable></ProcessTable>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import ConditionalPagination from '@/views/console/main/ConditionalPagination/ConditionalPagination.vue';
import InsightTable from '@/views/console/main/InsightTable/InsightTable.vue';
import Password2FA from '@/views/console/main/Password2FA/Password2FA.vue';
import ProcessTable from '@/views/console/main/ProcessTable/index.vue';


export default {
  name: "Main",
  components: {
    ConditionalPagination,
    InsightTable,
    Password2FA,
    ProcessTable
  },
  data() {
    return {}
  },// data
  methods: {},// methods
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