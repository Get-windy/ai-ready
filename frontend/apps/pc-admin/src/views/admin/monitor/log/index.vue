<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="日志监控" full-height>
      <template #headerExtra>
        <a-space :size="12">
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</span>
          <a-button size="small" @click="fetchData"><template #icon><ReloadOutlined /></template></a-button>
        </a-space>
      </template>
      <div class="search-area">
        <a-form layout="inline" :model="searchParams">
          <a-form-item label="操作人">
            <a-input v-model:value="searchParams.operator" placeholder="请输入" allow-clear style="width: 150px" />
          </a-form-item>
          <a-form-item label="操作类型">
            <a-select v-model:value="searchParams.action" placeholder="请选择" allow-clear style="width: 140px">
              <a-select-option value="CREATE">新增</a-select-option>
              <a-select-option value="UPDATE">修改</a-select-option>
              <a-select-option value="DELETE">删除</a-select-option>
              <a-select-option value="LOGIN">登录</a-select-option>
              <a-select-option value="EXPORT">导出</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="时间范围">
            <a-range-picker v-model:value="dateRange" @change="handleDateChange" style="width: 220px" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch"><template #icon><SearchOutlined /></template>查询</a-button>
              <a-button @click="handleReset"><template #icon><ClearOutlined /></template>重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>
      <div class="table-area">
        <BillTableList
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="billPagination"
          :show-toolbar="false" :show-search="false" :show-add="false" :show-export="false" :show-batch-delete="false"
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
import request from '@/utils/request'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')

const searchParams = reactive({ operator: '', action: '', startDate: '', endDate: '' })
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const columns = [
  { title: '操作人', field: 'operator', key: 'operator', width: 120 },
  { title: '操作类型', field: 'action', key: 'action', width: 100 },
  { title: '操作内容', field: 'content', key: 'content', width: 300, ellipsis: true },
  { title: 'IP地址', field: 'ip', key: 'ip', width: 140 },
  { title: '操作时间', field: 'createTime', key: 'createTime', width: 170 },
]

const handleDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) { searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''; searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || '' }
  else { searchParams.startDate = ''; searchParams.endDate = '' }
}

const fetchData = async () => {
  loading.value = true; hasError.value = false
  try {
    const params = { page: pagination.current, size: pagination.pageSize, ...searchParams }
    const res: any = await request.get('/api/audit-log/page', { params })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e: any) {
    hasError.value = true
    console.warn('[日志监控] 获取列表失败', e)
    tableData.value = []
  } finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { searchParams.operator = ''; searchParams.action = ''; searchParams.startDate = ''; searchParams.endDate = ''; dateRange.value = null; pagination.current = 1; fetchData() }
const handlePageChange = (page: number, pageSize: number) => { pagination.current = page; pagination.pageSize = pageSize; fetchData() }
const handleError = (e: Error) => { hasError.value = true; console.error(e) }

onMounted(fetchData)
</script>
<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.update-time { font-size: 12px; color: #999; }
</style>
