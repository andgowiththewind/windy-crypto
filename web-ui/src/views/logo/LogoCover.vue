<template>
  <div class="login-container">
    <el-image
        title="windy crypto! Click to enter~"
        style="width: 180px; height: 180px"
        :src="logoRight"
        v-cloak
        @click="unCoverLogo">
    </el-image>
    <AuthorInfo></AuthorInfo>
  </div>
</template>

<script>
import logo_right from '@/assets/logo_right.png';
import AuthorInfo from '@/views/console/footer/AuthorInfo.vue';

export default {
  name: "LogoCover",
  components: {AuthorInfo},
  data() {
    return {
      logoRight: logo_right
    }
  },// data
  methods: {
    unCoverLogo() {
      this.$emit('unCoverLogo');
    }
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
[v-cloak] {
  display: none;
}

.login-container {
  overflow: hidden;
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  margin: 0px;
  font-family: 'Rubik Mono One', sans-serif;
  background: linear-gradient(to left, #0b44d0, #dca05e);
  /* Flexbox for centering */
  display: flex;
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */
}
</style>