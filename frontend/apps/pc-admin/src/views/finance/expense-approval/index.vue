<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="费用审批" full-height>
      <template #headerExtra>
        <a-space :size="12">
          <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
          <span v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</span>
          <a-button size="small" @click="fetchData"><template #icon><ReloadOutlined /></template></a-button>
        </a-space>
      </template>
      <div class="search-area">
        <a-form layout="inline" :model="searchParams">
          <a-form-item label="单号">
            <a-input v-model:value="searchParams.docNo" placeholder="请输入单号" allow-clear style="width: 160px" @pressEnter="handleSearch" />
          </a-form-item>
          <a-form-item label="申请人">
            <a-input v-model:value="searchParams.applicantName" placeholder="请输入申请人" allow-clear style="width: 140px" @pressEnter="handleSearch" />
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
  docNo: '',
  applicantName: '',
})

const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

const statusMap: Record<string, { text: string; color: string }> = {
  '待审批': { text: '待审批', color: 'orange' },
  '已审批': { text: '已审批', color: 'blue' },
  '已驳回': { text: '已驳回', color: 'red' },
  '已完成': { text: '已完成', color: 'green' },
}

const columns = [
  { title: '单号', field: 'docNo', key: 'docNo', width: 160 },
  { title: '申请人', field: 'applicantName', key: 'applicantName', width: 100 },
  { title: '费用类型', field: 'expenseType', key: 'expenseType', width: 100 },
  { title: '金额', field: 'amount', key: 'amount', width: 120, align: 'right', slotName: 'amountCell' },
  { title: '审批状态', field: 'status', key: 'status', width: 100, slotName: 'statusCell' },
  { title: '提交时间', field: 'createTime', key: 'createTime', width: 170 },
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
    if (searchParams.docNo) params.docNo = searchParams.docNo
    if (searchParams.applicantName) params.applicantName = searchParams.applicantName
    if (dateRange.value) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    const res: any = await request.get('/api/finance/expense-approval/page', { params })
    if (res) {
      const data = res.data || res
      tableData.value = data.records || data.content || data.list || []
      pagination.total = data.total || 0
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e: any) { hasError.value = true; console.warn('[费用审批] 获取失败', e)
  } finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { searchParams.docNo = ''; searchParams.applicantName = ''; dateRange.value = null; pagination.current = 1; fetchData() }
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
