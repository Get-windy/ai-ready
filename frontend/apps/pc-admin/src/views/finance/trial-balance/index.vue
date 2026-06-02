<template>
  <div class="trial-balance-page">
    <a-card>
      <template #title><AuditOutlined /> 试算平衡表</template>
      <template #extra>
        <a-space>
          会计期间：
          <a-date-picker
            v-model:value="periodDate"
            picker="month"
            style="width: 140px"
            size="small"
          />
          <a-button type="primary" size="small" @click="fetchData">
            <SearchOutlined /> 生成
          </a-button>
          <a-button size="small" @click="handleExport">
            <ExportOutlined /> 导出
          </a-button>
        </a-space>
      </template>

      <a-alert
        v-if="isBalanced !== null"
        :type="isBalanced ? 'success' : 'error'"
        :message="isBalanced ? '试算平衡 ✓' : '试算不平衡 ✗'"
        :description="balanceMessage"
        show-icon
        style="margin-bottom: 16px"
      />

      <a-table
        :columns="columns"
        :data-source="tableData"
        :pagination="false"
        :loading="loading"
        row-key="subjectCode"
        size="small"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'subjectName'">
            <span :style="{ paddingLeft: (record._level || 0) * 20 + 'px' }">
              {{ record.subjectCode }} {{ record.subjectName }}
            </span>
          </template>
          <template v-if="column.dataIndex?.includes('Debit') || column.dataIndex?.includes('Credit')">
            <span :class="record[column.dataIndex] !== 0 ? 'amount-value' : 'text-disabled'">
              {{ record[column.dataIndex]?.toFixed(2) || '-' }}
            </span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { AuditOutlined, SearchOutlined, ExportOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { accountingApi, type TrialBalanceItem } from '@/api/finance/accounting'

const loading = ref(false)
const periodDate = ref(dayjs())
const rawItems = ref<TrialBalanceItem[]>([])
const totals = ref({
  totalOpeningDebit: 0,
  totalOpeningCredit: 0,
  totalPeriodDebit: 0,
  totalPeriodCredit: 0,
  totalClosingDebit: 0,
  totalClosingCredit: 0
})

const columns = [
  { title: '科目', key: 'subjectName', dataIndex: 'subjectName', width: 220 },
  { title: '期初借方', dataIndex: 'openingDebit', key: 'openingDebit', width: 130, align: 'right' },
  { title: '期初贷方', dataIndex: 'openingCredit', key: 'openingCredit', width: 130, align: 'right' },
  { title: '本期借方', dataIndex: 'periodDebit', key: 'periodDebit', width: 130, align: 'right' },
  { title: '本期贷方', dataIndex: 'periodCredit', key: 'periodCredit', width: 130, align: 'right' },
  { title: '期末借方', dataIndex: 'closingDebit', key: 'closingDebit', width: 130, align: 'right' },
  { title: '期末贷方', dataIndex: 'closingCredit', key: 'closingCredit', width: 130, align: 'right' }
]

const tableData = computed(() => {
  const data = rawItems.value.map((item, idx) => ({ ...item, _level: 0, _key: idx }))
  // 添加汇总行
  data.push({
    subjectCode: '',
    subjectName: '合计',
    openingDebit: totals.value.totalOpeningDebit,
    openingCredit: totals.value.totalOpeningCredit,
    periodDebit: totals.value.totalPeriodDebit,
    periodCredit: totals.value.totalPeriodCredit,
    closingDebit: totals.value.totalClosingDebit,
    closingCredit: totals.value.totalClosingCredit,
    _level: 0,
    _key: -1
  } as any)
  return data
})

const isBalanced = computed(() => {
  if (rawItems.value.length === 0) return null
  return (
    Math.abs(totals.value.totalOpeningDebit - totals.value.totalOpeningCredit) < 0.01 &&
    Math.abs(totals.value.totalPeriodDebit - totals.value.totalPeriodCredit) < 0.01 &&
    Math.abs(totals.value.totalClosingDebit - totals.value.totalClosingCredit) < 0.01
  )
})

const balanceMessage = computed(() => {
  if (isBalanced.value === null) return ''
  if (isBalanced.value) {
    return '所有科目借贷平衡，报表数据正确。'
  }
  return `期初差额: ${(totals.value.totalOpeningDebit - totals.value.totalOpeningCredit).toFixed(2)} | ` +
    `本期差额: ${(totals.value.totalPeriodDebit - totals.value.totalPeriodCredit).toFixed(2)} | ` +
    `期末差额: ${(totals.value.totalClosingDebit - totals.value.totalClosingCredit).toFixed(2)}`
})

async function fetchData() {
  loading.value = true
  try {
    const period = periodDate.value?.format('YYYY-MM')
    const res = await accountingApi.getTrialBalance({ accountingPeriod: period })
    rawItems.value = res.data?.items || []
    totals.value = {
      totalOpeningDebit: res.data?.totalOpeningDebit || 0,
      totalOpeningCredit: res.data?.totalOpeningCredit || 0,
      totalPeriodDebit: res.data?.totalPeriodDebit || 0,
      totalPeriodCredit: res.data?.totalPeriodCredit || 0,
      totalClosingDebit: res.data?.totalClosingDebit || 0,
      totalClosingCredit: res.data?.totalClosingCredit || 0
    }
  } catch {
    message.error('获取试算平衡表失败')
  } finally {
    loading.value = false
  }
}

function handleExport() {
  message.success('导出功能开发中')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.trial-balance-page {
  padding: 16px;
}

.amount-value {
  font-family: monospace;
  font-weight: 500;
}

.text-disabled {
  color: var(--color-text-disabled, #bbb);
}
</style>
