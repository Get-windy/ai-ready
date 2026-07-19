<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="库存预警补货"
      full-height
    >
      <template #headerExtra>
        <a-space :size="12">
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span
            v-if="lastUpdateTime"
            class="update-time"
          >最后更新: {{ lastUpdateTime }}</span>
          <a-button
            size="small"
            @click="fetchData"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
          </a-button>
        </a-space>
      </template>
      <div class="search-area">
        <a-form
          layout="inline"
          :model="searchParams"
        >
          <a-form-item label="产品编码">
            <a-input
              v-model:value="searchParams.productCode"
              placeholder="请输入"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item label="产品名称">
            <a-input
              v-model:value="searchParams.productName"
              placeholder="请输入"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item label="日期范围">
            <a-range-picker
              v-model:value="dateRange"
              style="width: 220px"
              @change="handleDateChange"
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
                </template>查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ClearOutlined />
                </template>重置
              </a-button>
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
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :show-export="false"
          :show-batch-delete="false"
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

const searchParams = reactive({ productCode: '', productName: '', startDate: '', endDate: '' })
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const columns = [
  { title: '产品编码', field: 'productCode', key: 'productCode', width: 120 },
  { title: '产品名称', field: 'productName', key: 'productName', width: 180 },
  { title: '当前库存', field: 'currentStock', key: 'currentStock', width: 100, align: 'right' },
  { title: '安全库存', field: 'safeStock', key: 'safeStock', width: 100, align: 'right' },
  { title: '建议补货量', field: 'suggestQty', key: 'suggestQty', width: 100, align: 'right' },
  { title: '最近日期', field: 'lastDate', key: 'lastDate', width: 120 },
]

const handleDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) { searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''; searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || '' }
  else { searchParams.startDate = ''; searchParams.endDate = '' }
}

const fetchData = async () => {
  loading.value = true; hasError.value = false
  try {
    const res: any = await request.get('/purchase/alert-replenish/page', {
      params: { page: pagination.current, size: pagination.pageSize, ...searchParams }
    })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e: any) { hasError.value = true; console.warn('[库存预警补货] 获取失败', e)
  } finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { searchParams.productCode = ''; searchParams.productName = ''; searchParams.startDate = ''; searchParams.endDate = ''; dateRange.value = null; pagination.current = 1; fetchData() }
const handlePageChange = (page: number, pageSize: number) => { pagination.current = page; pagination.pageSize = pageSize; fetchData() }
const handleError = (e: Error) => { hasError.value = true; console.error(e) }
onMounted(fetchData)
</script>
<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.update-time { font-size: 12px; color: #999; }
</style>
