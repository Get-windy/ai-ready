<template><div class="page"><van-nav-bar title="更多功能" left-arrow @click-left="$router.back()"/><van-grid :column-num="3"><van-grid-item v-for="item in items" :key="item.name" :icon="item.icon" :text="item.text" :url="item.url"/></van-grid></div></template>
<script setup lang="ts">import { ref } from 'vue'
const items = ref([
  { name: 'stock', icon: 'inbox-o', text: '库存查询', url: '' },
  { name: 'supplier', icon: 'shop-o', text: '供应商', url: '' },
  { name: 'attendance', icon: 'stop-circle-o', text: '考勤', url: '' },
  { name: 'task', icon: 'completed', text: '任务', url: '' },
  { name: 'doc', icon: 'notes-o', text: '文档', url: '' },
  { name: 'meeting', icon: 'video-o', text: '会议', url: '' },
])
</script>
