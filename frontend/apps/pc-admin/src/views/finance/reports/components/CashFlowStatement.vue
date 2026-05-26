<template>
  <div class="cash-flow-statement">
    <div class="filter-area">
      <a-form layout="inline">
        <a-form-item label="报表月份">
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

    <a-card title="现金流量表">
      <a-descriptions
        bordered
        :column="2"
      >
        <a-descriptions-item label="经营活动现金流">
          ¥{{ cashFlow.operatingCashFlow?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="投资活动现金流">
          ¥{{ cashFlow.investingCashFlow?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="筹资活动现金流">
          ¥{{ cashFlow.financingCashFlow?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="现金净增加额">
          ¥{{ cashFlow.netIncrease?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="期初现金余额">
          ¥{{ cashFlow.beginningCash?.toFixed(2) }}
        </a-descriptions-item>
        <a-descriptions-item label="期末现金余额">
          ¥{{ cashFlow.endingCash?.toFixed(2) }}
        </a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'

interface CashFlowStatement {
  operatingCashFlow: number
  investingCashFlow: number
  financingCashFlow: number
  netIncrease: number
  beginningCash: number
  endingCash: number
}

const cashFlow = ref<CashFlowStatement>({
  operatingCashFlow: 0,
  investingCashFlow: 0,
  financingCashFlow: 0,
  netIncrease: 0,
  beginningCash: 0,
  endingCash: 0
})

const queryParams = reactive({
  month: undefined as string | undefined
})

const loading = ref(false)

const handleGenerate = async () => {
  if (!queryParams.month) {
    message.warning('请选择报表月份')
    return
  }
  loading.value = true
  try {
    const response = await fetch(`/api/v1/finance/report/cash-flow?period=${queryParams.month}`)
    const data = await response.json()
    if (data.code === 200) {
      cashFlow.value = data.data || {
        operatingCashFlow: 0,
        investingCashFlow: 0,
        financingCashFlow: 0,
        netIncrease: 0,
        beginningCash: 0,
        endingCash: 0
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
  message.info('导出现金流量表')
}
</script>

<style scoped>
.cash-flow-statement {
  padding: 16px;
}

.filter-area {
  margin-bottom: 16px;
}
</style>