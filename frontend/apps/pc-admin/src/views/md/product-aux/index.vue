<template>
  <ErrorBoundary>
    <PageContainer title="商品辅助资料">
      <div class="search-area">
        <div class="search-row">
          <div class="search-item">
            <span class="search-label">资料类型</span>
            <a-select
              v-model:value="searchParams.auxType"
              placeholder="请选择资料类型"
              allow-clear
              style="width: 180px"
            >
              <a-select-option value="品牌">
                品牌
              </a-select-option>
              <a-select-option value="产地">
                产地
              </a-select-option>
              <a-select-option value="材质">
                材质
              </a-select-option>
              <a-select-option value="颜色">
                颜色
              </a-select-option>
            </a-select>
          </div>
          <div class="search-item">
            <span class="search-label">名称</span>
            <a-input
              v-model:value="searchParams.name"
              placeholder="请输入名称"
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
const apiUrl = '/md/product-aux/page'

const searchParams = reactive({
  auxType: undefined as string | undefined,
  name: '',
})

const columns = [
  { title: '资料类型', dataIndex: 'auxType', width: 100 },
  { title: '编码', dataIndex: 'code', width: 120 },
  { title: '名称', dataIndex: 'name', width: 200 },
  { title: '排序', dataIndex: 'sort', width: 80 },
  { title: '状态', dataIndex: 'status', width: 100 },
]

const handleSearch = () => {
  tableRef.value?.reload()
}

const handleReset = () => {
  searchParams.auxType = undefined
  searchParams.name = ''
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
