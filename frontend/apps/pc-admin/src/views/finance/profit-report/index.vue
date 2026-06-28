<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <a-breadcrumb>
              <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
              <a-breadcrumb-item>财务管理</a-breadcrumb-item>
              <a-breadcrumb-item>利润表</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header__title">利润表</h2>
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
            <label>年度</label>
            <a-input-number
              v-model:value="searchParams.year"
              placeholder="请输入年度"
              :min="2000"
              :max="2099"
              style="width: 120px"
              @pressEnter="handleSearch"
            />
          </span>
          <span class="search-item">
            <label>期间</label>
            <a-input-number
              v-model:value="searchParams.period"
              placeholder="请输入期间"
              :min="1"
              :max="12"
              style="width: 120px"
              @pressEnter="handleSearch"
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
          <template #currentAmountCell="{ record }">
            <span class="amount-cell">{{ formatAmount(record.currentAmount) }}</span>
          </template>
          <template #yearAmountCell="{ record }">
            <span class="amount-cell">{{ formatAmount(record.yearAmount) }}</span>
          </template>
        </BillTableList>
        <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'FinanceProfitReportPage' })

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
  year: new Date().getFullYear(),
  period: undefined as number | undefined,
})

const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

const columns = [
  { title: '项目', dataIndex: 'itemName', key: 'itemName', width: 200 },
  { title: '行次', dataIndex: 'lineNo', key: 'lineNo', width: 60 },
  { title: '本期金额', dataIndex: 'currentAmount', key: 'currentAmount', width: 140, align: 'right' as const, slotName: 'currentAmountCell' },
  { title: '本年累计金额', dataIndex: 'yearAmount', key: 'yearAmount', width: 140, align: 'right' as const, slotName: 'yearAmountCell' },
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
  searchParams.year = new Date().getFullYear()
  searchParams.period = undefined
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
    if (searchParams.year) params.year = searchParams.year
    if (searchParams.period) params.period = searchParams.period

    const res = await request.get('/api/finance/profit-report/page', params) as any
    tableData.value = res?.records || res?.data?.records || []
    pagination.total = res?.total || res?.data?.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    hasError.value = false
  } catch (error: any) {
    message.error('获取利润表数据失败')
    console.warn('[利润表] 加载失败:', error?.message)
    hasError.value = true
  } finally {
    loading.value = false
  }
}

function handleError(error: any) {
  console.warn('[利润表] 页面异常', error)
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
