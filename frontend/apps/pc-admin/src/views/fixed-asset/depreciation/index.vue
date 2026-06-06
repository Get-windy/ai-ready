<template>
  <div class="depreciation-list">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :table-key="'fixed-asset-depreciation-list'"
      :filter-fields="filterFields"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
        <a-button type="primary" ghost @click="handleBatchCalculate">批量计提折旧</a-button>
      </template>
      <template #empty>
        <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配折旧记录">
          <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
          <a-button @click="handleResetFilters">清除筛选</a-button>
        </a-empty>
        <a-empty v-else description="暂无折旧记录">
          <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
        </a-empty>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'completed' ? 'green' : 'orange'">
            {{ record.status === 'completed' ? '已完成' : '失败' }}
          </a-tag>
        </template>
      </template>
    </TableList>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, InboxOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import TableList from '@/components/TableList/TableList.vue'
import { depreciationApi } from '@/api/fixed-asset'

const loading = ref(false)
const tableData = ref([])
const tableRef = ref()
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

const columns = [
  { title: '资产编码', dataIndex: 'assetCode', width: 140 },
  { title: '资产名称', dataIndex: 'assetName', width: 180 },
  { title: '期间', dataIndex: 'period', width: 100 },
  { title: '折旧日期', dataIndex: 'depreciationDate', width: 120 },
  { title: '本期折旧', dataIndex: 'periodAmount', width: 120 },
  { title: '累计折旧', dataIndex: 'accumulatedDepreciation', width: 120 },
  { title: '净值', dataIndex: 'netValue', width: 120 },
  { title: '资产原值', dataIndex: 'assetOriginalValue', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
]

const filterFields = [
  { key: 'assetId', label: '资产ID', type: 'input' as const, placeholder: '资产ID' },
  { key: 'period', label: '期间', type: 'input' as const, placeholder: 'yyyy-MM' },
]

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

function fetchData() {
  loading.value = true
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchFilters.assetId) params.assetId = Number(searchFilters.assetId)
  if (searchFilters.period) params.period = searchFilters.period

  depreciationApi.getPage(params).then((res: any) => {
    tableData.value = res.data?.content || res.data?.records || []
    pagination.total = res.data?.totalElements || res.data?.total || 0
    lastUpdated.value = new Date().toISOString()
  }).finally(() => {
    loading.value = false
  })
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function handleBatchCalculate() {
  depreciationApi.batchCalculate().then(() => {
    message.success('批量折旧计提完成')
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '批量折旧失败')
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault() }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
.action-more-btn {
  border: none; box-shadow: none; padding: 4px 8px;
}
.action-cell-inner {
  display: inline-flex; align-items: center;
}
</style>
