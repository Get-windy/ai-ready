<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <a-breadcrumb>
              <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
              <a-breadcrumb-item>销售管理</a-breadcrumb-item>
              <a-breadcrumb-item>销售出库</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header__title">销售出库</h2>
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

      <VxeTableList
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @page-change="handlePageChange"
      >
        <template #statusCell="{ record }">
          <a-tag :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text }}</a-tag>
        </template>
        <template #action="{ record }">
          <a-space>
            <a @click="handleView(record)">详情</a>
          </a-space>
        </template>
      </VxeTableList>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'SaleOutboundPage' })

import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { outboundApi } from '@/api/erp'
import type { SaleOutbound } from '@/api/erp'

const loading = ref(false)
const dataSource = ref<SaleOutbound[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const searchFields = [
  { name: 'keyword', label: '关键字', type: 'input' as const, placeholder: '出库单号/客户' },
  { name: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待审核', value: 0 }, { label: '已出库', value: 1 }, { label: '已驳回', value: 2 }
  ]}
]

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'orange' },
  1: { text: '已出库', color: 'green' },
  2: { text: '已驳回', color: 'red' }
}

const columns = [
  { title: '出库单号', dataIndex: 'outboundNo', key: 'outboundNo', width: 160 },
  { title: '源订单号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 150 },
  { title: '出库日期', dataIndex: 'outboundDate', key: 'outboundDate', width: 110 },
  { title: '金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', type: 'action', width: 80, fixed: 'right' }
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
    const res = await outboundApi.page({
      ...queryParams.value,
      page: pagination.current,
      pageSize: pagination.pageSize
    })
    const pageResult = res as unknown as { records: SaleOutbound[]; total: number }
    dataSource.value = pageResult.records || []
    pagination.total = pageResult.total || 0
  } catch (error: any) {
    message.error('获取销售出库列表失败')
    console.warn('[销售出库] 加载失败:', error?.message)
  } finally {
    loading.value = false
  }
}

function handleView(record: SaleOutbound) {
  message.info(`查看出库单: ${record.outboundNo}`)
}

function handleError(error: any) {
  console.warn('[销售出库] 页面异常', error)
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-header__left { display: flex; flex-direction: column; gap: 4px; }
.page-header__title { margin: 0; font-size: 20px; font-weight: 600; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
</style>
