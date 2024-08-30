<template>
  <div>
    <div class="view-progress-btn" @click="drawerVo.show=true">
      <el-tooltip effect="dark" :content="$t('i18n_1829433519491911680')" placement="top">
        <span><i class="fa fa-table"></i></span>
      </el-tooltip>
    </div>
    <div>
      <el-drawer
          :size="'80%'"
          :with-header="false"
          title="process-table"
          :visible.sync="drawerVo.show"
          :direction="drawerVo.direction">
        <div>
          <el-container v-cloak class="windy-container">
            <el-aside width="35vw" style="padding: 4px;">
              <ProcessPartAside></ProcessPartAside>
            </el-aside>
            <el-container>
              <el-header style="padding: 0px;height: 30px;">
                <ProcessPartHeader></ProcessPartHeader>
              </el-header>
              <el-main style="padding: 1px;">
                <ProcessPartMain></ProcessPartMain>
              </el-main>
              <el-footer style="padding: 1px;height: 30px;">
                <ProcessPartFooter></ProcessPartFooter>
              </el-footer>
            </el-container>
          </el-container>
        </div>
      </el-drawer>
    </div>
  </div>
</template>

<script>
// import {Notification, MessageBox, Message, Loading} from 'element-ui';
import ProcessPartMain from '@/views/console/main/ProcessTable/ProcessPartMain.vue';
import ProcessPartHeader from '@/views/console/main/ProcessTable/ProcessPartHeader.vue';
import ProcessPartAside from '@/views/console/main/ProcessTable/ProcessPartAside.vue';
import ProcessPartFooter from '@/views/console/main/ProcessTable/ProcessPartFooter.vue';

export default {
  name: "ProcessTable",
  components: {
    ProcessPartMain, ProcessPartHeader, ProcessPartAside, ProcessPartFooter
  },
  data() {
    return {
      drawerVo: {
        show: false,
        direction: 'btt',
      }
    }
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
.view-progress-btn {
  opacity: 65%;
  color: rgb(0, 133, 125);
  position: fixed;
  left: 10px;
  bottom: 10px;
  font-size: 70px;
}

.view-progress-btn:hover {
  opacity: 100%;
  color: rgb(0, 133, 125);
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
</style>