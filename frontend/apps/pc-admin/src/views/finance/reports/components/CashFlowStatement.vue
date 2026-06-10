<template>
  <div class="cash-flow-statement">
    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card" style="--card-color: #1890ff;">
        <div class="summary-card-title">经营活动现金流</div>
        <div class="summary-card-value">¥{{ formatAmount(cashFlow.operatingCashFlow) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #faad14;">
        <div class="summary-card-title">投资活动现金流</div>
        <div class="summary-card-value">¥{{ formatAmount(cashFlow.investingCashFlow) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #52c41a;">
        <div class="summary-card-title">筹资活动现金流</div>
        <div class="summary-card-value">¥{{ formatAmount(cashFlow.financingCashFlow) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #722ed1;">
        <div class="summary-card-title">现金净增加额</div>
        <div class="summary-card-value">¥{{ formatAmount(cashFlow.netIncrease) }}</div>
      </div>
    </div>

    <div class="filter-area">
      <a-form layout="inline">
        <a-form-item label="报表月份">
          <a-month-picker v-model:value="queryParams.month" format="YYYY-MM" value-format="YYYY-MM" size="small" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleGenerate" :loading="loading">生成报表</a-button>
          <a-button style="margin-left: 8px" @click="handleExport">导出</a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-card title="现金流量表">
      <a-descriptions bordered :column="2">
        <a-descriptions-item label="经营活动现金流">¥{{ cashFlow.operatingCashFlow?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="投资活动现金流">¥{{ cashFlow.investingCashFlow?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="筹资活动现金流">¥{{ cashFlow.financingCashFlow?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="现金净增加额">¥{{ cashFlow.netIncrease?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="期初现金余额">¥{{ cashFlow.beginningCash?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="期末现金余额">¥{{ cashFlow.endingCash?.toFixed(2) }}</a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import * as XLSX from 'xlsx'

interface CashFlowStatement {
  operatingCashFlow: number; investingCashFlow: number; financingCashFlow: number; netIncrease: number; beginningCash: number; endingCash: number
}

const cashFlow = ref<CashFlowStatement>({ operatingCashFlow: 0, investingCashFlow: 0, financingCashFlow: 0, netIncrease: 0, beginningCash: 0, endingCash: 0 })
const queryParams = reactive({ month: undefined as string | undefined })
const loading = ref(false)

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'
}

const handleGenerate = async () => {
  if (!queryParams.month) { message.warning('请选择报表月份'); return }
  loading.value = true
  try {
    const res = await request.post('/finance/statement/cash-flow-statement/generate', null, { params: { period: queryParams.month } })
    if ((res as any)?.code === 200 && (res as any)?.data) {
      const { data } = (res as any)
      cashFlow.value = {
        operatingCashFlow: data.operatingCashFlow || 0,
        investingCashFlow: data.investingCashFlow || 0,
        financingCashFlow: data.financingCashFlow || 0,
        netIncrease: data.netIncrease || 0,
        beginningCash: data.beginningCash || 0,
        endingCash: data.endingCash || 0
      }
    }
    message.success('报表生成成功')
  } catch (err: any) {
    message.warning('报表生成接口暂不可用，可尝试生成其他报表')
  } finally { loading.value = false }
}

const handleExport = () => {
  const data = cashFlow.value
  if (!data.operatingCashFlow && !data.endingCash) {
    message.warning('暂无数据可导出，请先生成报表')
    return
  }
  const ws = XLSX.utils.json_to_sheet([
    { '项目': '经营活动现金流', '金额': data.operatingCashFlow },
    { '项目': '投资活动现金流', '金额': data.investingCashFlow },
    { '项目': '筹资活动现金流', '金额': data.financingCashFlow },
    { '项目': '现金净增加额', '金额': data.netIncrease },
    { '项目': '期初现金余额', '金额': data.beginningCash },
    { '项目': '期末现金余额', '金额': data.endingCash }
  ])
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '现金流量表')
  XLSX.writeFile(wb, `现金流量表_${queryParams.month || 'unknown'}.xlsx`)
  message.success('导出成功')
}

function handleParentCreate() { handleAdd() }
function handleAdd() {
  message.info('创建功能由父组件触发')
}

onMounted(() => {
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', handleGenerate)
})

onUnmounted(() => {
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', handleGenerate)
})

defineExpose({})
</script>

<style scoped>
.cash-flow-statement {
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
