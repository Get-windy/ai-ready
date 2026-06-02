<template><div class="page"><van-nav-bar title="修改密码" left-arrow @click-left="$router.back()"/><van-form @submit="onSubmit"><van-field v-model="form.oldPassword" type="password" label="原密码" required/><van-field v-model="form.newPassword" type="password" label="新密码" required/><van-field v-model="form.confirmPassword" type="password" label="确认密码" required/><div style="margin:16px"><van-button round block type="primary" native-type="submit">确认修改</van-button></div></van-form></div></template>
<script setup lang="ts">import { reactive } from 'vue'; import { showToast } from 'vant'
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const onSubmit = () => { if(form.newPassword !== form.confirmPassword) { showToast('两次密码不一致'); return } showToast('密码修改成功') }
</script>
