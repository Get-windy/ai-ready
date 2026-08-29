<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <a-breadcrumb>
              <a-breadcrumb-item>
                <router-link to="/">
                  首页
                </router-link>
              </a-breadcrumb-item>
              <a-breadcrumb-item>财务管理</a-breadcrumb-item>
              <a-breadcrumb-item>付款单</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header__title">
              付款单
            </h2>
          </div>
          <div class="page-header__right">
            <a-space :size="12">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <a-button
                size="small"
                :loading="loading"
                @click="fetchData"
              >
                <template #icon>
                  <ReloadOutlined />
                </template>刷新
              </a-button>
            </a-space>
          </div>
        </div>
      </template>

      <SearchBar
        :fields="searchFields"
        :loading="loading"
        @search="handleSearch"
        @reset="handleReset"
      />

      <BillTableList
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @page-change="handlePageChange"
      >
        <template #statusCell="{ record }">
          <a-tag :color="statusMap[record.status]?.color">
            {{ statusMap[record.status]?.text || record.status }}
          </a-tag>
        </template>
        <template #action="{ record }">
          <a @click="handleView(record)">详情</a>
        </template>
      </BillTableList>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'FinancePaymentDocPage' })

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
  { name: 'keyword', label: '关键字', type: 'input' as const, placeholder: '付款单号/供应商' },
  { name: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 },
    { label: '待审批', value: 1 },
    { label: '已审批', value: 2 },
    { label: '已完成', value: 7 },
  ]},
]

// 后端 ReceiptStatus: 0-草稿 1-待审批 2-已审批 3-已拒绝 4-待核销 5-核销中 6-已核销 7-已完成 8-已取消
const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '已拒绝', color: 'red' },
  4: { text: '待核销', color: 'purple' },
  5: { text: '核销中', color: 'cyan' },
  6: { text: '已核销', color: 'geekblue' },
  7: { text: '已完成', color: 'green' },
  8: { text: '已取消', color: 'default' },
}

const columns = [
  { title: '单据编号', dataIndex: 'paymentNo', key: 'paymentNo', width: 160 },
  { title: '单据日期', dataIndex: 'paymentDate', key: 'paymentDate', width: 110 },
  { title: '单据类型', dataIndex: 'paymentType', key: 'paymentType', width: 90 },
  { title: '往来单位', dataIndex: 'supplierName', key: 'supplierName', width: 150 },
  { title: '结算单位', dataIndex: 'supplierName', key: 'settleUnit', width: 150 },
  { title: '结算方式', dataIndex: 'paymentMethod', key: 'paymentMethod', width: 100 },
  { title: '本单金额', dataIndex: 'paymentAmount', key: 'paymentAmount', width: 120, align: 'right' },
  { title: '已结算', dataIndex: 'verifiedAmount', key: 'verifiedAmount', width: 120, align: 'right' },
  { title: '待审金额', dataIndex: 'pendingAmount', key: 'pendingAmount', width: 120, align: 'right' },
  { title: '未结算', dataIndex: 'unsettledAmount', key: 'unsettledAmount', width: 120, align: 'right' },
  { title: '来源订单', dataIndex: 'orderNo', key: 'orderNo', width: 140 },
  { title: '经手人', dataIndex: 'purchaserName', key: 'purchaserName', width: 100 },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '单据备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
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
    const res = await request.get('/erp/payment/page', {
      ...queryParams.value,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }) as any
    const records = res?.records || res?.data?.records || []
    // 未结算 = 本单金额 - 已结算
    records.forEach((r: any) => {
      r.unsettledAmount = Number(r.paymentAmount || 0) - Number(r.verifiedAmount || 0)
    })
    dataSource.value = records
    pagination.total = res?.total || res?.data?.total || 0
  } catch (error: any) {
    message.error('获取付款单列表失败')
    console.warn('[付款单] 加载失败:', error?.message)
  } finally {
    loading.value = false
  }
}

function handleView(record: any) {
  router.push(`/finance/payment-doc/form?id=${record.id}`)
}

function handleError(error: any) {
  console.warn('[付款单] 页面异常', error)
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; }
.page-header__left { display: flex; flex-direction: column; gap: 4px; }
.page-header__title { margin: 0; font-size: 20px; font-weight: 600; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
</style>
