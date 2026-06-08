<template>
  <div class="general-ledger-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-records">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ ledgerData.length }}</div>
          <div class="stat-card-label">本期记录</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-debit">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(summaryData?.totalDebit || 0) }}</div>
          <div class="stat-card-label">借方合计</div>
        </div>
        <ArrowUpOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-credit">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(summaryData?.totalCredit || 0) }}</div>
          <div class="stat-card-label">贷方合计</div>
        </div>
        <ArrowDownOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-balance">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(summaryData?.balance || 0) }}</div>
          <div class="stat-card-label">期末余额</div>
        </div>
        <WalletOutlined class="stat-card-icon" />
      </div>
    </div>

    <div class="gl-search-bar">
      <span class="gl-search-label">会计期间：</span>
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
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="columns"
      :data-source="ledgerData"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :show-toolbar="false"
      :show-search="false"
      :show-add="false"
      :show-edit="false"
      :show-delete="false"
      :show-export="false"
      :selectable="true"
      size="small"
      @page-change="handlePageChange"
      @selection-change="handleSelectionChange"
    />

    <!-- 汇总信息 -->
    <div v-if="summaryData" class="summary-bar">
      本期合计：借方 {{ summaryData.totalDebit.toFixed(2) }} |
      贷方 {{ summaryData.totalCredit.toFixed(2) }} |
      余额 {{ summaryData.balance.toFixed(2) }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { FileTextOutlined, SearchOutlined, ArrowUpOutlined, ArrowDownOutlined, WalletOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { accountingApi, type LedgerRecord, type AccountSubject } from '@/api/finance/accounting'

const tableRef = ref()
const loading = ref(false)
const subjectLoading = ref(false)
const ledgerData = ref<LedgerRecord[]>([])
const periodDate = ref(dayjs())
const querySubjectId = ref<number | undefined>(undefined)
const subjectOptions = ref<AccountSubject[]>([])

const columns = computed(() => [
  { title: '日期', field: 'businessDate', width: 100 },
  { title: '凭证号', field: 'voucherNo', width: 130 },
  { title: '科目编码', field: 'subjectCode', width: 100 },
  { title: '科目名称', field: 'subjectName', width: 150 },
  { title: '摘要', field: 'summary', minWidth: 100 },
  { title: '借方金额', field: 'debitAmount', width: 120, align: 'right', formatter: ({ cellValue }) => cellValue > 0 ? cellValue.toFixed(2) : '-' },
  { title: '贷方金额', field: 'creditAmount', width: 120, align: 'right', formatter: ({ cellValue }) => cellValue > 0 ? cellValue.toFixed(2) : '-' },
  { title: '余额', field: 'balance', width: 140, align: 'right', formatter: ({ cellValue, row }) => {
    const absVal = Math.abs(cellValue).toFixed(2)
    const dir = cellValue >= 0 ? '借' : '贷'
    return `${absVal} ${dir}`
  }}
])

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100']
})

const summaryData = computed(() => {
  if (ledgerData.value.length === 0) return null
  return {
    totalDebit: ledgerData.value.reduce((s, r) => s + (r.debitAmount || 0), 0),
    totalCredit: ledgerData.value.reduce((s, r) => s + (r.creditAmount || 0), 0),
    balance: ledgerData.value[ledgerData.value.length - 1]?.balance || 0
  }
})

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

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

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function handleSelectionChange(rows: any[], ids: any[]) {
  // 可以在这里处理选中行的逻辑
  console.log('Selected rows:', rows.length)
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
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-records { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-debit { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-credit { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-balance { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

.gl-search-bar {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.gl-search-label {
  font-size: 13px;
  white-space: nowrap;
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
