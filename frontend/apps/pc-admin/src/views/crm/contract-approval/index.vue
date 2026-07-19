<template>
  <PageContainer
    title="合同审批"
    full-height
  >
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : hasError ? 'error' : 'success'" />
          <span
            v-if="lastUpdateTime"
            class="update-time"
          >
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-button
          size="small"
          :loading="loading"
          @click="fetchData"
        >
          <template #icon>
            <ReloadOutlined />
          </template>
          刷新
        </a-button>
      </a-space>
    </template>

    <ErrorBoundary @reset="fetchData">
      <div class="search-area">
        <a-space wrap>
          <a-input
            v-model:value="searchParams.contractNo"
            placeholder="合同编号"
            allow-clear
            style="width: 160px"
            @press-enter="handleSearch"
          />
          <a-input
            v-model:value="searchParams.customerName"
            placeholder="客户名称"
            allow-clear
            style="width: 160px"
            @press-enter="handleSearch"
          />
          <a-range-picker
            v-model:value="dateRange"
            style="width: 240px"
          />
          <a-button
            type="primary"
            @click="handleSearch"
          >
            <template #icon>
              <SearchOutlined />
            </template>
            查询
          </a-button>
          <a-button @click="handleReset">
            <template #icon>
              <ClearOutlined />
            </template>
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
  contractNo: '',
  customerName: ''
})
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total
}))

const columns = [
  { field: 'contractNo', title: '合同编号', width: 160 },
  { field: 'customerName', title: '客户名称', width: 160 },
  { field: 'amount', title: '合同金额', width: 120, align: 'right' as const },
  { field: 'approverName', title: '审批人', width: 100 },
  { field: 'status', title: '审批状态', width: 100 },
  { field: 'createTime', title: '创建时间', width: 170 }
]

async function fetchData() {
  loading.value = true
  hasError.value = false
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      contractNo: searchParams.contractNo || undefined,
      customerName: searchParams.customerName || undefined
    }
    if (dateRange.value) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    const res = await request.get('/crm/contract-approval/page', { params })
    const result = res as any
    const data = result.data ?? result
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    hasError.value = true
    console.warn('[合同审批] 加载数据失败', err)
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
  searchParams.contractNo = ''
  searchParams.customerName = ''
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
  console.log('导出合同审批数据')
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
