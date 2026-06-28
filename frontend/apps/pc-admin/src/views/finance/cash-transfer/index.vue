<template>
  <ErrorBoundary>
    <PageContainer title="现金转账">
      <div class="search-area">
        <div class="search-row">
          <div class="search-item">
            <span class="search-label">单据编号</span>
            <a-input v-model:value="searchParams.docNo" placeholder="请输入单据编号" allow-clear style="width: 180px" />
          </div>
          <div class="search-item">
            <span class="search-label">日期范围</span>
            <a-range-picker v-model:value="dateRange" style="width: 240px" />
          </div>
          <div class="search-item">
            <a-space>
              <a-button type="primary" @click="handleSearch"><template #icon><SearchOutlined /></template>查询</a-button>
              <a-button @click="handleReset"><template #icon><ClearOutlined /></template>重置</a-button>
            </a-space>
          </div>
        </div>
      </div>
      <BillTableList :columns="columns" :api-url="apiUrl" :params="searchParams" ref="tableRef" />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { Dayjs } from 'dayjs'
import { SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'

const tableRef = ref()
const apiUrl = '/api/finance/cash-transfer/page'
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const searchParams = reactive({ docNo: '', startDate: '', endDate: '' })

const columns = [
  { title: '单据编号', dataIndex: 'docNo', width: 160 },
  { title: '转出账户', dataIndex: 'fromAccount', width: 140 },
  { title: '转入账户', dataIndex: 'toAccount', width: 140 },
  { title: '转账金额', dataIndex: 'amount', width: 120, align: 'right' },
  { title: '制单人', dataIndex: 'creatorName', width: 100 },
  { title: '单据状态', dataIndex: 'status', width: 100 },
  { title: '制单日期', dataIndex: 'createTime', width: 170 },
]

const handleSearch = () => {
  if (dateRange.value) {
    searchParams.startDate = dateRange.value[0].format('YYYY-MM-DD')
    searchParams.endDate = dateRange.value[1].format('YYYY-MM-DD')
  } else { searchParams.startDate = ''; searchParams.endDate = '' }
  tableRef.value?.reload()
}
const handleReset = () => {
  searchParams.docNo = ''; searchParams.startDate = ''; searchParams.endDate = ''
  dateRange.value = null; tableRef.value?.reload()
}
</script>

<style scoped>
.search-area { padding: 16px 16px 0; background: #fff; border-radius: 4px; margin-bottom: 16px; }
.search-row { display: flex; flex-wrap: wrap; align-items: center; gap: 16px; margin-bottom: 16px; }
.search-item { display: flex; align-items: center; gap: 8px; }
.search-label { white-space: nowrap; font-size: 14px; }
</style>
