<template>
  <div class="replenishment-page" style="padding: 16px; height: 100%; display: flex; flex-direction: column;">
    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <AlertOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待处理建议</div>
            <div class="summary-value">{{ statistics.pendingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);">
            <FireOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">高优先级</div>
            <div class="summary-value warning">{{ statistics.highPriorityCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已生成采购</div>
            <div class="summary-value">{{ statistics.processedCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
            <DollarOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">预估采购成本</div>
            <div class="summary-value">¥{{ formatAmount(statistics.estimatedCost) }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card :bordered="false" style="flex: 1; overflow: hidden;" :bodyStyle="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }">
      <div class="page-header" style="margin-bottom: 16px;">
        <h2 style="margin: 0;">智能补货建议</h2>
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

      <a-tabs v-model:activeKey="activeTab" style="flex: 1; overflow: hidden;">
        <a-tab-pane key="pending" tab="待处理">
          <VxeTableList
            ref="pendingTableRef"
            :columns="pendingVxeColumns"
            :data-source="pendingSuggestions"
            :loading="loading"
            :pagination="pagination"
            row-key="id"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
            @page-change="handlePageChange"
          >
            <template #productCell="{ record }">
              <div class="product-info">
                <span class="product-name">{{ record.productName }}</span>
                <span class="product-code">{{ record.productCode }}</span>
              </div>
            </template>
            <template #stockCell="{ record }">
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
            <template #analysisCell="{ record }">
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
            <template #suggestedQtyCell="{ record }">
              <span class="suggested-qty">建议采购 {{ record.suggestedQty }} 件</span>
            </template>
            <template #priorityCell="{ record }">
              <a-progress :percent="record.priority" :stroke-color="getPriorityColor(record.priority)" :show-info="true" size="small" />
            </template>
            <template #estimatedArrivalCell="{ record }">
              <span class="arrival-date">{{ formatDate(record.estimatedArrival) }}</span>
            </template>
            <template #action="{ record }">
              <a-space>
                <a-button size="small" type="primary" @click="handleCreateOrder(record)">创建采购单</a-button>
                <a-button size="small" @click="handleIgnore(record)">忽略</a-button>
                <a @click="handleViewDetail(record)">详情</a>
              </a-space>
            </template>
          </VxeTableList>
        </a-tab-pane>
        <a-tab-pane key="processed" tab="已处理">
          <VxeTableList
            :columns="processedVxeColumns"
            :data-source="processedSuggestions"
            :loading="loading"
            :pagination="false"
            row-key="id"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
          >
            <template #statusCell="{ record }">
              <a-tag color="green">已生成采购单</a-tag>
            </template>
            <template #purchaseOrderCell="{ record }">
              <a @click="goPurchaseOrder(record.purchaseOrderId)">{{ record.purchaseOrderNo }}</a>
            </template>
          </VxeTableList>
        </a-tab-pane>
        <a-tab-pane key="ignored" tab="已忽略">
          <VxeTableList
            :columns="ignoredVxeColumns"
            :data-source="ignoredSuggestions"
            :loading="loading"
            :pagination="false"
            row-key="id"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
          >
            <template #statusCell="{ record }">
              <a-tag color="default">已忽略</a-tag>
            </template>
            <template #ignoreReasonCell="{ record }">
              <span class="ignore-reason">{{ record.ignoreReason }}</span>
            </template>
          </VxeTableList>
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, AlertOutlined, FireOutlined, CheckCircleOutlined, DollarOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import * as echarts from 'echarts'
import { replenishmentApi, type ReplenishmentSuggestion } from '@/api/erp'

const loading = ref(false)
const activeTab = ref('pending')
const detailVisible = ref(false)
const ignoreVisible = ref(false)
const createOrderVisible = ref(false)
const reportVisible = ref(false)
const pendingTableRef = ref()

// 统计数据
const statistics = ref({
  pendingCount: 0,
  highPriorityCount: 0,
  processedCount: 0,
  estimatedCost: 0
})

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const pendingVxeColumns = computed(() => [
  { field: 'productName', title: '产品信息', width: 180, slotName: 'productCell' },
  { field: 'stock', title: '库存状态', width: 150, slotName: 'stockCell' },
  { field: 'analysis', title: '销售分析', width: 150, slotName: 'analysisCell' },
  { field: 'suggestedQty', title: '建议采购量', width: 120, slotName: 'suggestedQtyCell' },
  { field: 'priority', title: '优先级', width: 120, slotName: 'priorityCell' },
  { field: 'estimatedArrival', title: '预计到货', width: 120, slotName: 'estimatedArrivalCell' },
  { field: 'action', title: '操作', width: 200, fixed: 'right', type: 'action' },
])

const processedVxeColumns = computed(() => [
  { field: 'productName', title: '产品名称', width: 180 },
  { field: 'suggestedQty', title: '建议采购量', width: 120 },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'purchaseOrder', title: '采购订单', width: 150, slotName: 'purchaseOrderCell' },
  { field: 'processTime', title: '处理时间', width: 150 },
])

const ignoredVxeColumns = computed(() => [
  { field: 'productName', title: '产品名称', width: 180 },
  { field: 'suggestedQty', title: '建议采购量', width: 120 },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'ignoreReason', title: '忽略原因', width: 200, slotName: 'ignoreReasonCell' },
  { field: 'processTime', title: '处理时间', width: 150 },
])

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
    // 更新统计
    statistics.value.pendingCount = pendingSuggestions.value.length
    statistics.value.highPriorityCount = pendingSuggestions.value.filter(s => s.priority >= 80).length
    statistics.value.processedCount = processedSuggestions.value.length
    statistics.value.estimatedCost = pendingSuggestions.value.reduce((sum, s) => sum + (s.suggestedQty * s.avgPrice || 0), 0)
  } catch (err: any) {
    message.error('获取补货建议失败: ' + (err?.message || ''))
    // Mock 数据
    pendingSuggestions.value = mockSuggestions()
    statistics.value.pendingCount = pendingSuggestions.value.length
    statistics.value.highPriorityCount = pendingSuggestions.value.filter(s => s.priority >= 80).length
    statistics.value.estimatedCost = pendingSuggestions.value.reduce((sum, s) => sum + (s.suggestedQty * 100 || 0), 0)
  } finally {
    loading.value = false
  }
}

const mockSuggestions = () => [
  { id: 1, productName: '工业传感器', productCode: 'P001', currentQty: 50, safetyStock: 100, shortageQty: 50, avgDailySales: 5, daysOfStock: 10, leadTime: 7, suggestedQty: 150, priority: 85, estimatedArrival: '2024-02-01' },
  { id: 2, productName: '智能控制器', productCode: 'P002', currentQty: 30, safetyStock: 80, shortageQty: 50, avgDailySales: 8, daysOfStock: 4, leadTime: 5, suggestedQty: 200, priority: 90, estimatedArrival: '2024-01-28' },
  { id: 3, productName: '连接线缆套装', productCode: 'P003', currentQty: 100, safetyStock: 150, shortageQty: 50, avgDailySales: 10, daysOfStock: 10, leadTime: 3, suggestedQty: 100, priority: 60, estimatedArrival: '2024-01-25' }
]

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

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
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
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.warning {
  color: #f5222d;
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
