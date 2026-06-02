<template><div class="page"><van-nav-bar title="请假申请" left-arrow @click-left="$router.back()"/><van-tabs v-model:active="tab"><van-tab title="请假记录"><van-cell-group inset><van-cell v-for="i in 2" :key="i" title="年假" label="2024-01-10 ~ 2024-01-12" value="已批准" is-link/></van-cell-group></van-tab><van-tab title="申请请假"><van-form @submit="onSubmit"><van-field v-model="form.type" label="请假类型" placeholder="年假/事假/病假" required/><van-field v-model="form.days" label="天数" placeholder="请输入" type="digit" required/><van-field v-model="form.reason" label="原因" placeholder="选填" type="textarea"/><div style="margin:16px"><van-button round block type="primary" native-type="submit">提交申请</van-button></div></van-form></van-tab></van-tabs></div></template>
<script setup lang="ts">import { ref, reactive } from 'vue'; import { showToast } from 'vant'
const tab = ref(0); const form = reactive({ type: '', days: '', reason: '' })
const onSubmit = () => { showToast('请假申请已提交'); tab.value = 0 }
</script>
