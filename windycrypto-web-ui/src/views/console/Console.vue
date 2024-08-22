<template>
  <div>
    <el-container v-cloak class="windy-container">
      <el-aside v-drag style="width: 250px;">
        <Aside></Aside>
      </el-aside>
      <el-container>
        <el-header style="height: 50px;">
          <Header></Header>
        </el-header>
        <el-main style="padding: 5px !important;">
          <Main></Main>
        </el-main>
        <el-footer style="height: 50px;">
          <Footer></Footer>
        </el-footer>
      </el-container>
    </el-container>
  </div>
</template>

<script>
// import {Notification, MessageBox, Message, Loading} from 'element-ui';
// import request from '@/utils/request';
// import * as CommonConsts from '@/config/CommonConsts';
import Aside from '@/views/console/aside/Aside.vue';
import Header from '@/views/console/header/Header.vue';
import Main from '@/views/console/main/Main.vue';
import Footer from '@/views/console/footer/Footer.vue';

export default {
  name: "Console",
  components: {Aside, Header, Main, Footer},
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
[v-cloak] {
  display: none;
}

.el-header, .el-footer {
  background-color: #B3C0D1;
  color: #333;
}

.el-aside {
  background-color: #D3DCE6;
  color: #333;
}

.el-main {
  background-color: #E9EEF3;
  color: #333;
}

.windy-container {
  overflow: hidden;
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  margin: 0px;
}
</style>