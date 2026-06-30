<template>
  <ErrorBoundary>
    <PageContainer title="实时跟踪">
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="日期范围">
            <a-range-picker v-model:value="dateRange" style="width: 220px" />
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
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange" size="small" />
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref<any[]>([])
const dateRange = ref(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` })

const columns: any[] = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 150 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 }
]

async function loadData() {
  loading.value = true
  try {
    const result = await request.get('/dms/realtime-tracking/page', { pageNum: pagination.current, pageSize: pagination.pageSize })
    if (result?.records) { tableData.value = result.records; pagination.total = result.total }
    else if (Array.isArray(result)) { tableData.value = result; pagination.total = result.length }
  } catch (e) { tableData.value = [] }
  finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { dateRange.value = null; pagination.current = 1; loadData() }
function handleTableChange(p) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

onMounted(loadData)
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
</style>