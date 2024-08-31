<template>
  <div>
    <div class="author-info-zone">
      <p>
        <span style="color:#FBDB4A;">© 2024 </span>
        <span style="color:#F3934A;">dororo. </span>
        <span style="color:#EB547D;">All </span>
        <span style="color:#9F6AA7;">rights </span>
        <span style="color:#5476B3;">reserved. </span>
      </p>
    </div>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';

export default {
  name: "AuthorInfo",
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
.author-info-zone {
  font-family: 'Rubik Mono One', sans-serif;
  position: fixed;
  right: 10px;
  bottom: -5px;
}
</style>