<template>
  <ErrorBoundary>
    <PageContainer title="预警查询">
      <template #header-extra>
        <a-space>
          <a-badge :count="statistics.overStockCount" :overflow-count="999" size="small">
            <a-button size="small" @click="activeTab = 'overStock'">
              <template #icon><FireOutlined /></template>超储
            </a-button>
          </a-badge>
          <a-badge :count="statistics.lowStockCount" :overflow-count="999" size="small" :offset="[4, -4]">
            <a-button size="small" type="primary" ghost @click="activeTab = 'lowStock'">
              <template #icon><BellOutlined /></template>低库存
            </a-button>
          </a-badge>
          <a-button size="small" @click="openAlertConfig">
            <template #icon><SettingOutlined /></template>预警配置
          </a-button>
        </a-space>
      </template>

      <!-- 统计卡片 -->
      <a-row :gutter="16" style="margin-bottom: 16px;">
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
              <AlertOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">预警条目</div>
              <div class="summary-value">{{ statistics.totalAlerts }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card highlight-danger">
            <div class="summary-icon" style="background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);">
              <MinusCircleOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">低库存预警</div>
              <div class="summary-value warning">{{ statistics.lowStockCount }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card highlight-warning">
            <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
              <PlusCircleOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">超储预警</div>
              <div class="summary-value warning">{{ statistics.overStockCount }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
              <ShoppingCartOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">可补货</div>
              <div class="summary-value">{{ statistics.replenishable }}</div>
            </div>
          </div>
        </a-col>
      </a-row>

      <a-tabs v-model:active-key="activeTab" @change="reload">
        <a-tab-pane key="all" tab="全部预警">
          <ARReportPage
            ref="reportRef"
            title="预警查询"
            :query-fields="queryFields"
            :columns="columns"
            :fetcher="fetcher"
            export-file-name="预警查询"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'statusText'">
                <a-tag :color="record.alertType === 'LOW_STOCK' ? '#f5222d' : '#faad14'">
                  {{ record.alertType === 'LOW_STOCK' ? '低库存' : record.alertType === 'OVER_STOCK' ? '超储' : record.alertType }}
                </a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'diffQty'">
                <span :style="{ color: record.diffQty < 0 ? '#f5222d' : '#faad14', fontWeight: 600 }">
                  {{ record.diffQty > 0 ? '+' : '' }}{{ formatQty(record.diffQty) }}
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'currentQty'">
                <span :style="{ color: record.currentQty < (record.safetyStock || record.minStock) ? '#f5222d' : 'inherit', fontWeight: record.currentQty < (record.safetyStock || record.minStock) ? 600 : 'inherit' }">
                  {{ formatQty(record.currentQty) }}
                </span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space :size="4">
                  <a-button v-if="record.alertType === 'LOW_STOCK'" size="small" type="link" @click="goReplenish(record)">补货</a-button>
                  <a-button size="small" type="link" @click="viewDetail(record)">详情</a-button>
                </a-space>
              </template>
            </template>
          </ARReportPage>
        </a-tab-pane>
        <a-tab-pane key="lowStock" tab="低库存">
          <ARReportPage
            ref="lowStockRef"
            title="低库存预警"
            :query-fields="queryFields"
            :columns="columns"
            :fetcher="lowStockFetcher"
            export-file-name="低库存预警"
            row-key="id"
          />
        </a-tab-pane>
        <a-tab-pane key="overStock" tab="超储">
          <ARReportPage
            ref="overStockRef"
            title="超储预警"
            :query-fields="queryFields"
            :columns="columns"
            :fetcher="overStockFetcher"
            export-file-name="超储预警"
            row-key="id"
          />
        </a-tab-pane>
      </a-tabs>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  SearchOutlined, ClearOutlined, SettingOutlined, AlertOutlined,
  FireOutlined, BellOutlined, MinusCircleOutlined, PlusCircleOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import request from '@/utils/request'

const router = useRouter()
const activeTab = ref('all')
const reportRef = ref<any>(null)
const lowStockRef = ref<any>(null)
const overStockRef = ref<any>(null)

const statistics = reactive({ totalAlerts: 0, lowStockCount: 0, overStockCount: 0, replenishable: 0 })

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

const queryFields: ReportQueryField[] = [
  { key: 'productCode', type: 'input', label: '产品编码', placeholder: '产品编码', width: 140 },
  { key: 'productName', type: 'input', label: '产品名称', placeholder: '产品名称', width: 140 },
  { key: 'warehouseId', type: 'input', label: '仓库ID', placeholder: '仓库ID', width: 100 },
  { key: 'alertType', type: 'select', label: '预警类型', placeholder: '全部', options: [
    { label: '全部', value: '' }, { label: '低库存', value: 'LOW_STOCK' }, { label: '超储', value: 'OVER_STOCK' },
  ]},
]

const columns: any[] = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 130 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 160, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '当前库存', dataIndex: 'currentQty', key: 'currentQty', width: 100, align: 'right' },
  { title: '最小库存', dataIndex: 'minStock', key: 'minStock', width: 90, align: 'right' },
  { title: '最高库存', dataIndex: 'maxStock', key: 'maxStock', width: 90, align: 'right' },
  { title: '安全库存', dataIndex: 'safetyStock', key: 'safetyStock', width: 90, align: 'right' },
  { title: '差异数量', dataIndex: 'diffQty', key: 'diffQty', width: 100, align: 'right' },
  { title: '预警类型', dataIndex: 'statusText', key: 'statusText', width: 90 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' },
]

function fetcher(params: Record<string, any>) {
  return request.get('/erp/stock-alert/page', { params: { ...params, pageNum: params.page, pageSize: params.size } })
}

function lowStockFetcher(params: Record<string, any>) {
  return request.get('/erp/stock-alert/page', { params: { ...params, pageNum: params.page, pageSize: params.size, alertType: 'LOW_STOCK' } })
}

function overStockFetcher(params: Record<string, any>) {
  return request.get('/erp/stock-alert/page', { params: { ...params, pageNum: params.page, pageSize: params.size, alertType: 'OVER_STOCK' } })
}

async function loadStatistics() {
  try {
    const res = await request.get('/erp/stock-alert/statistics')
    if (res?.data) Object.assign(statistics, res.data)
  } catch { /* ignore */ }
}

function reload() {
  reportRef.value?.reload()
  lowStockRef.value?.reload()
  overStockRef.value?.reload()
  loadStatistics()
}

function goReplenish(record: any) {
  router.push({ path: '/erp/stock/replenishment', query: { productId: record.productId, productName: record.productName } })
}

function viewDetail(record: any) {
  message.info(`产品: ${record.productName}, 当前库存: ${record.currentQty}, 安全库存: ${record.safetyStock}`)
}

function openAlertConfig() {
  router.push('/erp/stock-alert-config')
}

onMounted(loadStatistics)
</script>

<style scoped>
.summary-card { display: flex; align-items: center; padding: 16px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); transition: all 0.3s; }
.summary-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.12); transform: translateY(-2px); }
.summary-card.highlight-danger { background: linear-gradient(135deg, #fff2f0 0%, #fff1f0 100%); border: 1px solid #ffa39e; }
.summary-card.highlight-warning { background: linear-gradient(135deg, #fffbe6 0%, #fff7e6 100%); border: 1px solid #ffe58f; }
.summary-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px; margin-right: 16px; }
.summary-content { flex: 1; }
.summary-title { font-size: 14px; color: #666; margin-bottom: 4px; }
.summary-value { font-size: 24px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, monospace; font-variant-numeric: tabular-nums; }
.summary-value.warning { color: #f5222d; }
</style>
