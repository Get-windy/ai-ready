<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <a-breadcrumb>
              <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
              <a-breadcrumb-item>财务管理</a-breadcrumb-item>
              <a-breadcrumb-item>费用单</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header__title">费用单</h2>
          </div>
          <div class="page-header__right">
            <a-space :size="12">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <a-button size="small" :loading="loading" @click="fetchData">
                <template #icon><ReloadOutlined /></template>刷新
              </a-button>
            </a-space>
          </div>
        </div>
      </template>

      <SearchBar :fields="searchFields" :loading="loading" @search="handleSearch" @reset="handleReset" />

      <BillTableList
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @page-change="handlePageChange"
      >
        <template #statusCell="{ record }">
          <a-tag :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text || record.status }}</a-tag>
        </template>
        <template #action="{ record }">
          <a @click="handleView(record)">详情</a>
        </template>
      </BillTableList>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'FinanceExpenseDocPage' })

import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { useRouter } from 'vue-router'
import { request } from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const searchFields = [
  { name: 'keyword', label: '关键字', type: 'input' as const, placeholder: '费用单号/申请人' },
  { name: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待审批', value: '待审批' },
    { label: '已审批', value: '已审批' },
    { label: '已报销', value: '已报销' },
  ]},
]

const statusMap: Record<string, { text: string; color: string }> = {
  '待审批': { text: '待审批', color: 'orange' },
  '已审批': { text: '已审批', color: 'blue' },
  '已报销': { text: '已报销', color: 'green' },
}

const columns = [
  { title: '费用单号', dataIndex: 'expenseNo', key: 'expenseNo', width: 160 },
  { title: '部门', dataIndex: 'department', key: 'department', width: 100 },
  { title: '费用日期', dataIndex: 'expenseDate', key: 'expenseDate', width: 110 },
  { title: '费用金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '费用类型', dataIndex: 'expenseType', key: 'expenseType', width: 100 },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', type: 'action', width: 80, fixed: 'right' },
]

const queryParams = ref<Record<string, any>>({})

function handleSearch(values: Record<string, any>) {
  queryParams.value = values
  pagination.current = 1
  fetchData()
}

function handleReset() {
  queryParams.value = {}
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
    const res = await request.get('/finance/expense-doc/page', {
      ...queryParams.value,
      page: pagination.current,
      pageSize: pagination.pageSize,
    }) as any
    dataSource.value = res?.records || res?.data?.records || []
    pagination.total = res?.total || res?.data?.total || 0
  } catch (error: any) {
    message.error('获取费用单列表失败')
    console.warn('[费用单] 加载失败:', error?.message)
  } finally {
    loading.value = false
  }
}

function handleView(record: any) {
  router.push(`/finance/expense-doc/form?id=${record.id}`)
}

function handleError(error: any) {
  console.warn('[费用单] 页面异常', error)
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-header__left { display: flex; flex-direction: column; gap: 4px; }
.page-header__title { margin: 0; font-size: 20px; font-weight: 600; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
</style>
