<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :current-view="currentView"
    :selected-count="selectedRowKeys.length"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    @view-change="handleViewChange"
    @clear-selection="handleClearSelection"
    @page-change="handlePageChange"
    @page-size-change="handlePageSizeChange"
  >
    <template #actions>
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #list-view>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
    :error="error"
    :empty="!loading && !error && dataSource.length === 0"
        :row-selection="rowSelection"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getBatchStatusColor(record.status)">
              {{ getBatchStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'expiryDate'">
            <a-tag :color="getExpiryColor(record.expiryDate)">
              {{ record.expiryDate }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>

  <a-modal
    v-model:open="detailVisible"
    title="批次详情"
    width="700px"
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="批次号">{{ currentRecord.batchNo }}</a-descriptions-item>
      <a-descriptions-item label="产品编码">{{ currentRecord.productCode }}</a-descriptions-item>
      <a-descriptions-item label="产品名称">{{ currentRecord.productName }}</a-descriptions-item>
      <a-descriptions-item label="生产日期">{{ currentRecord.productionDate }}</a-descriptions-item>
      <a-descriptions-item label="有效期">
        <a-tag :color="getExpiryColor(currentRecord.expiryDate)">{{ currentRecord.expiryDate }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getBatchStatusColor(currentRecord.status)">{{ getBatchStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="库存数量">{{ currentRecord.quantity || '-' }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ currentRecord.warehouseName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ExportOutlined } from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'
import dayjs from 'dayjs'

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const currentView = ref('list')
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

const breadcrumbItems = computed(() => [
  { text: '库存管理', path: '/stock' },
  { text: '批次管理' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 180 },
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 150 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '生产日期', dataIndex: 'productionDate', key: 'productionDate', width: 120 },
  { title: '有效期', dataIndex: 'expiryDate', key: 'expiryDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', fixed: 'right', width: 80 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys
  }
}))

const getBatchStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'green',
    2: 'orange',
    3: 'red'
  }
  return colors[status] || 'default'
}

const getBatchStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '待检',
    1: '合格',
    2: '临期',
    3: '过期'
  }
  return texts[status] || '未知'
}

const getExpiryColor = (expiryDate: string) => {
  const today = new Date()
  const expiry = new Date(expiryDate)
  const days = Math.ceil((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))

  if (days < 0) return 'red'
  if (days < 30) return 'orange'
  return 'green'
}

const fetchData = async () => {
  loading.value = true
  try {
    dataSource.value = []
    pagination.total = 0
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleViewChange = (view: string) => {
  currentView.value = view
}

const handleClearSelection = () => {
  selectedRowKeys.value = []
}

const handlePageChange = (page: number) => {
  pagination.current = page
  fetchData()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.current = 1
  fetchData()
}

const handleView = (record: any) => {
  currentRecord.value = record
  detailVisible.value = true
}

// ── 导出 ──────────────────────────────────────────────
const handleExport = async () => {
  const hide = message.loading('正在导出批次数据...', 0)
  try {
    const blob = await fetch('/api/erp/batch-sn/batch/export', {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token') || ''}`
      }
    }).then(res => {
      if (!res.ok) throw new Error('导出请求失败')
      return res.blob()
    })

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `批次报表_${dayjs().format('YYYY-MM-DD_HHmmss')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    hide()
    message.success('导出成功，文件下载中')
  } catch (err: any) {
    hide()
    message.error(err?.message || '导出失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>
