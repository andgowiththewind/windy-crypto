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
import * as echarts from 'echarts';

export default {
  name: "ProcessPartAside",
  components: {},
  data() {
    return {
      chart: null,
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
      let base = +new Date(1968, 9, 3);
      let oneDay = 24 * 3600 * 1000;
      let date = [];
      let data = [Math.random() * 300];
      for (let i = 1; i < 2000; i++) {
        var now = new Date((base += oneDay));
        date.push([now.getFullYear(), now.getMonth() + 1, now.getDate()].join('/'));
        data.push(Math.round((Math.random() - 0.5) * 20 + data[i - 1]));
      }
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
            dataZoom: {
              yAxisIndex: 'none'
            },
            restore: {},
            saveAsImage: {}
          }
        },
        xAxis: {
          type: 'category',// 表示 X 轴是类别轴，也就是说 X 轴上的值是离散的类别（例如日期、字符串等）
          boundaryGap: false,
          data: date
        },
        yAxis: {
          type: 'value',
          boundaryGap: [0, '100%']
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 10
          },
          {
            start: 0,
            end: 10
          }
        ],
        series: [
          {
            name: 'Fake Data',
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
            data: data
          }
        ]
      };

      this.chart.setOption(option);


      // 自动调整图表大小以适应窗口变化
      window.addEventListener('resize', () => {
        this.chart.resize();
      });
    },
  },// methods
  watch: {
    // 'searchParamVo.topPath': {handler: function (val, oldVal) {if (val) {this.searchParamVo.topPath = val;this.searchParamVo.topPath = '';}}, deep: true},
  },// watch
  mounted() {
    this.initChart();
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