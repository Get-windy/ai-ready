<template>
  <ErrorBoundary>
    <PageContainer title="库存期初">
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="产品编码">
            <a-input
              v-model:value="searchParams.productCode"
              placeholder="请输入产品编码"
              allow-clear
              style="width: 180px"
            />
          </a-form-item>
          <a-form-item label="产品名称">
            <a-input
              v-model:value="searchParams.productName"
              placeholder="请输入产品名称"
              allow-clear
              style="width: 180px"
            />
          </a-form-item>
          <a-form-item>
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
          </a-form-item>
        </a-form>
      </div>
      <div class="table-area">
        <BillTableList
          ref="tableRef"
          :columns="columns"
          :api-url="apiUrl"
          :params="searchParams"
        />
        <div
          v-if="lastUpdateTime"
          class="update-time"
        >
          最后更新: {{ lastUpdateTime }}
        </div>
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
const apiUrl = '/set/initial-stock/page'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')

const searchParams = reactive({
  productCode: '',
  productName: '',
})

const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const columns = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 200 },
  { title: '仓库名称', dataIndex: 'warehouseName', width: 140 },
  { title: '期初数量', dataIndex: 'quantity', width: 100, align: 'right' as const },
  { title: '期初单价', dataIndex: 'unitPrice', width: 100, align: 'right' as const },
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
  searchParams.productCode = ''
  searchParams.productName = ''
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
