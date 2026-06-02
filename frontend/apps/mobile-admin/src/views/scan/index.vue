<template>
  <div class="scan-page">
    <div class="scan-header"><van-nav-bar title="扫码" left-arrow @click-left="$router.back()" /></div>
    <div class="scan-body">
      <div class="scanner-placeholder">
        <van-icon name="scan" size="64" color="#1989fa" />
        <p>将二维码放入框内即可自动扫描</p>
        <p class="tip">支持商品码、库位码、单据码</p>
      </div>
      <van-field v-model="manualInput" placeholder="或手动输入条码" clearable @keyup.enter="handleSubmit">
        <template #button><van-button size="small" type="primary" @click="handleSubmit">查询</van-button></template>
      </van-field>
    </div>
  </div>
</template>
<script setup lang="ts">import { ref } from 'vue'; import { showToast } from 'vant'
const manualInput = ref('')
const handleSubmit = () => { if (manualInput.value) { showToast('已查询: ' + manualInput.value); manualInput.value = '' } else showToast('请输入条码') }
</script>
<style scoped>.scan-page { min-height: 100vh; background: #f5f5f5 } .scan-body { padding: 40px 16px; text-align: center } .scanner-placeholder { background: #fff; border-radius: 8px; padding: 60px 20px; margin-bottom: 16px } .scanner-placeholder p { color: #666; margin-top: 12px } .tip { font-size: 12px; color: #999 }</style>
