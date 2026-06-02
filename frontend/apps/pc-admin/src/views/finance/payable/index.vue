<template>
  <div class="finance-payable-page">
    <a-card :bordered="false">
      <a-tabs v-model:activeKey="activeTab">
        <!-- 列表标签 -->
        <a-tab-pane key="list" tab="应付列表">
          <!-- 搜索 -->
          <div class="search-area">
            <a-form layout="inline" :model="searchForm">
              <a-form-item label="供应商">
                <a-input
                  v-model:value="searchForm.supplierName"
                  placeholder="供应商名称"
                  allow-clear
                  style="width: 180px"
                />
              </a-form-item>
              <a-form-item label="状态">
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部"
                  allow-clear
                  style="width: 120px"
                >
                  <a-select-option :value="0">未核销</a-select-option>
                  <a-select-option :value="1">部分核销</a-select-option>
                  <a-select-option :value="2">已核销</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item>
                <a-space>
                  <a-button type="primary" @click="handleSearch">
                    <template #icon><SearchOutlined /></template>
                    搜索
                  </a-button>
                  <a-button @click="handleReset">
                    <template #icon><ReloadOutlined /></template>
                    重置
                  </a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </div>

          <a-table
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :pagination="pagination"
            row-key="id"
            :scroll="{ x: 1200 }"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="statusColorMap[record.status] || 'default'">
                  {{ statusLabelMap[record.status] || '未知' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'amount' || column.key === 'writtenOff' || column.key === 'balance'">
                {{ formatAmount(record[column.dataIndex]) }}
              </template>
              <template v-else-if="column.key === 'dueDate'">
                <span :class="{ 'text-danger': isOverdue(record.dueDate) && record.balance > 0 }">
                  {{ record.dueDate }}
                </span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button
                  type="link"
                  size="small"
                  :disabled="record.status === 2"
                  @click="handleWriteOff(record)"
                >
                  核销
                </a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 账龄分析标签 -->
        <a-tab-pane key="aging" tab="账龄分析">
          <div ref="agingChartRef" style="height: 400px"></div>
        </a-tab-pane>
      </a-tabs>
    </a-card>

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
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { payableApi } from '@/api/finance'

const activeTab = ref('list')
const loading = ref(false)
const tableData = ref<any[]>([])

const searchForm = reactive({
  supplierName: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
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
  { title: '总额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' },
  { title: '已核销', dataIndex: 'writtenOff', key: 'writtenOff', width: 120, align: 'right' },
  { title: '余额', dataIndex: 'balance', key: 'balance', width: 120, align: 'right' },
  { title: '到期日', dataIndex: 'dueDate', key: 'dueDate', width: 110 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
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

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
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
})
</script>

<style scoped>
.finance-payable-page {
  padding: 0;
}

.search-area {
  margin-bottom: 16px;
}

.text-danger {
  color: #ff4d4f;
  font-weight: 600;
}
</style>
