<template>
  <ErrorBoundary>
    <PageContainer title="客户">
      <div class="search-area">
        <div class="search-row">
          <div class="search-item">
            <span class="search-label">客户名称</span>
            <a-input
              v-model:value="searchParams.customerName"
              placeholder="请输入客户名称"
              allow-clear
              style="width: 180px"
            />
          </div>
          <div class="search-item">
            <span class="search-label">联系电话</span>
            <a-input
              v-model:value="searchParams.phone"
              placeholder="请输入联系电话"
              allow-clear
              style="width: 180px"
            />
          </div>
          <div class="search-item">
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
          </div>
        </div>
      </div>
      <BillTableList
        :columns="columns"
        :api-url="apiUrl"
        :params="searchParams"
        ref="tableRef"
      />
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
const apiUrl = '/api/md/customer/page'

const searchParams = reactive({
  customerName: '',
  phone: '',
})

const columns = [
  { title: '客户编码', dataIndex: 'customerCode', width: 120 },
  { title: '客户名称', dataIndex: 'customerName', width: 200 },
  { title: '联系人', dataIndex: 'contactName', width: 100 },
  { title: '联系电话', dataIndex: 'phone', width: 130 },
  { title: '地址', dataIndex: 'address', width: 240 },
  { title: '状态', dataIndex: 'status', width: 100 },
]

const handleSearch = () => {
  tableRef.value?.reload()
}

const handleReset = () => {
  searchParams.customerName = ''
  searchParams.phone = ''
  tableRef.value?.reload()
}
</script>

<style scoped>
.search-area {
  padding: 16px 16px 0;
  background: #fff;
  border-radius: 4px;
  margin-bottom: 16px;
}
.search-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-label {
  white-space: nowrap;
  font-size: 14px;
}
</style>
