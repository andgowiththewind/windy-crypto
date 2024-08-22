<template>
  <div v-loading="loading">
    <el-button type="warning" size="mini" style="width: 100%;height: 50px;" @click="dialogVo.show=true">
      <i>{{ $t('i18n.aside.inputPath') }}</i>
    </el-button>
    <el-tree :data="treeVo.data" :props="treeVo.defaultProps" @node-click="handleNodeClick"></el-tree>
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
        data: [{
          label: '一级 1',
          children: [{
            label: '二级 1-1',
            children: [{
              label: '三级 1-1-1'
            }]
          }]
        }, {
          label: '一级 2',
          children: [{
            label: '二级 2-1',
            children: [{
              label: '三级 2-1-1'
            }]
          }, {
            label: '二级 2-2',
            children: [{
              label: '三级 2-2-1'
            }]
          }]
        }, {
          label: '一级 3',
          children: [{
            label: '二级 3-1',
            children: [{
              label: '三级 3-1-1'
            }]
          }, {
            label: '二级 3-2',
            children: [{
              label: '三级 3-2-1'
            }]
          }]
        }],
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