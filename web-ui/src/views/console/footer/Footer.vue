<template>
  <div>
      <AuthorInfo></AuthorInfo>
  </div>
</template>

<script>
// import {Notification, MessageBox, Message, Loading} from 'element-ui';
import AuthorInfo from '@/views/console/footer/AuthorInfo.vue';

export default {
  name: "Footer",
  components: {AuthorInfo},
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