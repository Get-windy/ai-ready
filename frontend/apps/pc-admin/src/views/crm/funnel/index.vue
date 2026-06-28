<template>
  <PageContainer title="销售漏斗" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : hasError ? 'error' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-button size="small" :loading="loading" @click="fetchData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <ErrorBoundary @reset="fetchData">
      <div class="search-area">
        <a-space wrap>
          <a-input
            v-model:value="searchParams.salesmanName"
            placeholder="销售人员"
            allow-clear
            style="width: 160px"
            @pressEnter="handleSearch"
          />
          <a-range-picker
            v-model:value="dateRange"
            placeholder="时间段"
            style="width: 240px"
          />
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
          <a-button @click="handleReset">
            <template #icon><ClearOutlined /></template>
            重置
          </a-button>
        </a-space>
      </div>

      <div class="table-area">
        <BillTableList
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="billPagination"
          :show-add="false"
          :show-search="false"
          :show-export="true"
          :selectable="false"
          :min-empty-rows="10"
          @refresh="fetchData"
          @page-change="handlePageChange"
          @export="handleExport"
        />
      </div>
    </ErrorBoundary>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import type { Dayjs } from 'dayjs'
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
  salesmanName: ''
})
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

const columns = [
  { field: 'stageName', title: '阶段名称', width: 140 },
  { field: 'opportunityCount', title: '商机数量', width: 100, align: 'right' as const },
  { field: 'expectedAmount', title: '预计金额', width: 140, align: 'right' as const },
  { field: 'weightedAmount', title: '加权金额', width: 140, align: 'right' as const },
  { field: 'conversionRate', title: '转化率', width: 100, align: 'right' as const },
  { field: 'avgDays', title: '平均停留天数', width: 100, align: 'right' as const }
]

async function fetchData() {
  loading.value = true
  hasError.value = false
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      salesmanName: searchParams.salesmanName || undefined
    }
    if (dateRange.value) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    const res = await request.get('/api/crm/funnel/page', { params })
    const result = res as any
    const data = result.data ?? result
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    hasError.value = true
    console.warn('[销售漏斗] 加载数据失败', err)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchParams.salesmanName = ''
  dateRange.value = null
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleExport() {
  console.log('导出销售漏斗数据')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.search-area {
  padding: 12px 16px;
  background: #fff;
  border-radius: 6px;
  margin-bottom: 12px;
}

.table-area {
  flex: 1;
  min-height: 0;
}

.update-time {
  color: #999;
  font-size: 12px;
}

.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}
</style>
