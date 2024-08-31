<template>
  <div id="app">
    <AppWrapper/>
  </div>
</template>

<script>
import AppWrapper from "@/views/app/AppWrapper.vue";

export default {
  name: 'App',
  components: {AppWrapper},
  methods: {
    consoleEnv() {
      console.log(process.env.VUE_APP_ENV);
    }
  },
  mounted() {
    this.consoleEnv();
  }
}
</script>

<style>
/* 定义抖动动画 */
@keyframes shake {
  0% { transform: translate(1px, 1px) rotate(0deg); }
  10% { transform: translate(-1px, -2px) rotate(-1deg); }
  20% { transform: translate(-3px, 0px) rotate(1deg); }
  30% { transform: translate(3px, 2px) rotate(0deg); }
  40% { transform: translate(1px, -1px) rotate(1deg); }
  50% { transform: translate(-1px, 2px) rotate(-1deg); }
  60% { transform: translate(-3px, 1px) rotate(0deg); }
  70% { transform: translate(3px, 1px) rotate(-1deg); }
  80% { transform: translate(-1px, -1px) rotate(1deg); }
  90% { transform: translate(1px, 2px) rotate(0deg); }
  100% { transform: translate(1px, -2px) rotate(-1deg); }
}

/* 鼠标悬停时触发抖动动画 */
.shake-on-hover:hover {
  animation: shake 0.2s infinite;
}
</style>
