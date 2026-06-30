<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <a-breadcrumb>
              <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
              <a-breadcrumb-item>采购管理</a-breadcrumb-item>
              <a-breadcrumb-item>采购退货</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header__title">采购退货</h2>
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
          <a-tag :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text }}</a-tag>
        </template>
        <template #action="{ record }">
          <a-space>
            <a @click="handleView(record)">详情</a>
          </a-space>
        </template>
      </BillTableList>

      <!-- 详情抽屉 -->
      <a-drawer
        v-model:open="detailVisible"
        title="采购退货单详情"
        :width="520"
        :loading="detailLoading"
      >
        <template v-if="detailData">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="退货单号" :span="2">{{ detailData.returnNo }}</a-descriptions-item>
            <a-descriptions-item label="源订单号" :span="2">{{ detailData.orderNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="供应商" :span="2">{{ detailData.supplierName }}</a-descriptions-item>
            <a-descriptions-item label="退货日期">{{ detailData.returnDate }}</a-descriptions-item>
            <a-descriptions-item label="金额">
              <span style="color: #ff4d4f; font-weight: 600;">
                {{ detailData.totalAmount != null ? `¥${detailData.totalAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '-' }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="statusMap[detailData.status]?.color">{{ statusMap[detailData.status]?.text }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="制单人">{{ detailData.creatorName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间" :span="2">{{ detailData.createTime }}</a-descriptions-item>
          </a-descriptions>
          <div style="margin-top: 16px;">
            <a-space>
              <a-button
                v-if="detailData.status === 0"
                type="primary"
                size="small"
                @click="handleApprove(detailData)"
              >审核通过</a-button>
              <a-button
                v-if="detailData.status === 0"
                danger
                size="small"
                @click="handleReject(detailData)"
              >驳回</a-button>
            </a-space>
          </div>
        </template>
      </a-drawer>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseReturnPage' })

import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { purchaseReturnApi } from '@/api/erp'
import type { PurchaseReturn } from '@/api/erp'

const loading = ref(false)
const dataSource = ref<PurchaseReturn[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const searchFields = [
  { name: 'keyword', label: '关键字', type: 'input' as const, placeholder: '退货单号/供应商' },
  { name: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待审核', value: 0 }, { label: '已通过', value: 1 }, { label: '已驳回', value: 2 }
  ]}
]

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'orange' },
  1: { text: '已通过', color: 'green' },
  2: { text: '已驳回', color: 'red' }
}

const columns = [
  { title: '退货单号', dataIndex: 'returnNo', key: 'returnNo', width: 160 },
  { title: '源订单号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150 },
  { title: '退货日期', dataIndex: 'returnDate', key: 'returnDate', width: 110 },
  { title: '金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '制单人', dataIndex: 'creatorName', key: 'creatorName', width: 100 },
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
    const res = await purchaseReturnApi.page({
      ...queryParams.value,
      page: pagination.current,
      pageSize: pagination.pageSize
    })
    const pageResult = res as unknown as { records: PurchaseReturn[]; total: number }
    dataSource.value = pageResult.records || []
    pagination.total = pageResult.total || 0
  } catch (error: any) {
    message.error('获取采购退货列表失败')
    console.warn('[采购退货] 加载失败:', error?.message)
  } finally {
    loading.value = false
  }
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<PurchaseReturn | null>(null)

async function handleView(record: PurchaseReturn) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await purchaseReturnApi.getById(record.id)
    detailData.value = (res as any)?.data || (res as any) || record
  } catch (e) {
    detailData.value = record
  } finally {
    detailLoading.value = false
  }
}

async function handleApprove(record: PurchaseReturn) {
  try {
    await purchaseReturnApi.approve(record.id)
    message.success('审核通过')
    detailVisible.value = false
    fetchData()
  } catch (e) {
    message.error('审核失败')
  }
}

function handleReject(record: PurchaseReturn) {
  Modal.confirm({
    title: '确认驳回',
    content: `确定要驳回退货单 ${record.returnNo} 吗？`,
    onOk: async () => {
      try {
        await purchaseReturnApi.reject(record.id)
        message.success('已驳回')
        detailVisible.value = false
        fetchData()
      } catch (e) {
        message.error('驳回失败')
      }
    }
  })
}

function handleError(error: any) {
  console.warn('[采购退货] 页面异常', error)
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
