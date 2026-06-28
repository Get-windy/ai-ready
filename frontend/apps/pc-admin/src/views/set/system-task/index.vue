<template>
  <ErrorBoundary>
    <PageContainer title="系统任务">
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="任务名称">
            <a-input
              v-model:value="searchParams.taskName"
              placeholder="请输入任务名称"
              allow-clear
              style="width: 180px"
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="searchParams.status"
              placeholder="请选择状态"
              style="width: 180px"
              allow-clear
            >
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="待执行">待执行</a-select-option>
              <a-select-option value="执行中">执行中</a-select-option>
              <a-select-option value="已完成">已完成</a-select-option>
              <a-select-option value="已失败">已失败</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon><ClearOutlined /></template>
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>
      <div class="table-area">
        <BillTableList
          :columns="columns"
          :api-url="apiUrl"
          :params="searchParams"
          ref="tableRef"
        />
        <div v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</div>
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
import request from '@/utils/request'

const tableRef = ref()
const apiUrl = '/api/set/system-task/page'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')

const searchParams = reactive({
  taskName: '',
  status: '',
})

const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const columns = [
  { title: '任务编号', dataIndex: 'taskNo', width: 160 },
  { title: '任务名称', dataIndex: 'taskName', width: 200 },
  { title: '任务类型', dataIndex: 'taskType', width: 100 },
  { title: '执行频率', dataIndex: 'frequency', width: 100 },
  { title: '上次执行', dataIndex: 'lastExecTime', width: 170 },
  { title: '状态', dataIndex: 'status', width: 100 },
]

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    lastUpdateTime.value = new Date().toLocaleString()
  } catch (e) {
    hasError.value = true
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  tableRef.value?.reload()
  fetchData()
}

const handleReset = () => {
  searchParams.taskName = ''
  searchParams.status = ''
  dateRange.value = null
  pagination.current = 1
  tableRef.value?.reload()
  fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
}

const handleError = () => {
  hasError.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.update-time { font-size: 12px; color: #999; }
</style>
