<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <a-breadcrumb>
              <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
              <a-breadcrumb-item>财务管理</a-breadcrumb-item>
              <a-breadcrumb-item>费用报销</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header__title">费用报销</h2>
          </div>
          <div class="page-header__right">
            <a-badge :status="loading ? 'processing' : 'success'" />
            <a-button size="small" :loading="loading" @click="fetchData">
              <template #icon><ReloadOutlined /></template>刷新
            </a-button>
          </div>
        </div>
      </template>

      <div class="search-area">
        <a-space :size="12" wrap>
          <span class="search-item">
            <label>报销编号</label>
            <a-input
              v-model:value="searchParams.reimburseNo"
              placeholder="请输入报销编号"
              style="width: 160px"
              allow-clear
              @pressEnter="handleSearch"
            />
          </span>
          <span class="search-item">
            <label>报销人</label>
            <a-input
              v-model:value="searchParams.applicantName"
              placeholder="请输入报销人"
              style="width: 140px"
              allow-clear
              @pressEnter="handleSearch"
            />
          </span>
          <span class="search-item">
            <label>日期范围</label>
            <a-range-picker
              v-model:value="dateRange"
              format="YYYY-MM-DD"
              style="width: 260px"
            />
          </span>
          <a-button type="primary" :loading="loading" @click="handleSearch">
            <template #icon><SearchOutlined /></template>查询
          </a-button>
          <a-button @click="handleReset">
            <template #icon><ClearOutlined /></template>重置
          </a-button>
        </a-space>
      </div>

      <div class="table-area">
        <BillTableList
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="billPagination"
          row-key="id"
          @page-change="handlePageChange"
        >
          <template #amountCell="{ record }">
            <span class="amount-cell">{{ formatAmount(record.amount) }}</span>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text || record.status }}</a-tag>
          </template>
        </BillTableList>
        <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'FinanceExpenseReimbursePage' })

import { ref, reactive, computed, onMounted } from 'vue'
import type { Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import request from '@/utils/request'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')

const searchParams = reactive({
  reimburseNo: '',
  applicantName: '',
})

const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

const statusMap: Record<string, { text: string; color: string }> = {
  '待审批': { text: '待审批', color: 'orange' },
  '已审批': { text: '已审批', color: 'blue' },
  '已驳回': { text: '已驳回', color: 'red' },
  '已完成': { text: '已完成', color: 'green' },
}

const columns = [
  { title: '报销编号', dataIndex: 'reimburseNo', key: 'reimburseNo', width: 160 },
  { title: '报销人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '费用类型', dataIndex: 'expenseType', key: 'expenseType', width: 100 },
  { title: '报销金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' as const, slotName: 'amountCell' },
  { title: '审批状态', dataIndex: 'status', key: 'status', width: 100, slotName: 'statusCell' },
  { title: '报销日期', dataIndex: 'applyDate', key: 'applyDate', width: 120 },
]

function formatAmount(val: number | null | undefined): string {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchParams.reimburseNo = ''
  searchParams.applicantName = ''
  dateRange.value = null
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.reimburseNo) params.reimburseNo = searchParams.reimburseNo
    if (searchParams.applicantName) params.applicantName = searchParams.applicantName
    if (dateRange.value) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }

    const res = await request.get('/api/finance/expense-reimburse/page', params) as any
    tableData.value = res?.records || res?.data?.records || []
    pagination.total = res?.total || res?.data?.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    hasError.value = false
  } catch (error: any) {
    message.error('获取费用报销列表失败')
    console.warn('[费用报销] 加载失败:', error?.message)
    hasError.value = true
  } finally {
    loading.value = false
  }
}

function handleError(error: any) {
  console.warn('[费用报销] 页面异常', error)
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-header__left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.page-header__title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.search-area {
  padding: 12px 16px;
  background: #fff;
  border-radius: 6px;
  margin-bottom: 12px;
}
.search-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.search-item label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
.table-area {
  flex: 1;
  min-height: 0;
  position: relative;
}
.update-time {
  position: absolute;
  bottom: 8px;
  right: 16px;
  font-size: 12px;
  color: #999;
}
.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}
</style>
