<template><div class="page"><van-nav-bar title="费用报销" left-arrow @click-left="$router.back()"/><van-tabs v-model:active="tab"><van-tab title="我的报销"><van-cell-group inset v-for="i in 3" :key="i"><van-cell title="差旅费报销" label="2024-01-15" value="¥1,200" is-link/></van-cell-group><van-empty v-if="false" description="暂无报销记录"/></van-tab><van-tab title="新建报销"><van-form @submit="onSubmit"><van-field v-model="form.title" label="报销事由" placeholder="请输入" required/><van-field v-model="form.amount" label="金额" placeholder="请输入金额" type="number" required/><van-field v-model="form.remark" label="备注" placeholder="选填" type="textarea"/><div style="margin:16px"><van-button round block type="primary" native-type="submit">提交</van-button></div></van-form></van-tab></van-tabs></div></template>
<script setup lang="ts">import { ref, reactive } from 'vue'; import { showToast } from 'vant'
const tab = ref(0)
const form = reactive({ title: '', amount: '', remark: '' })
const onSubmit = () => { showToast('提交成功'); form.title = ''; form.amount = ''; form.remark = '' }
</script>
