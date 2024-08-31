<template>
  <div>
    <div>
      <!--热力图-->
      <el-card shadow="always">
        <div class="contribution-grid">
          <div v-for="(contribution, index) in gridList" :key="contribution.id" :title="contribution.title" class="contribution-day" :style="{ backgroundColor: contribution.color }"></div>
        </div>
      </el-card>
    </div>
    <p></p>
    <div>
      <!--折线图-->
      <el-card shadow="always">
        <div style="height: 400px;width: 100%;">
          <div ref="secondIoChart" class="second-io-chart-style"></div>
          <!--<div v-show="ioEchartData.length>0" ref="secondIoChart" class="second-io-chart-style"></div>-->
          <!--<div v-show="ioEchartData.length==0" class="empty-placeholder"><el-empty :description="$t('i18n_1829932998070595584')"/></div>-->
        </div>
      </el-card>
    </div>
    <p></p>
    <div>
      <!--饼图-->
      <el-card shadow="always">
        <div style="height: 200px;width: 100%;">
          <div ref="pieChart" class="pie-chart-style"></div>
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
      gridList: this.generateContributions(),
      contributions: this.generateContributions(),// 模拟一年的日期数据（365天）
      pieVo: {waiting: 0, io: 0, free: 0},
      pieChart: null,
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
          id: i + 1,
          title: `${date.toISOString().slice(0, 10)}：no query results`,
          color: '#ebedf0',
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
        grid: {
          left: '1%',  // 默认是'10%'，增大这个值以增加左边距
          right: '2%',
          bottom: '2%',
          containLabel: true
        },
        tooltip: {
          trigger: 'axis',
          formatter: function (params) {
            // params 是一个数组，包含当前鼠标悬浮点的所有系列的数据
            let tooltipContent = '';
            params.forEach(item => {
              // 查找与当前点匹配的ioEchartData元素
              let dataItem = this.ioEchartData.find(d => d.key === item.name);
              if (dataItem) {
                tooltipContent += `[${dataItem.datetimeStr}]---${dataItem.label}<br/>`;
              }
            });
            return tooltipContent;
          }.bind(this),  // 绑定当前this上下文
          position: function (pt) {
            return [pt[0], '10%'];
          }
        },
        title: {
          left: 'center',
          text: this.$t('i18n_1829888844594548736'),// 标题
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
    updatePieChart() {
      this.pieChart = echarts.init(this.$refs.pieChart);
      let option = this.getPieChartOptions();
      this.pieChart.setOption(option);
    },
    getPieChartOptions() {
      let option = {
        tooltip: {
          trigger: 'item'
        },
        legend: {
          top: '5%',
          left: 'center'
        },
        series: [
          {
            name: 'status',
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 40,
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: [
              {value: this.pieVo.free, name: 'FREE'},
              {value: this.pieVo.io, name: 'IO'},
              {value: this.pieVo.waiting, name: 'WAITING'}
            ]
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
    this.updatePieChart();
    this.$bus.$on(Methods.FN_OBJECT_ASSIGN_IO_ECHART_DATA, (data) => {
      this.ioEchartData = data;
      // devConsoleLog('this.ioEchartData', this.ioEchartData);
      this.updateChart();
    });
    this.$bus.$on(Methods.FN_OBJECT_ASSIGN_GRID_LIST, (data) => {
      this.gridList = data;
    });
    this.$bus.$on(Methods.FN_OBJECT_ASSIGN_PIE, (data) => {

      this.updatePieChart();
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

.pie-chart-style {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>