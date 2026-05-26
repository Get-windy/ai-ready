<template>
  <div class="balance-sheet">
    <div class="filter-area">
      <a-form layout="inline">
        <a-form-item label="报表日期">
          <a-month-picker
            v-model:value="queryParams.month"
            format="YYYY-MM"
            value-format="YYYY-MM"
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            @click="handleGenerate"
          >
            生成报表
          </a-button>
          <a-button
            style="margin-left: 8px"
            @click="handleExport"
          >
            导出
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-card title="资产负债表">
      <a-descriptions
        bordered
        :column="2"
      >
        <a-descriptions-item label="资产总额">
          ¥{{ balanceSheet.totalAssets?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="负债总额">
          ¥{{ balanceSheet.totalLiabilities?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="流动资产">
          ¥{{ balanceSheet.currentAssets?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="流动负债">
          ¥{{ balanceSheet.currentLiabilities?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="固定资产">
          ¥{{ balanceSheet.fixedAssets?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="非流动负债">
          ¥{{ balanceSheet.nonCurrentLiabilities?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="所有者权益">
          ¥{{ balanceSheet.equity?.toFixed(2) }}
        </a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'

interface BalanceSheet {
  totalAssets: number
  totalLiabilities: number
  currentAssets: number
  currentLiabilities: number
  fixedAssets: number
  nonCurrentLiabilities: number
  equity: number
}

const balanceSheet = ref<BalanceSheet>({
  totalAssets: 0,
  totalLiabilities: 0,
  currentAssets: 0,
  currentLiabilities: 0,
  fixedAssets: 0,
  nonCurrentLiabilities: 0,
  equity: 0
})

const queryParams = reactive({
  month: undefined as string | undefined
})

const loading = ref(false)

const handleGenerate = async () => {
  if (!queryParams.month) {
    message.warning('请选择报表日期')
    return
  }
  loading.value = true
  try {
    const response = await fetch(`/api/v1/finance/report/balance-sheet?period=${queryParams.month}`)
    const data = await response.json()
    if (data.code === 200) {
      balanceSheet.value = data.data || {
        totalAssets: 0,
        totalLiabilities: 0,
        currentAssets: 0,
        currentLiabilities: 0,
        fixedAssets: 0,
        nonCurrentLiabilities: 0,
        equity: 0
      }
      message.success('报表生成成功')
    } else {
      message.error(data.message || '报表生成失败')
    }
  } catch (error) {
    message.error('报表生成失败')
  } finally {
    loading.value = false
  }
}

const handleExport = () => {
  message.info('导出资产负债表')
}
</script>

<style scoped>
.balance-sheet {
  padding: 16px;
}

.filter-area {
  margin-bottom: 16px;
}
</style>