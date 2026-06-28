<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="费用统计" full-height>
      <template #headerExtra>
        <a-space :size="12">
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</span>
          <a-button size="small" @click="fetchData"><template #icon><ReloadOutlined /></template></a-button>
        </a-space>
      </template>
      <div class="search-area">
        <a-form layout="inline" :model="searchParams">
          <a-form-item label="费用类型">
            <a-select v-model:value="searchParams.expenseType" placeholder="请选择费用类型" allow-clear style="width: 140px">
              <a-select-option value="">全部</a-select-option>
              <a-select-option value="差旅费">差旅费</a-select-option>
              <a-select-option value="办公费">办公费</a-select-option>
              <a-select-option value="招待费">招待费</a-select-option>
              <a-select-option value="交通费">交通费</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="年度">
            <a-input-number v-model:value="searchParams.year" placeholder="年度" :min="2020" :max="2099" style="width: 120px" />
          </a-form-item>
          <a-form-item label="部门">
            <a-input v-model:value="searchParams.deptName" placeholder="请输入部门" allow-clear style="width: 140px" @pressEnter="handleSearch" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="loading" @click="handleSearch"><template #icon><SearchOutlined /></template>查询</a-button>
              <a-button @click="handleReset"><template #icon><ClearOutlined /></template>重置</a-button>
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
          :show-toolbar="false" :show-search="false" :show-add="false" :show-export="false" :show-batch-delete="false"
          :selectable="false"
          row-key="id"
          @page-change="handlePageChange"
        >
          <template #totalAmountCell="{ record }">
            <span class="amount-cell">{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template #avgAmountCell="{ record }">
            <span class="amount-cell">{{ formatAmount(record.avgAmount) }}</span>
          </template>
          <template #yoyRateCell="{ record }">
            <span :class="record.yoyRate > 0 ? 'trend-up' : record.yoyRate < 0 ? 'trend-down' : ''">
              {{ formatPercent(record.yoyRate) }}
            </span>
          </template>
        </BillTableList>
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

const searchParams = reactive({
  expenseType: undefined as string | undefined,
  year: undefined as number | undefined,
  deptName: '',
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const columns = [
  { title: '费用类型', field: 'expenseType', key: 'expenseType', width: 120 },
  { title: '部门', field: 'deptName', key: 'deptName', width: 140 },
  { title: '笔数', field: 'count', key: 'count', width: 80, align: 'right' },
  { title: '总金额', field: 'totalAmount', key: 'totalAmount', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { title: '平均金额', field: 'avgAmount', key: 'avgAmount', width: 120, align: 'right', slotName: 'avgAmountCell' },
  { title: '同比', field: 'yoyRate', key: 'yoyRate', width: 100, align: 'right', slotName: 'yoyRateCell' },
]

function formatAmount(val: number | null | undefined): string {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatPercent(val: number | null | undefined): string {
  if (val === null || val === undefined) return '-'
  return (val * 100).toFixed(2) + '%'
}

const fetchData = async () => {
  loading.value = true; hasError.value = false
  try {
    const res: any = await request.get('/api/finance/expense-stats/page', {
      params: { page: pagination.current, size: pagination.pageSize, ...searchParams }
    })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e: any) { hasError.value = true; console.warn('[费用统计] 获取失败', e)
  } finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { searchParams.expenseType = undefined; searchParams.year = undefined; searchParams.deptName = ''; pagination.current = 1; fetchData() }
const handlePageChange = (page: number, pageSize: number) => { pagination.current = page; pagination.pageSize = pageSize; fetchData() }
const handleError = (e: Error) => { hasError.value = true; console.error(e) }
onMounted(fetchData)
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.update-time { font-size: 12px; color: #999; }
.amount-cell { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; font-weight: 500; }
.trend-up { color: #f5222d; }
.trend-down { color: #52c41a; }
</style>
