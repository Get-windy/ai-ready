<template>
  <ErrorBoundary>
    <PageContainer title="财务期初">
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="科目代码">
            <a-input
              v-model:value="searchParams.subjectCode"
              placeholder="请输入科目代码"
              allow-clear
              style="width: 180px"
            />
          </a-form-item>
          <a-form-item label="年度">
            <a-input-number
              v-model:value="searchParams.year"
              placeholder="请输入年度"
              style="width: 180px"
              :min="2000"
              :max="2099"
            />
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
const apiUrl = '/api/set/initial-finance/page'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')

const searchParams = reactive({
  subjectCode: '',
  year: undefined as number | undefined,
})

const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const columns = [
  { title: '科目代码', dataIndex: 'subjectCode', width: 100 },
  { title: '科目名称', dataIndex: 'subjectName', width: 200 },
  { title: '期初余额', dataIndex: 'openBalance', width: 140, align: 'right' as const },
  { title: '借贷方向', dataIndex: 'direction', width: 80 },
  { title: '录入年度', dataIndex: 'year', width: 80 },
  { title: '录入时间', dataIndex: 'createTime', width: 170 },
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
  searchParams.subjectCode = ''
  searchParams.year = undefined
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
