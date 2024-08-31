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
      <el-card shadow="always">
        <div ref="chart" style="width: 600px; height: 400px;"></div>
      </el-card>
    </div>
    <p></p>
    <div>
      <!--折线图-->
      <el-card shadow="always">
        <div ref="secondIoChart" style="width: 600px; height: 400px;"></div>
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
  },// methods
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
</style>