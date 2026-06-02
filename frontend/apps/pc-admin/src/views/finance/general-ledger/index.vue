<template>
  <div class="general-ledger-page">
    <a-card>
      <template #title><FileTextOutlined /> 总账查询</template>
      <template #extra>
        会计期间：
        <a-date-picker
          v-model:value="periodDate"
          picker="month"
          style="width: 140px"
          size="small"
          @change="fetchData"
        />
        <a-select
          v-model:value="querySubjectId"
          placeholder="选择科目"
          style="width: 200px; margin-left: 8px"
          size="small"
          allow-clear
          show-search
          :filter-option="false"
          :loading="subjectLoading"
          @change="fetchData"
          @search="handleSubjectSearch"
        >
          <a-select-option
            v-for="s in subjectOptions"
            :key="s.id"
            :value="s.id"
          >
            {{ s.subjectCode }} {{ s.subjectName }}
          </a-select-option>
        </a-select>
        <a-button size="small" type="primary" style="margin-left: 8px" @click="fetchData">
          <template #icon><SearchOutlined /></template>
          查询
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="ledgerData"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
        size="small"
        bordered
        :scroll="{ x: 1000 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'debitAmount'">
            <span v-if="record.debitAmount > 0" class="debit-amount">
              {{ record.debitAmount.toFixed(2) }}
            </span>
            <span v-else class="text-disabled">-</span>
          </template>
          <template v-if="column.key === 'creditAmount'">
            <span v-if="record.creditAmount > 0" class="credit-amount">
              {{ record.creditAmount.toFixed(2) }}
            </span>
            <span v-else class="text-disabled">-</span>
          </template>
          <template v-if="column.key === 'balance'">
            <span :class="record.balance >= 0 ? 'debit-balance' : 'credit-balance'">
              {{ Math.abs(record.balance).toFixed(2) }}
              <span class="balance-direction">{{ record.balance >= 0 ? '借' : '贷' }}</span>
            </span>
          </template>
          <template v-if="column.key === 'voucherNo'">
            <a @click="viewVoucher(record)">{{ record.voucherNo }}</a>
          </template>
        </template>
      </a-table>

      <!-- 汇总信息 -->
      <div v-if="summaryData" class="summary-bar">
        本期合计：借方 {{ summaryData.totalDebit.toFixed(2) }} |
        贷方 {{ summaryData.totalCredit.toFixed(2) }} |
        余额 {{ summaryData.balance.toFixed(2) }}
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { FileTextOutlined, SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { accountingApi, type LedgerRecord, type AccountSubject } from '@/api/finance/accounting'

const loading = ref(false)
const subjectLoading = ref(false)
const ledgerData = ref<LedgerRecord[]>([])
const periodDate = ref(dayjs())
const querySubjectId = ref<number | undefined>(undefined)
const subjectOptions = ref<AccountSubject[]>([])

const columns = [
  { title: '日期', dataIndex: 'businessDate', key: 'businessDate', width: 100 },
  { title: '凭证号', dataIndex: 'voucherNo', key: 'voucherNo', width: 130 },
  { title: '科目编码', dataIndex: 'subjectCode', key: 'subjectCode', width: 100 },
  { title: '科目名称', dataIndex: 'subjectName', key: 'subjectName', width: 150 },
  { title: '摘要', dataIndex: 'summary', key: 'summary', ellipsis: true },
  { title: '借方金额', dataIndex: 'debitAmount', key: 'debitAmount', width: 120, align: 'right' },
  { title: '贷方金额', dataIndex: 'creditAmount', key: 'creditAmount', width: 120, align: 'right' },
  { title: '余额', dataIndex: 'balance', key: 'balance', width: 140, align: 'right' }
]

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  onChange: (page: number, size: number) => {
    pagination.current = page
    pagination.pageSize = size
    fetchData()
  }
})

const summaryData = computed(() => {
  if (ledgerData.value.length === 0) return null
  return {
    totalDebit: ledgerData.value.reduce((s, r) => s + (r.debitAmount || 0), 0),
    totalCredit: ledgerData.value.reduce((s, r) => s + (r.creditAmount || 0), 0),
    balance: ledgerData.value[ledgerData.value.length - 1]?.balance || 0
  }
})

async function fetchData() {
  loading.value = true
  try {
    const period = periodDate.value?.format('YYYY-MM')
    const res = await accountingApi.queryGeneralLedger({
      subjectId: querySubjectId.value,
      accountingPeriod: period,
      current: pagination.current,
      size: pagination.pageSize
    })
    ledgerData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {
    message.error('获取总账数据失败')
  } finally {
    loading.value = false
  }
}

async function handleSubjectSearch(value: string) {
  if (!value) return
  subjectLoading.value = true
  try {
    const res = await accountingApi.querySubjects({ subjectName: value, current: 1, size: 20 })
    subjectOptions.value = res.data?.records || []
  } catch {
    // ignore
  } finally {
    subjectLoading.value = false
  }
}

function viewVoucher(record: LedgerRecord) {
  // TODO: 跳转到凭证详情
  message.info(`凭证: ${record.voucherNo}`)
}

onMounted(async () => {
  try {
    const res = await accountingApi.getDetailSubjects()
    subjectOptions.value = res.data || []
  } catch { /* ignore */ }
  fetchData()
})
</script>

<style scoped>
.general-ledger-page {
  padding: 16px;
}

.debit-amount {
  color: var(--color-primary, #1890ff);
  font-family: monospace;
}

.credit-amount {
  color: var(--color-danger, #ff4d4f);
  font-family: monospace;
}

.debit-balance {
  color: var(--color-primary, #1890ff);
  font-weight: 500;
  font-family: monospace;
}

.credit-balance {
  color: var(--color-danger, #ff4d4f);
  font-weight: 500;
  font-family: monospace;
}

.balance-direction {
  font-size: 11px;
  margin-left: 4px;
}

.text-disabled {
  color: var(--color-text-disabled, #bbb);
}

.summary-bar {
  margin-top: 12px;
  padding: 8px 16px;
  background: var(--color-bg-layout, #fafafa);
  border: 1px solid var(--color-border-secondary, #f0f0f0);
  border-radius: 4px;
  font-size: 13px;
  font-family: monospace;
}
</style>
