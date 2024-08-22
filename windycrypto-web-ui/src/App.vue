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
</style>
