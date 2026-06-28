<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="费用支付" full-height>
      <template #headerExtra>
        <a-space :size="12">
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</span>
          <a-button size="small" @click="fetchData"><template #icon><ReloadOutlined /></template></a-button>
        </a-space>
      </template>
      <div class="search-area">
        <a-form layout="inline" :model="searchParams">
          <a-form-item label="支付编号">
            <a-input v-model:value="searchParams.payNo" placeholder="请输入支付编号" allow-clear style="width: 160px" @pressEnter="handleSearch" />
          </a-form-item>
          <a-form-item label="收款人">
            <a-input v-model:value="searchParams.payeeName" placeholder="请输入收款人" allow-clear style="width: 140px" @pressEnter="handleSearch" />
          </a-form-item>
          <a-form-item label="日期范围">
            <a-range-picker v-model:value="dateRange" format="YYYY-MM-DD" style="width: 260px" />
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
          <template #amountCell="{ record }">
            <span class="amount-cell">{{ formatAmount(record.amount) }}</span>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text || record.status }}</a-tag>
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
  payNo: '',
  payeeName: '',
})

const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const statusMap: Record<string, { text: string; color: string }> = {
  '待支付': { text: '待支付', color: 'orange' },
  '已支付': { text: '已支付', color: 'green' },
  '支付失败': { text: '支付失败', color: 'red' },
  '已取消': { text: '已取消', color: 'default' },
}

const columns = [
  { title: '支付编号', field: 'payNo', key: 'payNo', width: 160 },
  { title: '收款人', field: 'payeeName', key: 'payeeName', width: 100 },
  { title: '支付方式', field: 'payMethod', key: 'payMethod', width: 100 },
  { title: '支付金额', field: 'amount', key: 'amount', width: 120, align: 'right', slotName: 'amountCell' },
  { title: '状态', field: 'status', key: 'status', width: 100, slotName: 'statusCell' },
  { title: '支付日期', field: 'payDate', key: 'payDate', width: 120 },
]

function formatAmount(val: number | null | undefined): string {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const fetchData = async () => {
  loading.value = true; hasError.value = false
  try {
    const params: Record<string, any> = {
      page: pagination.current,
      size: pagination.pageSize,
    }
    if (searchParams.payNo) params.payNo = searchParams.payNo
    if (searchParams.payeeName) params.payeeName = searchParams.payeeName
    if (dateRange.value) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    const res: any = await request.get('/api/finance/expense-pay/page', { params })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e: any) { hasError.value = true; console.warn('[费用支付] 获取失败', e)
  } finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { searchParams.payNo = ''; searchParams.payeeName = ''; dateRange.value = null; pagination.current = 1; fetchData() }
const handlePageChange = (page: number, pageSize: number) => { pagination.current = page; pagination.pageSize = pageSize; fetchData() }
const handleError = (e: Error) => { hasError.value = true; console.error(e) }
onMounted(fetchData)
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.update-time { font-size: 12px; color: #999; }
.amount-cell { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; font-weight: 500; }
</style>
