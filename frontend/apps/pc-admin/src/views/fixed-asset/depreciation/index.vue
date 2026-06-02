<template>
  <div class="depreciation-list">
    <a-card style="margin-bottom: 16px">
      <a-form layout="inline" :model="searchForm">
        <a-form-item label="资产ID">
          <a-input v-model:value="searchForm.assetId" placeholder="资产ID" allow-clear />
        </a-form-item>
        <a-form-item label="期间">
          <a-input v-model:value="searchForm.period" placeholder="yyyy-MM" allow-clear />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
            <a-button type="primary" ghost @click="handleBatchCalculate">批量计提折旧</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card>
      <a-table
        :dataSource="tableData"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="onTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'completed' ? 'green' : 'orange'">
              {{ record.status === 'completed' ? '已完成' : '失败' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { depreciationApi } from '@/api/fixed-asset'
import { message } from 'ant-design-vue'

const loading = ref(false)
const tableData = ref([])

const searchForm = reactive({
  assetId: undefined as string | undefined,
  period: undefined as string | undefined,
})

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

onMounted(() => {
  fetchData()
})

function fetchData() {
  loading.value = true
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchForm.assetId) params.assetId = Number(searchForm.assetId)
  if (searchForm.period) params.period = searchForm.period

  depreciationApi.getPage(params).then((res: any) => {
    tableData.value = res.data?.content || res.data?.records || []
    pagination.total = res.data?.totalElements || res.data?.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.assetId = undefined
  searchForm.period = undefined
  handleSearch()
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
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
</script>
