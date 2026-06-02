<template>
  <div class="replenishment-page">
    <a-card :bordered="false">
      <div class="page-header">
        <h2>智能补货建议</h2>
        <a-space>
          <a-button type="primary" @click="generateSuggestions">
            <template #icon><ReloadOutlined /></template>
            生成建议
          </a-button>
          <a-button @click="showReport">查看报告</a-button>
        </a-space>
      </div>

      <a-alert type="info" show-icon style="margin-bottom: 16px">
        <template #message>
          系统根据库存水平、销售趋势、安全库存和采购周期自动计算补货建议。建议优先级越高表示补货紧迫程度越高。
        </template>
      </a-alert>

      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="pending" tab="待处理">
          <a-table
            :columns="columns"
            :data-source="pendingSuggestions"
            :loading="loading"
            :pagination="pagination"
            row-key="id"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'product'">
                <div class="product-info">
                  <span class="product-name">{{ record.productName }}</span>
                  <span class="product-code">{{ record.productCode }}</span>
                </div>
              </template>
              <template v-if="column.key === 'stock'">
                <div class="stock-info">
                  <div class="stock-row">
                    <span class="stock-label">当前库存:</span>
                    <span class="stock-value">{{ record.currentQty }}</span>
                  </div>
                  <div class="stock-row">
                    <span class="stock-label">安全库存:</span>
                    <span class="stock-value">{{ record.safetyStock }}</span>
                  </div>
                  <div class="stock-row danger">
                    <span class="stock-label">缺口:</span>
                    <span class="stock-value shortage">{{ record.shortageQty }}</span>
                  </div>
                </div>
              </template>
              <template v-if="column.key === 'analysis'">
                <div class="analysis-info">
                  <div class="analysis-row">
                    <span class="analysis-label">日均销量:</span>
                    <span class="analysis-value">{{ record.avgDailySales }}件</span>
                  </div>
                  <div class="analysis-row">
                    <span class="analysis-label">库存天数:</span>
                    <span class="analysis-value" :class="{ danger: record.daysOfStock <= 3 }">{{ record.daysOfStock }}天</span>
                  </div>
                  <div class="analysis-row">
                    <span class="analysis-label">采购周期:</span>
                    <span class="analysis-value">{{ record.leadTime }}天</span>
                  </div>
                </div>
              </template>
              <template v-if="column.key === 'suggestedQty'">
                <span class="suggested-qty">建议采购 {{ record.suggestedQty }} 件</span>
              </template>
              <template v-if="column.key === 'priority'">
                <a-progress 
                  :percent="record.priority" 
                  :stroke-color="getPriorityColor(record.priority)"
                  :show-info="true"
                  size="small"
                />
              </template>
              <template v-if="column.key === 'estimatedArrival'">
                <span class="arrival-date">{{ formatDate(record.estimatedArrival) }}</span>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button size="small" type="primary" @click="handleCreateOrder(record)">
                    创建采购单
                  </a-button>
                  <a-button size="small" @click="handleIgnore(record)">
                    忽略
                  </a-button>
                  <a @click="handleViewDetail(record)">详情</a>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="processed" tab="已处理">
          <a-table
            :columns="processedColumns"
            :data-source="processedSuggestions"
            :loading="loading"
            :pagination="false"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag color="green">已生成采购单</a-tag>
              </template>
              <template v-if="column.key === 'purchaseOrder'">
                <a @click="goPurchaseOrder(record.purchaseOrderId)">{{ record.purchaseOrderNo }}</a>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="ignored" tab="已忽略">
          <a-table
            :columns="ignoredColumns"
            :data-source="ignoredSuggestions"
            :loading="loading"
            :pagination="false"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag color="default">已忽略</a-tag>
              </template>
              <template v-if="column.key === 'ignoreReason'">
                <span class="ignore-reason">{{ record.ignoreReason }}</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="detailVisible"
      title="补货建议详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="产品名称">{{ suggestionDetail.productName }}</a-descriptions-item>
        <a-descriptions-item label="产品编码">{{ suggestionDetail.productCode }}</a-descriptions-item>
        <a-descriptions-item label="当前库存">{{ suggestionDetail.currentQty }}</a-descriptions-item>
        <a-descriptions-item label="安全库存">{{ suggestionDetail.safetyStock }}</a-descriptions-item>
        <a-descriptions-item label="缺口数量">
          <span class="shortage">{{ suggestionDetail.shortageQty }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="建议采购量">
          <span class="suggested">{{ suggestionDetail.suggestedQty }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="日均销量">{{ suggestionDetail.avgDailySales }}件</a-descriptions-item>
        <a-descriptions-item label="库存天数">
          <span :class="{ danger: suggestionDetail.daysOfStock <= 3 }">{{ suggestionDetail.daysOfStock }}天</span>
        </a-descriptions-item>
        <a-descriptions-item label="采购周期">{{ suggestionDetail.leadTime }}天</a-descriptions-item>
        <a-descriptions-item label="优先级">
          <a-progress :percent="suggestionDetail.priority" :stroke-color="getPriorityColor(suggestionDetail.priority)" />
        </a-descriptions-item>
        <a-descriptions-item label="预计到货日期">{{ formatDate(suggestionDetail.estimatedArrival) }}</a-descriptions-item>
        <a-descriptions-item label="建议原因" :span="2">{{ suggestionDetail.reason }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>销售趋势分析</a-divider>
      <div ref="salesTrendChartRef" class="chart-container"></div>
    </a-modal>

    <a-modal
      v-model:open="ignoreVisible"
      title="忽略补货建议"
      width="500px"
      @ok="confirmIgnore"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="产品名称">
          <span>{{ ignoreData.productName }}</span>
        </a-form-item>
        <a-form-item label="建议采购量">
          <span>{{ ignoreData.suggestedQty }}件</span>
        </a-form-item>
        <a-form-item label="忽略原因">
          <a-textarea v-model:value="ignoreReason" placeholder="请输入忽略原因" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="createOrderVisible"
      title="创建采购订单"
      width="600px"
      @ok="confirmCreateOrder"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="产品名称">
          <span>{{ createOrderData.productName }}</span>
        </a-form-item>
        <a-form-item label="建议采购量">
          <span>{{ createOrderData.suggestedQty }}件</span>
        </a-form-item>
        <a-form-item label="供应商" name="supplierId">
          <a-select v-model:value="selectedSupplierId" placeholder="请选择供应商" show-search :filter-option="filterOption">
            <a-select-option v-for="supplier in supplierList" :key="supplier.id" :value="supplier.id">
              {{ supplier.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="预计到货日期">
          <span>{{ formatDate(createOrderData.estimatedArrival) }}</span>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="reportVisible"
      title="补货建议报告"
      width="800px"
      :footer="null"
    >
      <a-row :gutter="16">
        <a-col :span="6">
          <a-statistic title="建议总数" :value="report.totalSuggestions" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="高优先级" :value="report.highPriorityCount" :value-style="{ color: '#f5222d' }" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="中优先级" :value="report.mediumPriorityCount" :value-style="{ color: '#faad14' }" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="低优先级" :value="report.lowPriorityCount" />
        </a-col>
      </a-row>
      <a-divider />
      <a-row :gutter="16">
        <a-col :span="8">
          <a-statistic title="总缺口数量" :value="report.totalShortageQty" suffix="件" />
        </a-col>
        <a-col :span="8">
          <a-statistic title="建议采购总量" :value="report.totalSuggestedQty" suffix="件" />
        </a-col>
        <a-col :span="8">
          <a-statistic title="预估采购成本" :value="report.estimatedCost" :precision="2" prefix="¥" />
        </a-col>
      </a-row>
      <a-divider>优先级分布</a-divider>
      <div ref="priorityChartRef" class="chart-container"></div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import type { TableProps } from 'ant-design-vue'
import * as echarts from 'echarts'
import { replenishmentApi, type ReplenishmentSuggestion } from '@/api/erp'

const loading = ref(false)
const activeTab = ref('pending')
const detailVisible = ref(false)
const ignoreVisible = ref(false)
const createOrderVisible = ref(false)
const reportVisible = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '产品信息', key: 'product', width: 180 },
  { title: '库存状态', key: 'stock', width: 150 },
  { title: '销售分析', key: 'analysis', width: 150 },
  { title: '建议采购量', key: 'suggestedQty', width: 120 },
  { title: '优先级', key: 'priority', width: 120 },
  { title: '预计到货', key: 'estimatedArrival', width: 120 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const processedColumns = [
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '建议采购量', dataIndex: 'suggestedQty', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '采购订单', key: 'purchaseOrder', width: 150 },
  { title: '处理时间', dataIndex: 'processTime', width: 150 }
]

const ignoredColumns = [
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '建议采购量', dataIndex: 'suggestedQty', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '忽略原因', key: 'ignoreReason', width: 200 },
  { title: '处理时间', dataIndex: 'processTime', width: 150 }
]

const pendingSuggestions = ref<any[]>([])
const processedSuggestions = ref<any[]>([])
const ignoredSuggestions = ref<any[]>([])

const suggestionDetail = ref<any>({})
const ignoreData = ref<any>({})
const createOrderData = ref<any>({})
const report = ref<any>({
  totalSuggestions: 15,
  highPriorityCount: 5,
  mediumPriorityCount: 7,
  lowPriorityCount: 3,
  totalShortageQty: 850,
  totalSuggestedQty: 1200,
  estimatedCost: 125000
})

const ignoreReason = ref('')
const selectedSupplierId = ref<number>()

const supplierList = ref([
  { id: 1, name: '北京办公用品有限公司' },
  { id: 2, name: '上海电子设备公司' },
  { id: 3, name: '广州物流运输公司' }
])

const salesTrendChartRef = ref<HTMLElement>()
const priorityChartRef = ref<HTMLElement>()
let salesTrendChart: echarts.ECharts | null = null
let priorityChart: echarts.ECharts | null = null

onMounted(() => {
  loadSuggestions()
})

onUnmounted(() => {
  salesTrendChart?.dispose()
  priorityChart?.dispose()
})

const loadSuggestions = async () => {
  loading.value = true
  try {
    const params = { pageNum: pagination.current, pageSize: pagination.pageSize, status: 'pending' }
    const res = await replenishmentApi.list(params)
    pendingSuggestions.value = res.records || []
    pagination.total = res.total
  } catch (err: any) {
    message.error('获取补货建议失败: ' + (err?.message || ''))
  } finally {
    loading.value = false
  }
}

const generateSuggestions = async () => {
  loading.value = true
  message.loading('正在生成补货建议...', 0)
  try {
    await replenishmentApi.generate()
    await loadSuggestions()
    message.destroy()
    message.success('补货建议已生成')
  } catch (err: any) {
    message.destroy()
    message.error('生成补货建议失败: ' + (err?.message || ''))
  } finally {
    loading.value = false
  }
}

const showReport = () => {
  reportVisible.value = true
  initPriorityChart()
}

const getPriorityColor = (priority: number) => {
  if (priority >= 80) return '#f5222d'
  if (priority >= 50) return '#faad14'
  return '#52c41a'
}

const formatDate = (date: string) => {
  if (!date) return ''
  return date.split('T')[0]
}

const filterOption = (input: string, option: any) => {
  return option.name.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  loadSuggestions()
}

const handleViewDetail = (record: any) => {
  suggestionDetail.value = record
  detailVisible.value = true
  initSalesTrendChart()
}

const handleIgnore = (record: any) => {
  ignoreData.value = record
  ignoreReason.value = ''
  ignoreVisible.value = true
}

const confirmIgnore = () => {
  if (!ignoreReason.value.trim()) {
    message.warning('请输入忽略原因')
    return
  }
  message.success('建议已忽略')
  ignoreVisible.value = false
  loadSuggestions()
}

const handleCreateOrder = (record: any) => {
  createOrderData.value = record
  selectedSupplierId.value = undefined
  createOrderVisible.value = true
}

const confirmCreateOrder = () => {
  if (!selectedSupplierId.value) {
    message.warning('请选择供应商')
    return
  }
  message.success('采购订单已创建')
  createOrderVisible.value = false
  loadSuggestions()
}

const goPurchaseOrder = (orderId: number) => {
  message.info(`跳转到采购订单: ${orderId}`)
}

const initSalesTrendChart = () => {
  if (!salesTrendChartRef.value) return
  salesTrendChart = echarts.init(salesTrendChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [{
      name: '销量',
      type: 'line',
      data: [3, 4, 2, 5, 3, 1, 2],
      smooth: true,
      areaStyle: { color: 'rgba(24, 144, 255, 0.2)' }
    }]
  }
  salesTrendChart.setOption(option)
}

const initPriorityChart = () => {
  if (!priorityChartRef.value) return
  priorityChart = echarts.init(priorityChartRef.value)
  const option = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      name: '优先级分布',
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { name: '高优先级', value: report.value.highPriorityCount, itemStyle: { color: '#f5222d' } },
        { name: '中优先级', value: report.value.mediumPriorityCount, itemStyle: { color: '#faad14' } },
        { name: '低优先级', value: report.value.lowPriorityCount, itemStyle: { color: '#52c41a' } }
      ]
    }]
  }
  priorityChart.setOption(option)
}
</script>

<style scoped lang="scss">
.replenishment-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    margin: 0;
  }
}

.product-info {
  .product-name {
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }

  .product-code {
    font-size: 12px;
    color: #999;
    margin-left: 8px;
  }
}

.stock-info {
  .stock-row {
    display: flex;
    margin-top: 4px;

    .stock-label {
      width: 70px;
      font-size: 12px;
      color: #999;
    }

    .stock-value {
      font-size: 12px;
      color: #333;

      &.shortage {
        color: #f5222d;
        font-weight: 500;
      }
    }
  }
}

.analysis-info {
  .analysis-row {
    display: flex;
    margin-top: 4px;

    .analysis-label {
      width: 70px;
      font-size: 12px;
      color: #999;
    }

    .analysis-value {
      font-size: 12px;
      color: #333;

      &.danger {
        color: #f5222d;
      }
    }
  }
}

.suggested-qty {
  color: #1890ff;
  font-weight: 500;
}

.arrival-date {
  font-size: 12px;
  color: #52c41a;
}

.shortage {
  color: #f5222d;
  font-weight: 600;
}

.suggested {
  color: #1890ff;
  font-weight: 600;
}

.ignore-reason {
  font-size: 12px;
  color: #999;
}

.chart-container {
  height: 250px;
}
</style>