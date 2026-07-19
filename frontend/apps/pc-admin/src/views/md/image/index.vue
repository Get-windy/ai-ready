<template>
  <ErrorBoundary>
    <PageContainer title="图片管理">
      <div class="search-area">
        <div class="search-row">
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
            <span class="search-label">图片类型</span>
            <a-select
              v-model:value="searchParams.imageType"
              placeholder="全部"
              allow-clear
              style="width: 180px"
            >
              <a-select-option value="主图">
                主图
              </a-select-option>
              <a-select-option value="详情图">
                详情图
              </a-select-option>
              <a-select-option value="缩略图">
                缩略图
              </a-select-option>
            </a-select>
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
const apiUrl = '/md/image/page'

const searchParams = reactive({
  productName: '',
  imageType: undefined as string | undefined,
})

const columns = [
  { title: '产品名称', dataIndex: 'productName', width: 200 },
  { title: '图片类型', dataIndex: 'imageType', width: 100 },
  { title: '图片路径', dataIndex: 'imageUrl', width: 240 },
  { title: '上传时间', dataIndex: 'uploadTime', width: 170 },
]

const handleSearch = () => {
  tableRef.value?.reload()
}

const handleReset = () => {
  searchParams.productName = ''
  searchParams.imageType = undefined
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
