<template>
  <div class="asset-detail">
    <a-page-header
      :title="assetData?.assetName || '资产详情'"
      :sub-title="assetData?.assetCode"
      @back="goBack"
    >
      <template #tags>
        <a-tag :color="statusColor">{{ statusLabel }}</a-tag>
      </template>
      <template #extra>
        <a-button type="primary" @click="handleDepreciate">计提折旧</a-button>
        <a-button @click="goBack">返回列表</a-button>
      </template>
    </a-page-header>

    <a-card title="基本信息" style="margin-bottom: 16px">
      <a-descriptions :column="3" bordered :label-style="{ fontWeight: 'bold' }">
        <a-descriptions-item label="资产编码">{{ assetData?.assetCode }}</a-descriptions-item>
        <a-descriptions-item label="资产名称">{{ assetData?.assetName }}</a-descriptions-item>
        <a-descriptions-item label="分类">{{ assetData?.categoryName }}</a-descriptions-item>
        <a-descriptions-item label="购置日期">{{ assetData?.purchaseDate }}</a-descriptions-item>
        <a-descriptions-item label="原值">¥{{ assetData?.originalValue?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="净值">¥{{ assetData?.netValue?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="折旧方法">{{ methodMap[assetData?.depreciationMethod] }}</a-descriptions-item>
        <a-descriptions-item label="使用年限(月)">{{ assetData?.usefulLife }}</a-descriptions-item>
        <a-descriptions-item label="残值">¥{{ assetData?.salvageValue?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="残值率">{{ assetData?.salvageRate }}%</a-descriptions-item>
        <a-descriptions-item label="月折旧额">¥{{ assetData?.monthlyDepreciation?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="累计折旧">¥{{ assetData?.accumulatedDepreciation?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ statusMap[assetData?.status] }}</a-descriptions-item>
        <a-descriptions-item label="使用状态">{{ useStatusMap[assetData?.useStatus] }}</a-descriptions-item>
        <a-descriptions-item label="部门">{{ assetData?.departmentName }}</a-descriptions-item>
        <a-descriptions-item label="保管人">{{ assetData?.custodianName }}</a-descriptions-item>
        <a-descriptions-item label="存放地点">{{ assetData?.location }}</a-descriptions-item>
        <a-descriptions-item label="规格型号">{{ assetData?.specification }}</a-descriptions-item>
        <a-descriptions-item label="品牌">{{ assetData?.brand }}</a-descriptions-item>
        <a-descriptions-item label="供应商">{{ assetData?.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="发票号">{{ assetData?.invoiceNo }}</a-descriptions-item>
        <a-descriptions-item label="保修到期">{{ assetData?.warrantyEndDate }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="3">{{ assetData?.remark }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card>
      <a-tabs v-model:activeKey="detailTab">
        <a-tab-pane key="depreciation" tab="折旧历史">
          <a-table
            :dataSource="depreciationData"
            :columns="depreciationColumns"
            :loading="depreciationLoading"
            rowKey="id"
            :pagination="false"
            size="small"
          />
        </a-tab-pane>
        <a-tab-pane key="transfer" tab="转移记录">
          <a-table
            :dataSource="transferData"
            :columns="transferColumns"
            :loading="transferLoading"
            rowKey="id"
            :pagination="false"
            size="small"
          />
        </a-tab-pane>
        <a-tab-pane key="disposal" tab="处置信息">
          <a-table
            :dataSource="disposalData"
            :columns="disposalColumns"
            :loading="disposalLoading"
            rowKey="id"
            :pagination="false"
            size="small"
          />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fixedAssetApi, depreciationApi, transferApi, disposalApi } from '@/api/fixed-asset'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const assetId = ref(Number(route.params.id) || 0)
const assetData = ref<any>(null)
const detailTab = ref('depreciation')

const depreciationData = ref([])
const transferData = ref([])
const disposalData = ref([])
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

const statusLabel = computed(() => statusLabels[assetData.value?.status] || assetData.value?.status)
const statusColor = computed(() => statusColors[assetData.value?.status] || 'default')

const depreciationColumns = [
  { title: '期间', dataIndex: 'period', width: 100 },
  { title: '折旧日期', dataIndex: 'depreciationDate', width: 120 },
  { title: '本期折旧', dataIndex: 'periodAmount', width: 120 },
  { title: '累计折旧', dataIndex: 'accumulatedDepreciation', width: 120 },
  { title: '净值', dataIndex: 'netValue', width: 120 },
  { title: '状态', dataIndex: 'status', width: 80 },
]

const transferColumns = [
  { title: '转移单号', dataIndex: 'transferNo', width: 140 },
  { title: '调出部门', dataIndex: 'fromDepartmentName', width: 120 },
  { title: '调入部门', dataIndex: 'toDepartmentName', width: 120 },
  { title: '转移日期', dataIndex: 'transferDate', width: 120 },
  { title: '状态', dataIndex: 'status', width: 80 },
]

const disposalColumns = [
  { title: '处置单号', dataIndex: 'disposalNo', width: 140 },
  { title: '处置类型', dataIndex: 'disposalType', width: 100 },
  { title: '处置日期', dataIndex: 'disposalDate', width: 120 },
  { title: '处置金额', dataIndex: 'disposalAmount', width: 120 },
  { title: '处置损益', dataIndex: 'gainLoss', width: 120 },
  { title: '状态', dataIndex: 'status', width: 80 },
]

onMounted(() => {
  if (assetId.value) {
    fetchAssetDetail()
    fetchDepreciationHistory()
    fetchTransferHistory()
    fetchDisposalHistory()
  }
})

function fetchAssetDetail() {
  fixedAssetApi.getById(assetId.value).then((res: any) => {
    assetData.value = res.data
  })
}

function fetchDepreciationHistory() {
  depreciationLoading.value = true
  depreciationApi.getPage({ assetId: assetId.value, page: 0, size: 100 }).then((res: any) => {
    depreciationData.value = res.data?.content || res.data?.records || []
  }).finally(() => {
    depreciationLoading.value = false
  })
}

function fetchTransferHistory() {
  transferLoading.value = true
  transferApi.getPage({ page: 0, size: 100 }).then((res: any) => {
    const all = res.data?.content || res.data?.records || []
    transferData.value = all.filter((r: any) => r.assetId === assetId.value)
  }).finally(() => {
    transferLoading.value = false
  })
}

function fetchDisposalHistory() {
  disposalLoading.value = true
  disposalApi.getPage({ page: 0, size: 100 }).then((res: any) => {
    const all = res.data?.content || res.data?.records || []
    disposalData.value = all.filter((r: any) => r.assetId === assetId.value)
  }).finally(() => {
    disposalLoading.value = false
  })
}

function handleDepreciate() {
  fixedAssetApi.depreciate(assetId.value).then(() => {
    message.success('折旧计提成功')
    fetchAssetDetail()
    fetchDepreciationHistory()
  }).catch((err: any) => {
    message.error(err.message || '折旧失败')
  })
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.asset-detail {
  padding: 16px;
}
</style>
