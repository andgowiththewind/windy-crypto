<template>
  <div>
    <div>
      <el-card shadow="always">总是显示</el-card>
    </div>
    <p></p>
    <div>
      <el-card shadow="always">总是显示</el-card>
    </div>
    <p></p>
    <div>
      <el-card shadow="always">总是显示</el-card>
    </div>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';

export default {
  name: "ProcessPartAside",
  components: {},
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