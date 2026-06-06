<template>
  <div class="finance-payable-page">
    <a-tabs v-model:activeKey="activeTab">
      <!-- 列表标签 -->
      <a-tab-pane key="list" tab="应付列表">
        <TableList
          ref="tableRef"
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          :table-key="'finance-payable-list'"
          :filter-fields="filterFields"
          :show-search="false"
          :show-export="false"
          :show-add="false"
          :show-edit="false"
          :show-delete="false"
          :selectable="false"
          :scroll="{ x: 1200 }"
          @refresh="fetchData"
          @page-change="handlePageChange"
          @filter-change="handleFilterChange"
        >
          <template #toolbar-actions>
            <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
              更新 {{ dayjs(lastUpdated).format('HH:mm') }}
            </span>
          </template>

          <template #empty>
            <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配应付记录">
              <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
              <a-button @click="handleResetFilters">清除筛选</a-button>
            </a-empty>
            <a-empty v-else description="暂无应付账款">
              <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
            </a-empty>
          </template>

          <template #status="{ record }">
            <a-tag :color="statusColorMap[record.status] || 'default'">
              {{ statusLabelMap[record.status] || '未知' }}
            </a-tag>
          </template>
          <template #amount="{ record }">
            {{ formatAmount(record.amount) }}
          </template>
          <template #writtenOff="{ record }">
            {{ formatAmount(record.writtenOff) }}
          </template>
          <template #balance="{ record }">
            {{ formatAmount(record.balance) }}
          </template>
          <template #dueDate="{ record }">
            <span :class="{ 'text-danger': isOverdue(record.dueDate) && record.balance > 0 }">
              {{ record.dueDate }}
            </span>
          </template>
          <template #action="{ record }">
            <a-space :size="0" class="action-cell-inner">
              <a-tooltip :title="record.status === 2 ? '' : '核销'">
                <a-button type="link" size="small" :disabled="record.status === 2" @click="handleWriteOff(record)">
                  <template #icon><CheckCircleOutlined /></template>
                </a-button>
              </a-tooltip>
            </a-space>
          </template>
        </TableList>
      </a-tab-pane>

      <!-- 账龄分析标签 -->
      <a-tab-pane key="aging" tab="账龄分析">
        <div ref="agingChartRef" style="height: 400px"></div>
      </a-tab-pane>
    </a-tabs>

    <!-- 核销弹窗 -->
    <a-modal
      v-model:open="writeOffVisible"
      title="应付账款核销"
      :confirm-loading="writeOffLoading"
      @ok="handleWriteOffConfirm"
      @cancel="handleWriteOffCancel"
    >
      <a-descriptions v-if="writeOffTarget" :column="1" bordered size="small">
        <a-descriptions-item label="供应商">{{ writeOffTarget.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="来源单号">{{ writeOffTarget.sourceNo }}</a-descriptions-item>
        <a-descriptions-item label="应付总额">
          {{ formatAmount(writeOffTarget.amount) }}
        </a-descriptions-item>
        <a-descriptions-item label="已核销金额">
          {{ formatAmount(writeOffTarget.writtenOff) }}
        </a-descriptions-item>
        <a-descriptions-item label="剩余金额">
          {{ formatAmount(writeOffTarget.balance) }}
        </a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="核销金额" required>
          <a-input-number
            v-model:value="writeOffAmount"
            :min="0.01"
            :max="writeOffTarget?.balance || 0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入核销金额"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import { SearchOutlined, CheckCircleOutlined, InboxOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import TableList from '@/components/TableList/TableList.vue'
import { payableApi } from '@/api/finance'

const tableRef = ref()
const activeTab = ref('list')
const loading = ref(false)
const tableData = ref<any[]>([])

const searchForm = reactive({
  supplierName: '',
  status: undefined as number | undefined
})

const filterFields = [
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '供应商名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '未核销', value: 0 },
    { label: '部分核销', value: 1 },
    { label: '已核销', value: 2 }
  ]}
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchForm).some(v => v !== undefined && v !== null && v !== '')
})

const statusColorMap: Record<number, string> = {
  0: 'warning',
  1: 'processing',
  2: 'success'
}

const statusLabelMap: Record<number, string> = {
  0: '未核销',
  1: '部分核销',
  2: '已核销'
}

const columns: TableProps['columns'] = [
  { title: '来源单号', dataIndex: 'sourceNo', key: 'sourceNo', width: 150 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150 },
  { title: '总额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right', slotName: 'amount' },
  { title: '已核销', dataIndex: 'writtenOff', key: 'writtenOff', width: 120, align: 'right', slotName: 'writtenOff' },
  { title: '余额', dataIndex: 'balance', key: 'balance', width: 120, align: 'right', slotName: 'balance' },
  { title: '到期日', dataIndex: 'dueDate', key: 'dueDate', width: 110, slotName: 'dueDate' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100, slotName: 'status' },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const, slotName: 'action' }
]

// 核销弹窗
const writeOffVisible = ref(false)
const writeOffLoading = ref(false)
const writeOffTarget = ref<any>(null)
const writeOffAmount = ref(0)

// 账龄图表
const agingChartRef = ref<HTMLDivElement>()
let agingChart: echarts.ECharts | null = null

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.supplierName) params.supplierName = searchForm.supplierName
    if (searchForm.status !== undefined) params.status = searchForm.status

    const res = await payableApi.getPage(params)
    if (res.data) {
      tableData.value = res.data.records || res.data.list || []
      pagination.total = res.data.total || 0
      lastUpdated.value = new Date().toISOString()
    }
  } catch {
    message.error('获取应付账款数据失败')
  } finally {
    loading.value = false
  }
}

const fetchAgingData = async () => {
  try {
    const res = await payableApi.getAging()
    if (res.data) {
      initAgingChart(res.data)
    }
  } catch {
    // Silently handle
  }
}

const initAgingChart = (data: any) => {
  if (!agingChartRef.value) return
  agingChart?.dispose()
  agingChart = echarts.init(agingChartRef.value)

  const buckets = data.buckets || data || []
  const categories = buckets.map((b: any) => b.label || b.name || '')
  const values = buckets.map((b: any) => b.amount || b.value || 0)

  agingChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/>金额：¥${Number(p.value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: categories, axisLabel: { rotate: 15 } },
    yAxis: { type: 'value', name: '金额 (元)' },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#fa8c16' },
          { offset: 1, color: '#ffc069' }
        ])
      },
      barMaxWidth: 60
    }]
  })
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.supplierName = ''
  searchForm.status = undefined
  handleSearch()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  searchForm.supplierName = filters.supplierName || ''
  searchForm.status = filters.status !== undefined ? filters.status : undefined
  pagination.current = 1
  fetchData()
}

const handleWriteOff = (record: any) => {
  writeOffTarget.value = record
  writeOffAmount.value = record.balance
  writeOffVisible.value = true
}

const handleWriteOffConfirm = async () => {
  if (!writeOffAmount.value || writeOffAmount.value <= 0) {
    message.warning('请输入有效的核销金额')
    return
  }
  if (writeOffAmount.value > (writeOffTarget.value?.balance || 0)) {
    message.warning('核销金额不能大于剩余金额')
    return
  }
  writeOffLoading.value = true
  try {
    await payableApi.writeOff(writeOffTarget.value.id, writeOffAmount.value)
    message.success('核销成功')
    writeOffVisible.value = false
    fetchData()
  } catch {
    message.error('核销失败')
  } finally {
    writeOffLoading.value = false
  }
}

const handleWriteOffCancel = () => {
  writeOffVisible.value = false
}

const isOverdue = (dueDate: string) => {
  return dueDate && new Date(dueDate) < new Date()
}

function handleResetFilters() {
  Object.keys(searchForm).forEach(k => { (searchForm as any)[k] = undefined })
  pagination.current = 1; fetchData()
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return '¥' + Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// Watch for tab change to init chart
watch(activeTab, (val) => {
  if (val === 'aging') {
    nextTick(() => {
      fetchAgingData()
    })
  }
})

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.finance-payable-page {
  padding: 0;
}

.text-danger {
  color: #ff4d4f;
  font-weight: 600;
}
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
</style>
