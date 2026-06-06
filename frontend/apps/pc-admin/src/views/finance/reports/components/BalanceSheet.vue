<template>
  <div class="balance-sheet">
    <div class="filter-area">
      <a-form layout="inline">
        <a-form-item label="报表日期">
          <a-month-picker v-model:value="queryParams.month" format="YYYY-MM" value-format="YYYY-MM" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleGenerate" :loading="loading">生成报表</a-button>
          <a-button style="margin-left: 8px" @click="handleExport">导出</a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-card title="资产负债表">
      <a-descriptions bordered :column="2">
        <a-descriptions-item label="资产总额">¥{{ balanceSheet.totalAssets?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="负债总额">¥{{ balanceSheet.totalLiabilities?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="流动资产">¥{{ balanceSheet.currentAssets?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="流动负债">¥{{ balanceSheet.currentLiabilities?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="固定资产">¥{{ balanceSheet.fixedAssets?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="非流动负债">¥{{ balanceSheet.nonCurrentLiabilities?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="所有者权益">¥{{ balanceSheet.equity?.toFixed(2) }}</a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

interface BalanceSheet {
  totalAssets: number
  totalLiabilities: number
  currentAssets: number
  currentLiabilities: number
  fixedAssets: number
  nonCurrentLiabilities: number
  equity: number
}

const balanceSheet = ref<BalanceSheet>({ totalAssets: 0, totalLiabilities: 0, currentAssets: 0, currentLiabilities: 0, fixedAssets: 0, nonCurrentLiabilities: 0, equity: 0 })
const queryParams = reactive({ month: undefined as string | undefined })
const loading = ref(false)

const handleGenerate = async () => {
  if (!queryParams.month) { message.warning('请选择报表日期'); return }
  loading.value = true
  try {
    const res = await request.get('/finance/report/balance-sheet', { params: { period: queryParams.month } })
    balanceSheet.value = (res as any)?.data || balanceSheet.value
    message.success('报表生成成功')
  } catch (err: any) {
    message.error(err?.message || '报表生成失败')
  } finally { loading.value = false }
}

const handleExport = () => {
  message.info('导出功能开发中')
}
</script>

<style scoped>
.balance-sheet { padding: 16px; }
.filter-area { margin-bottom: 16px; }
</style>
