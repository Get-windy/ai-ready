<template>
  <ErrorBoundary>
    <PageContainer title="商品价格管理">
      <div class="search-area">
        <div class="search-row">
          <div class="search-item">
            <span class="search-label">产品编码</span>
            <a-input
              v-model:value="searchParams.productCode"
              placeholder="请输入产品编码"
              allow-clear
              style="width: 180px"
            />
          </div>
          <div class="search-item">
            <span class="search-label">产品名称</span>
            <a-input
              v-model:value="searchParams.productName"
              placeholder="请输入产品名称"
              allow-clear
              style="width: 180px"
            />
          </div>
          <div class="search-item">
            <a-space>
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
        </div>
      </div>
      <BillTableList
        ref="tableRef"
        :columns="columns"
        :api-url="apiUrl"
        :params="searchParams"
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
const apiUrl = '/md/product-price/page'

const searchParams = reactive({
  productCode: '',
  productName: '',
})

const columns = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 200 },
  { title: '零售价', dataIndex: 'retailPrice', width: 120, align: 'right' },
  { title: '批发价', dataIndex: 'wholesalePrice', width: 120, align: 'right' },
  { title: '成本价', dataIndex: 'costPrice', width: 120, align: 'right' },
  { title: '生效日期', dataIndex: 'effectiveDate', width: 120 },
]

const handleSearch = () => {
  tableRef.value?.reload()
}

const handleReset = () => {
  searchParams.productCode = ''
  searchParams.productName = ''
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
