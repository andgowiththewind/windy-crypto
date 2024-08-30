<template>
  <div>
    <div>
      <el-card shadow="always">
        <div ref="chart" style="width: 600px; height: 400px;"></div>
      </el-card>
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
import * as echarts from 'echarts';

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
    // 基于准备好的 dom，初始化 echarts 实例
    const myChart = echarts.init(this.$refs.chart);

    // 指定图表的配置项和数据
    const option = {
      title: {
        text: 'ECharts 示例'
      },
      tooltip: {},
      legend: {
        data: ['销量']
      },
      xAxis: {
        data: ["衬衫", "羊毛衫", "雪纺衫", "裤子", "高跟鞋", "袜子"]
      },
      yAxis: {},
      series: [{
        name: '销量',
        type: 'bar',
        data: [5, 20, 36, 10, 10, 20]
      }]
    };

    // 使用刚指定的配置项和数据显示图表。
    myChart.setOption(option);
  },// mounted
}
</script>

<style scoped>
</style>