<template><div><van-nav-bar title="登录记录" left-arrow @click-left="$router.back()"/><van-cell-group inset><van-cell v-for="item in list" :key="item.id" :title="item.device" :label="item.ip + ' | ' + item.location" :value="item.time"/></van-cell-group><van-empty v-if="!list.length" description="暂无登录记录"/></div></template>
<script setup lang="ts">import { ref } from 'vue'
const list = ref([{id:1,device:'iPhone 15',ip:'192.168.1.100',location:'北京市',time:'2024-01-15 10:30'},{id:2,device:'Chrome浏览器',ip:'192.168.1.101',location:'上海市',time:'2024-01-14 09:20'}])
</script>
