<template>
  <div class="report-page">
    <!-- Summary Cards -->
    <div class="stat-cards">
      <div class="stat-card stat-original" @click="activeTab = 'summary'">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(depreciationSummary?.totalOriginalValue || 0) }}</div>
          <div class="stat-card-label">资产原值</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-depreciation" @click="activeTab = 'summary'">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(depreciationSummary?.totalAccumulatedDepreciation || 0) }}</div>
          <div class="stat-card-label">累计折旧</div>
        </div>
        <CalculatorOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-net" @click="activeTab = 'summary'">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(depreciationSummary?.totalNetValue || 0) }}</div>
          <div class="stat-card-label">资产净值</div>
        </div>
        <LineChartOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-count" @click="activeTab = 'summary'">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ depreciationSummary?.assetCount || 0 }}</div>
          <div class="stat-card-label">资产数量</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
    </div>

    <!-- Tabs -->
    <a-card class="report-tabs-card">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="summary" tab="折旧汇总">
          <VxeTableList :columns="depreciationVxeColumns" :data-source="monthlyData" :loading="loading" row-key="period" :pagination="false" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false" />
        </a-tab-pane>

        <a-tab-pane key="ledger" tab="资产台账">
          <a-form layout="inline" style="margin-bottom: 16px">
            <a-form-item label="资产编码">
              <a-input v-model:value="ledgerParams.assetCode" placeholder="资产编码" allow-clear />
            </a-form-item>
            <a-form-item label="部门">
              <a-input v-model:value="ledgerParams.departmentId" placeholder="部门ID" allow-clear />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="fetchLedger">查询</a-button>
            </a-form-item>
          </a-form>
          <VxeTableList :columns="ledgerVxeColumns" :data-source="ledgerData" :loading="ledgerLoading" row-key="assetCode" :pagination="{ pageSize: 10 }" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false" />
        </a-tab-pane>

        <a-tab-pane key="age" tab="账龄分析">
          <a-row :gutter="16" style="margin-bottom: 16px">
            <a-col :span="12">
              <a-card title="按账龄分布">
                <v-chart :option="ageChartOption" style="height: 350px" autoresize />
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="账龄明细">
                <VxeTableList :columns="ageVxeColumns" :data-source="ageData" :loading="ageLoading" row-key="label" :pagination="false" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false" />
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>

        <a-tab-pane key="category" tab="分类汇总">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="分类资产分布">
                <v-chart :option="categoryChartOption" style="height: 350px" autoresize />
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="分类明细">
                <VxeTableList :columns="categoryVxeColumns" :data-source="categoryData" :loading="categoryLoading" row-key="categoryId" :pagination="false" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false" />
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { reportApi } from '@/api/fixed-asset'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import {
  DollarOutlined, CalculatorOutlined, LineChartOutlined, FileTextOutlined
} from '@ant-design/icons-vue'

use([CanvasRenderer, PieChart, BarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent])

const activeTab = ref('summary')
const loading = ref(false)
const ledgerLoading = ref(false)
const ageLoading = ref(false)
const categoryLoading = ref(false)

const depreciationSummary = ref<any>({})
const monthlyData = ref([])
const ledgerData = ref([])
const ageData = ref([])
const categoryData = ref([])

const ledgerParams = reactive({
  assetCode: undefined as string | undefined,
  departmentId: undefined as string | undefined,
})

const depreciationVxeColumns = [
  { field: 'period', title: '期间' },
  { field: 'count', title: '折旧笔数' },
  { field: 'totalAmount', title: '折旧金额' },
]

const ledgerVxeColumns = [
  { field: 'assetCode', title: '资产编码' },
  { field: 'assetName', title: '资产名称' },
  { field: 'categoryName', title: '分类' },
  { field: 'originalValue', title: '原值' },
  { field: 'accumulatedDepreciation', title: '累计折旧' },
  { field: 'netValue', title: '净值' },
  { field: 'status', title: '状态' },
  { field: 'departmentName', title: '部门' },
  { field: 'custodianName', title: '保管人' },
]

const ageVxeColumns = [
  { field: 'label', title: '账龄区间' },
  { field: 'count', title: '资产数量' },
  { field: 'originalValue', title: '原值' },
  { field: 'netValue', title: '净值' },
]

const categoryVxeColumns = [
  { field: 'categoryName', title: '分类名称' },
  { field: 'count', title: '资产数量' },
  { field: 'originalValue', title: '原值' },
  { field: 'netValue', title: '净值' },
]

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const ageChartOption = computed(() => ({
  title: { text: '资产账龄分析', left: 'center' },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ageData.value.map((d: any) => d.label) },
  yAxis: { type: 'value', name: '金额' },
  series: [
    {
      name: '原值',
      type: 'bar',
      data: ageData.value.map((d: any) => d.originalValue),
    },
    {
      name: '净值',
      type: 'bar',
      data: ageData.value.map((d: any) => d.netValue),
    },
  ],
}))

const categoryChartOption = computed(() => ({
  title: { text: '分类资产分布', left: 'center' },
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      data: categoryData.value.map((d: any) => ({
        name: d.categoryName,
        value: d.count,
      })),
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' },
      },
    },
  ],
}))

onMounted(() => {
  fetchDepreciationSummary()
  fetchAgeAnalysis()
  fetchCategorySummary()
})

function fetchDepreciationSummary() {
  loading.value = true
  reportApi.getDepreciationSummary().then((res: any) => {
    const data = res.data?.[0]
    if (data) {
      depreciationSummary.value = data
      monthlyData.value = data.monthlyData || []
    }
  }).finally(() => {
    loading.value = false
  })
}

function fetchLedger() {
  ledgerLoading.value = true
  const params: any = {}
  if (ledgerParams.assetCode) params.assetCode = ledgerParams.assetCode
  if (ledgerParams.departmentId) params.departmentId = ledgerParams.departmentId

  reportApi.getAssetLedger(params).then((res: any) => {
    ledgerData.value = res.data || []
  }).finally(() => {
    ledgerLoading.value = false
  })
}

function fetchAgeAnalysis() {
  ageLoading.value = true
  reportApi.getAgeAnalysis().then((res: any) => {
    ageData.value = res.data || []
  }).finally(() => {
    ageLoading.value = false
  })
}

function fetchCategorySummary() {
  categoryLoading.value = true
  reportApi.getCategorySummary().then((res: any) => {
    categoryData.value = res.data || []
  }).finally(() => {
    categoryLoading.value = false
  })
}
</script>

<style scoped>
.report-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-original { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-depreciation { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-net { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

/* Tabs Card */
.report-tabs-card {
  flex: 1;
  min-height: 0;
}

:deep(.ant-card-body) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.ant-tabs) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

:deep(.ant-tabs-content) {
  flex: 1;
}

:deep(.ant-tabs-tabpane) {
  height: 100%;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}
</style>
