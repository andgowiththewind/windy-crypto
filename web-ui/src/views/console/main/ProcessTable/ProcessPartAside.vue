<template>
  <div>
    <div>
      <el-card shadow="always">
        <div class="contribution-grid">
          <div
              v-for="(contribution, index) in contributions"
              :key="contribution.date"
              :title="contribution.date + ': ' + contribution.count + ' contributions'"
              class="contribution-day"
              :style="{ backgroundColor: getBackgroundColor(contribution.count) }">
          </div>
        </div>
      </el-card>
    </div>
    <p></p>
    <div>
      <!--折线图-->
      <el-card shadow="always">
        <div style="height: 400px;width: 100%;">
          <div ref="secondIoChart" class="second-io-chart-style"></div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import {Notification, MessageBox, Message, Loading} from 'element-ui';
import * as Methods from '@/config/Methods';
import * as echarts from 'echarts';
import {devConsoleLog} from '@/utils/commonUtils';


export default {
  name: "ProcessPartAside",
  components: {},
  data() {
    return {
      chart: null,
      ioEchartData: [],

      // 模拟一年的日期数据（365天）
      contributions: this.generateContributions(),
    }
  },// data
  methods: {
    generateContributions() {
      const contributions = [];
      const startDate = new Date();
      startDate.setDate(startDate.getDate() - 364); // 从 365 天前开始
      for (let i = 0; i < 365; i++) {
        const date = new Date(startDate);
        date.setDate(startDate.getDate() + i);
        contributions.push({
          date: date.toISOString().slice(0, 10), // 只保留日期部分
          count: Math.floor(Math.random() * 5)  // 随机生成 0 到 4 之间的贡献数
        });
      }
      return contributions;
    },
    getBackgroundColor(count) {
      // 根据贡献数返回不同的背景颜色
      const colors = ['#ebedf0', '#c6e48b', '#7bc96f', '#239a3b', '#196127'];
      return colors[count];
    },
    initChart() {
      this.chart = echarts.init(this.$refs.secondIoChart);
      let option = this.getIoChartOptions();
      this.chart.setOption(option);
      // 自动调整图表大小以适应窗口变化
      window.addEventListener('resize', () => {
        this.chart.resize();
      });
    },
    updateChart() {
      let option = this.getIoChartOptions();
      this.chart.setOption(option);
    },
    getIoChartOptions() {
      // 从`ioEchartData`每个元素中提取key的值组成`xAxisData`
      let xAxisData = this.ioEchartData.map(item => item.key);
      let seriesData = this.ioEchartData.map(item => item.value);
      //
      const option = {
        tooltip: {
          trigger: 'axis',
          position: function (pt) {
            return [pt[0], '10%'];
          }
        },
        title: {
          left: 'center',
          text: 'Large Area Chart',// 标题
        },
        toolbox: {
          feature: {
            // dataZoom: {yAxisIndex: 'none'},
            restore: {},
            saveAsImage: {}
          }
        },
        xAxis: {
          type: 'category',// 表示 X 轴是类别轴，也就是说 X 轴上的值是离散的类别（例如日期、字符串等）
          boundaryGap: false,
          data: xAxisData
        },
        yAxis: {
          type: 'value',
          boundaryGap: [0, '100%']
        },
        // dataZoom: [{type: 'inside', start: 0, end: 10}, {start: 0, end: 10}],
        series: [
          {
            name: 'Fake Data',// 系列名称
            type: 'line',
            symbol: 'none',
            sampling: 'lttb',
            itemStyle: {
              color: 'rgb(255, 70, 131)'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: 'rgb(255, 158, 68)'
                },
                {
                  offset: 1,
                  color: 'rgb(255, 70, 131)'
                }
              ])
            },
            data: seriesData
          }
        ]
      };
      return option;
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    this.initChart();
    this.$bus.$on(Methods.FN_OBJECT_ASSIGN_IO_ECHART_DATA, (data) => {
      this.ioEchartData = data;
      devConsoleLog('this.ioEchartData', this.ioEchartData);
      this.updateChart();
    });
  },// mounted
  beforeDestroy() {
    if (this.chart) {
      window.removeEventListener('resize', this.chart.resize);
      this.chart.dispose();
    }
  },// beforeDestroy
}
</script>

<style scoped>
.contribution-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 14px);
  grid-gap: 4px;
}

.contribution-day {
  width: 14px;
  height: 14px;
  background-color: #ebedf0;
  border-radius: 2px;
}

.second-io-chart-style {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>