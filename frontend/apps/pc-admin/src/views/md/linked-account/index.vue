<template>
  <ErrorBoundary>
    <PageContainer title="互联账号">
      <div class="search-area">
        <div class="search-row">
          <div class="search-item">
            <span class="search-label">账号名称</span>
            <a-input
              v-model:value="searchParams.accountName"
              placeholder="请输入账号名称"
              allow-clear
              style="width: 180px"
            />
          </div>
          <div class="search-item">
            <span class="search-label">平台</span>
            <a-select
              v-model:value="searchParams.platform"
              placeholder="全部"
              allow-clear
              style="width: 180px"
            >
              <a-select-option value="微信">
                微信
              </a-select-option>
              <a-select-option value="支付宝">
                支付宝
              </a-select-option>
              <a-select-option value="抖音">
                抖音
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
const apiUrl = '/md/linked-account/page'

const searchParams = reactive({
  accountName: '',
  platform: undefined as string | undefined,
})

const columns = [
  { title: '账号名称', dataIndex: 'accountName', width: 160 },
  { title: '平台', dataIndex: 'platform', width: 100 },
  { title: '账号', dataIndex: 'accountNo', width: 160 },
  { title: '关联类型', dataIndex: 'linkType', width: 100 },
  { title: '状态', dataIndex: 'status', width: 100 },
]

const handleSearch = () => {
  tableRef.value?.reload()
}

const handleReset = () => {
  searchParams.accountName = ''
  searchParams.platform = undefined
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
