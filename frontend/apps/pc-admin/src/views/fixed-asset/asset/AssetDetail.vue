<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">ERP / 固定资产 / 资产详情</span>
          <h2 class="page-header__title">{{ assetData?.assetName || '资产详情' }}</h2>
        </div>
        <div class="page-header__right">
          <a-space :size="8">
            <span v-if="lastUpdated" style="font-size: 12px; color: #999">更新于 {{ lastUpdated }}</span>
            <a-tag v-if="assetData" :color="statusColor">{{ statusLabel }}</a-tag>
            <PrintButton :record="assetData" business-type="fixed_asset" button-type="link" button-size="small" tooltip="打印" />
            <a-button v-permission="'erp:fixed-asset:asset:depreciate'" size="small" type="primary" @click="debounceClick('depreciate', handleDepreciate)" :loading="depreciateLoading">计提折旧</a-button>
            <a-button size="small" @click="goBack">返回</a-button>
          </a-space>
        </div>
      </div>
    </template>

    <!-- 加载态 -->
    <template v-if="loading && !assetData">
      <a-card size="small" style="margin-bottom: 12px">
        <a-skeleton active :paragraph="{ rows: 6 }" />
      </a-card>
    </template>

    <!-- 错误态 -->
    <template v-else-if="hasError">
      <a-result status="error" title="加载失败" sub-title="获取资产详情时发生错误">
        <template #extra>
          <a-button size="small" type="primary" @click="fetchData">重新加载</a-button>
        </template>
      </a-result>
    </template>

    <!-- 空数据 -->
    <template v-else-if="!assetData">
      <a-result status="404" title="资产不存在" sub-title="未找到该资产或已被删除">
        <template #extra>
          <a-button size="small" type="primary" @click="goBack">返回列表</a-button>
        </template>
      </a-result>
    </template>

    <!-- 正常内容 -->
    <template v-else>
      <div style="overflow-y: auto; height: 100%; padding-right: 4px">
        <a-card size="small" title="基本信息" style="margin-bottom: 12px">
          <a-descriptions :column="3" bordered size="small" :label-style="{ fontWeight: 600, background: '#fafafa', width: 120 }" :content-style="{ background: '#fff' }">
            <a-descriptions-item label="资产编码">{{ assetData.assetCode }}</a-descriptions-item>
            <a-descriptions-item label="资产名称">{{ assetData.assetName }}</a-descriptions-item>
            <a-descriptions-item label="分类">{{ assetData.categoryName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="购置日期">{{ assetData.purchaseDate || '-' }}</a-descriptions-item>
            <a-descriptions-item label="原值">{{ formatAmount(assetData.originalValue) }}</a-descriptions-item>
            <a-descriptions-item label="净值">{{ formatAmount(assetData.netValue) }}</a-descriptions-item>
            <a-descriptions-item label="折旧方法">{{ methodMap[assetData.depreciationMethod] || '-' }}</a-descriptions-item>
            <a-descriptions-item label="使用年限(月)">{{ assetData.usefulLife ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="残值">{{ formatAmount(assetData.salvageValue) }}</a-descriptions-item>
            <a-descriptions-item label="残值率">{{ assetData.salvageRate ?? 0 }}%</a-descriptions-item>
            <a-descriptions-item label="月折旧额">{{ formatAmount(assetData.monthlyDepreciation) }}</a-descriptions-item>
            <a-descriptions-item label="累计折旧">{{ formatAmount(assetData.accumulatedDepreciation) }}</a-descriptions-item>
            <a-descriptions-item label="状态">{{ statusMap[assetData.status] || assetData.status }}</a-descriptions-item>
            <a-descriptions-item label="使用状态">{{ useStatusMap[assetData.useStatus] || assetData.useStatus }}</a-descriptions-item>
            <a-descriptions-item label="部门">{{ assetData.departmentName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="保管人">{{ assetData.custodianName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="存放地点">{{ assetData.location || '-' }}</a-descriptions-item>
            <a-descriptions-item label="规格型号">{{ assetData.specification || '-' }}</a-descriptions-item>
            <a-descriptions-item label="品牌">{{ assetData.brand || '-' }}</a-descriptions-item>
            <a-descriptions-item label="供应商">{{ assetData.supplierName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="发票号">{{ assetData.invoiceNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="保修到期">{{ assetData.warrantyEndDate || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="3">{{ assetData.remark || '-' }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card size="small">
          <a-tabs v-model:activeKey="detailTab" size="small">
            <a-tab-pane key="depreciation" tab="折旧历史" :disabled="depreciationLoading">
              <VxeTableList
                :columns="depreciationVxeColumns"
                :data-source="depreciationData"
                :loading="depreciationLoading"
                row-key="id"
                :pagination="false"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              />
            </a-tab-pane>
            <a-tab-pane key="transfer" tab="转移记录">
              <VxeTableList
                :columns="transferVxeColumns"
                :data-source="transferData"
                :loading="transferLoading"
                row-key="id"
                :pagination="false"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              />
            </a-tab-pane>
            <a-tab-pane key="disposal" tab="处置信息">
              <VxeTableList
                :columns="disposalVxeColumns"
                :data-source="disposalData"
                :loading="disposalLoading"
                row-key="id"
                :pagination="false"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              />
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </div>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
defineOptions({ name: 'FixedAssetDetail' })

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer } from '@/components'
import { fixedAssetApi, depreciationApi, transferApi, disposalApi } from '@/api/fixed-asset'
import type { FixedAsset } from '@/api/fixed-asset'

const route = useRoute()
const router = useRouter()

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

function formatAmount(v: any): string {
  return v != null ? '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '¥0.00'
}

const assetId = ref(Number(route.params.id) || 0)
const assetData = ref<FixedAsset | null>(null)
const loading = ref(false)
const hasError = ref(false)
const detailTab = ref('depreciation')
const depreciateLoading = ref(false)
const lastUpdated = ref('')

const depreciationData = ref<any[]>([])
const transferData = ref<any[]>([])
const disposalData = ref<any[]>([])
const depreciationLoading = ref(false)
const transferLoading = ref(false)
const disposalLoading = ref(false)

const statusLabels: Record<string, string> = {
  draft: '草稿', active: '已启用', transferred: '已转移', disposed: '已处置', scrapped: '已报废',
}
const statusColors: Record<string, string> = {
  draft: 'default', active: 'green', transferred: 'blue', disposed: 'red', scrapped: 'orange',
}
const statusMap: Record<string, string> = statusLabels
const useStatusMap: Record<string, string> = {
  in_use: '使用中', idle: '闲置', maintenance: '维修中', disposed: '已处置',
}
const methodMap: Record<string, string> = {
  straight_line: '直线法', double_declining: '双倍余额递减法', sum_of_years: '年数总和法',
}

const statusLabel = computed(() => statusLabels[assetData.value?.status || ''] || assetData.value?.status || '-')
const statusColor = computed(() => statusColors[assetData.value?.status || ''] || 'default')

const formatAmountCell = ({ cellValue }: any) => formatAmount(cellValue)
const depreciationVxeColumns = [
  { field: 'period', title: '期间', width: 100 },
  { field: 'depreciationDate', title: '折旧日期', width: 120 },
  { field: 'periodAmount', title: '本期折旧', width: 130, align: 'right', formatter: formatAmountCell },
  { field: 'accumulatedDepreciation', title: '累计折旧', width: 130, align: 'right', formatter: formatAmountCell },
  { field: 'netValue', title: '净值', width: 130, align: 'right', formatter: formatAmountCell },
  { field: 'status', title: '状态', width: 80 },
]

const transferVxeColumns = [
  { field: 'transferNo', title: '转移单号', width: 140 },
  { field: 'fromDepartmentName', title: '调出部门', width: 120 },
  { field: 'toDepartmentName', title: '调入部门', width: 120 },
  { field: 'transferDate', title: '转移日期', width: 120 },
  { field: 'status', title: '状态', width: 80 },
]

const formatGainLoss = ({ cellValue }: any) => {
  if (cellValue == null) return '-'
  const prefix = cellValue >= 0 ? '' : '-'
  return prefix + '¥' + Math.abs(cellValue).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
const disposalVxeColumns = [
  { field: 'disposalNo', title: '处置单号', width: 140 },
  { field: 'disposalType', title: '处置类型', width: 100 },
  { field: 'disposalDate', title: '处置日期', width: 120 },
  { field: 'disposalAmount', title: '处置金额', width: 130, align: 'right', formatter: formatAmountCell },
  { field: 'gainLoss', title: '处置损益', width: 130, align: 'right', formatter: formatGainLoss },
  { field: 'status', title: '状态', width: 80 },
]

async function fetchAssetDetail() {
  try {
    const res = await fixedAssetApi.getById(assetId.value) as any
    assetData.value = res.data || res
    lastUpdated.value = new Date().toLocaleString()
  } catch (e: any) {
    console.warn('[资产详情] 加载失败', e)
    if (e?.response?.status === 404) {
      assetData.value = null as any
    } else {
      hasError.value = true
    }
  }
}

async function fetchDepreciationHistory() {
  if (!assetId.value) return
  depreciationLoading.value = true
  try {
    const res = await depreciationApi.getPage({ assetId: assetId.value, page: 0, size: 200 }) as any
    depreciationData.value = res.data?.content || res.data?.records || []
  } catch {
    depreciationData.value = []
  } finally {
    depreciationLoading.value = false
  }
}

async function fetchTransferHistory() {
  if (!assetId.value) return
  transferLoading.value = true
  try {
    // API may not support assetId filter; fetch all records and filter client-side
    const res = await transferApi.getPage({ page: 0, size: 200 }) as any
    const all = res.data?.content || res.data?.records || []
    transferData.value = all.filter((r: any) => r.assetId === assetId.value)
  } catch {
    transferData.value = []
  } finally {
    transferLoading.value = false
  }
}

async function fetchDisposalHistory() {
  if (!assetId.value) return
  disposalLoading.value = true
  try {
    const res = await disposalApi.getPage({ page: 0, size: 200 }) as any
    const all = res.data?.content || res.data?.records || []
    disposalData.value = all.filter((r: any) => r.assetId === assetId.value)
  } catch {
    disposalData.value = []
  } finally {
    disposalLoading.value = false
  }
}

async function fetchData() {
  loading.value = true
  hasError.value = false
  await Promise.all([
    fetchAssetDetail(),
    fetchDepreciationHistory(),
    fetchTransferHistory(),
    fetchDisposalHistory(),
  ])
  loading.value = false
}

async function handleDepreciate() {
  depreciateLoading.value = true
  try {
    await fixedAssetApi.depreciate(assetId.value)
    message.success('折旧计提成功')
    await Promise.all([fetchAssetDetail(), fetchDepreciationHistory()])
  } catch (e: any) {
    message.error(e?.message || '折旧失败')
  } finally {
    depreciateLoading.value = false
  }
}

function goBack() {
  router.back()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement) && !(e.target instanceof HTMLSelectElement)) {
    e.preventDefault()
    debounceClick('refresh', fetchData)()
  }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('fixed-asset:refresh', fetchData)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('fixed-asset:refresh', fetchData)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; flex-direction: column; gap: 2px; }
.page-header__breadcrumb { font-size: 12px; color: #999; }
.page-header__title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 8px; }
</style>
