<template>
  <div class="report-page">
    <!-- Summary Cards -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card hoverable @click="activeTab = 'summary'">
          <a-statistic title="资产原值" :value="depreciationSummary?.totalOriginalValue || 0" :precision="2" prefix="¥" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable @click="activeTab = 'summary'">
          <a-statistic title="累计折旧" :value="depreciationSummary?.totalAccumulatedDepreciation || 0" :precision="2" prefix="¥" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable @click="activeTab = 'summary'">
          <a-statistic title="资产净值" :value="depreciationSummary?.totalNetValue || 0" :precision="2" prefix="¥" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable @click="activeTab = 'summary'">
          <a-statistic title="资产数量" :value="depreciationSummary?.assetCount || 0" />
        </a-card>
      </a-col>
    </a-row>

    <!-- Tabs -->
    <a-card>
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="summary" tab="折旧汇总">
          <a-table
            :dataSource="monthlyData"
            :columns="depreciationColumns"
            :loading="loading"
            rowKey="period"
            :pagination="false"
            size="small"
          />
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
          <a-table
            :dataSource="ledgerData"
            :columns="ledgerColumns"
            :loading="ledgerLoading"
            rowKey="assetCode"
            :pagination="{ pageSize: 10 }"
            size="small"
          />
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
                <a-table
                  :dataSource="ageData"
                  :columns="ageColumns"
                  :loading="ageLoading"
                  rowKey="label"
                  :pagination="false"
                  size="small"
                />
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
                <a-table
                  :dataSource="categoryData"
                  :columns="categoryColumns"
                  :loading="categoryLoading"
                  rowKey="categoryId"
                  :pagination="false"
                  size="small"
                />
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
import { reportApi } from '@/api/fixed-asset'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent])

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

const depreciationColumns = [
  { title: '期间', dataIndex: 'period' },
  { title: '折旧笔数', dataIndex: 'count' },
  { title: '折旧金额', dataIndex: 'totalAmount' },
]

const ledgerColumns = [
  { title: '资产编码', dataIndex: 'assetCode' },
  { title: '资产名称', dataIndex: 'assetName' },
  { title: '分类', dataIndex: 'categoryName' },
  { title: '原值', dataIndex: 'originalValue' },
  { title: '累计折旧', dataIndex: 'accumulatedDepreciation' },
  { title: '净值', dataIndex: 'netValue' },
  { title: '状态', dataIndex: 'status' },
  { title: '部门', dataIndex: 'departmentName' },
  { title: '保管人', dataIndex: 'custodianName' },
]

const ageColumns = [
  { title: '账龄区间', dataIndex: 'label' },
  { title: '资产数量', dataIndex: 'count' },
  { title: '原值', dataIndex: 'originalValue' },
  { title: '净值', dataIndex: 'netValue' },
]

const categoryColumns = [
  { title: '分类名称', dataIndex: 'categoryName' },
  { title: '资产数量', dataIndex: 'count' },
  { title: '原值', dataIndex: 'originalValue' },
  { title: '净值', dataIndex: 'netValue' },
]

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
.report-page :deep(.ant-statistic) {
  text-align: center;
}
</style>
