<template>
  <div class="balance-sheet">
    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card" style="--card-color: #1890ff;">
        <div class="summary-card-title">资产总额</div>
        <div class="summary-card-value">¥{{ formatAmount(balanceSheet.totalAssets) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #faad14;">
        <div class="summary-card-title">负债总额</div>
        <div class="summary-card-value">¥{{ formatAmount(balanceSheet.totalLiabilities) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #52c41a;">
        <div class="summary-card-title">所有者权益</div>
        <div class="summary-card-value">¥{{ formatAmount(balanceSheet.equity) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #722ed1;">
        <div class="summary-card-title">资产负债率</div>
        <div class="summary-card-value">{{ debtRatio }}%</div>
      </div>
    </div>

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
import { ref, reactive, computed } from 'vue'
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

// 资产负债率
const debtRatio = computed(() => {
  if (!balanceSheet.value.totalAssets) return '0.00'
  return ((balanceSheet.value.totalLiabilities / balanceSheet.value.totalAssets) * 100).toFixed(2)
})

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'
}

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

defineExpose({})
</script>

<style scoped>
.balance-sheet {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 统计卡片 */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  flex-shrink: 0;
}

.summary-card {
  background: linear-gradient(135deg, var(--card-color), color-mix(in srgb, var(--card-color) 70%, #fff));
  border-radius: 8px;
  padding: 16px 20px;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.summary-card-title {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.summary-card-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-size: 24px;
  font-weight: 600;
}

.filter-area {
  margin-bottom: 0;
}
</style>
