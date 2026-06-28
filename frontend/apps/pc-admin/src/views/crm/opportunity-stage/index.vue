<template>
  <PageContainer title="商机阶段" full-height>
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
            v-model:value="searchParams.stageName"
            placeholder="阶段名称"
            allow-clear
            style="width: 160px"
            @pressEnter="handleSearch"
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
  stageName: ''
})
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

const columns = [
  { field: 'stageCode', title: '阶段代码', width: 100 },
  { field: 'stageName', title: '阶段名称', width: 140 },
  { field: 'winRate', title: '赢率', width: 100, align: 'right' as const },
  { field: 'sort', title: '排序', width: 80 },
  { field: 'description', title: '描述', width: 240 },
  { field: 'status', title: '状态', width: 100 }
]

async function fetchData() {
  loading.value = true
  hasError.value = false
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      stageName: searchParams.stageName || undefined
    }
    const res = await request.get('/api/crm/opportunity-stage/page', { params })
    const result = res as any
    const data = result.data ?? result
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    hasError.value = true
    console.warn('[商机阶段] 加载数据失败', err)
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
  searchParams.stageName = ''
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleExport() {
  console.log('导出商机阶段数据')
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
