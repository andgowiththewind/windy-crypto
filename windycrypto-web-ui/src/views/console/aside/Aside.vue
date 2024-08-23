<template>
  <div v-loading="loading">
    <el-button type="warning" size="mini" style="width: 100%;height: 50px;" @click="dialogVo.show=true">
      <i>{{ $t('i18n.aside.inputPath') }}</i>
    </el-button>
    <el-tree
        :data="treeVo.data"
        :props="treeVo.defaultProps"
        node-key="id"
        :default-expanded-keys="treeVo.defaultExpandedKeys"
        @node-click="handleNodeClick">
    </el-tree>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import * as Methods from '@/config/Methods';
import {devConsoleLog} from '@/utils/commonUtils';
import {getTreeData} from '@/api/asideApi';

export default {
  name: "Aside",
  components: {},
  data() {
    return {
      loading: false,
      dialogVo: {
        show: false,
      },
      treeVo: {
        data: [],
        defaultExpandedKeys: [1, 2],
        defaultProps: {
          children: 'children',
          label: 'label'
        }
      },
    }
  },// data
  methods: {
    handleNodeClick(data) {
      devConsoleLog('handleNodeClick:', data);

    },
    getTreeData(topFolderPath) {
      devConsoleLog('目录地址框回车事件ON:', topFolderPath);
      this.loading = true;
      getTreeData({path: topFolderPath}).then(res => {
        this.treeVo.data = res.data;
      }).finally(() => this.loading = false).catch();
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    this.$bus.$on(Methods.FN_GET_TREE_DATA, (topFolderPath) => this.getTreeData(topFolderPath));
  },// mounted
}
</script>

<style scoped>
</style>