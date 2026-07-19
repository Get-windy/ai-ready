<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="销售价格跟踪"
      full-height
    >
      <template #headerExtra>
        <a-space :size="12">
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span
            v-if="lastUpdateTime"
            class="update-time"
          >最后更新: {{ lastUpdateTime }}</span>
          <a-button
            size="small"
            @click="fetchData"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
          </a-button>
        </a-space>
      </template>
      <div class="search-area">
        <div class="search-grid" ref="gridRef">
          <div class="search-field-item">
            <a-range-picker v-model:value="dateRange" size="small" style="width: 100%" @change="handleDateChange" />
          </div>
          <div class="search-field-item">
            <a-input v-model:value="searchParams.productCode" placeholder="产品编码" allow-clear size="small" />
          </div>
          <div class="search-field-item">
            <a-input v-model:value="searchParams.productName" placeholder="产品名称" allow-clear size="small" />
          </div>
          <div class="search-action-group" ref="actionRef" :style="{ gridColumn: 'span ' + actionSpan }">
          <div class="search-field-item search-action-item">
            <a-space :size="4">
              <a-button type="primary" size="small" @click="handleSearch">
                <SearchOutlined /> 查询
              </a-button>
              <a-button size="small" @click="handleReset">
                <ClearOutlined /> 重置
              </a-button>
            </a-space>
          </div>
          </div>
        </div>
      </div>
      <div class="table-area">
        <BillTableList
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="billPagination"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-export="false"
          :show-batch-delete="false"
          :selectable="false"
          row-key="id"
          @page-change="handlePageChange"
        />
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>
<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import type { Dayjs } from 'dayjs'
import { ReloadOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import request from '@/utils/request'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

const searchParams = reactive({ productCode: '', productName: '', startDate: '', endDate: '' })
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const columns = [
  { title: '产品编码', field: 'productCode', key: 'productCode', width: 120 },
  { title: '产品名称', field: 'productName', key: 'productName', width: 180 },
  { title: '客户名称', field: 'customerName', key: 'customerName', width: 160 },
  { title: '最近售价', field: 'lastPrice', key: 'lastPrice', width: 120, align: 'right' },
  { title: '最近日期', field: 'lastDate', key: 'lastDate', width: 120 },
  { title: '最低价格', field: 'minPrice', key: 'minPrice', width: 120, align: 'right' },
  { title: '最高价格', field: 'maxPrice', key: 'maxPrice', width: 120, align: 'right' },
]

const handleDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) { searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''; searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || '' }
  else { searchParams.startDate = ''; searchParams.endDate = '' }
}

const fetchData = async () => {
  loading.value = true; hasError.value = false
  try {
    const res: any = await request.get('/sales/price-track/page', {
      params: { current: pagination.current, size: pagination.pageSize, ...searchParams }
    })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e: any) { hasError.value = true; console.warn('[销售价格跟踪] 获取失败', e)
  } finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { searchParams.productCode = ''; searchParams.productName = ''; searchParams.startDate = ''; searchParams.endDate = ''; dateRange.value = null; pagination.current = 1; fetchData() }
const handlePageChange = (page: number, pageSize: number) => { pagination.current = page; pagination.pageSize = pageSize; fetchData() }
const handleError = (e: Error) => { hasError.value = true; console.error(e) }
onMounted(fetchData)
</script>
<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }
.search-action-item { flex-shrink: 0; }
.search-action-group {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
.search-action-group .search-field-item {
  flex: 0 0 auto;
  width: auto;
  margin-right: 4px;
}
.search-action-group .search-field-item:last-child {
  margin-right: 0;
}
.search-more-toggle {
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.search-more-toggle::before,
.search-more-toggle::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e8e8e8;
}
.search-more-toggle :deep(.ant-btn) {
  font-size: 13px;
}
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.update-time { font-size: 12px; color: #999; }
</style>
