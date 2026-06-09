<template>
  <DetailLayout
    :breadcrumb-items="breadcrumbItems"
    :title="supplier?.supplierName || ''"
    :status="getStatusLabel(supplier?.cooperationStatus)"
    :status-type="getStatusType(supplier?.cooperationStatus)"
    :show-pager="true"
    :current-index="currentIndex"
    :total-count="totalCount"
    :tabs="tabs"
    :active-tab="activeTab"
    :show-related-documents="false"
    :show-activity-log="true"
    :activity-logs="activityLogs"
    :loading="loading"
    :error="error"
    @breadcrumb-click="handleBreadcrumbClick"
    @prev="handlePrev"
    @next="handleNext"
    @tab-change="handleTabChange"
  >
    <template #header-extra>
      <a-tag :color="getLevelColor(supplier?.supplierLevel)">
        {{ supplier?.supplierLevel }}级供应商
      </a-tag>
    </template>

    <template #actions>
      <a-button type="primary" @click="handleEdit">编辑</a-button>
      <a-button @click="handleEvaluate">绩效评估</a-button>
      <a-button @click="handleAddPoints">增加积分</a-button>
      <a-button @click="handleConsumePoints">消费积分</a-button>
    </template>

    <!-- 基本信息 Tab -->
    <template #tab-basic>
      <a-row :gutter="24">
        <a-col :span="12">
          <a-descriptions :column="1" bordered size="small" title="基础信息">
            <a-descriptions-item label="供应商编码">{{ supplier?.supplierCode }}</a-descriptions-item>
            <a-descriptions-item label="供应商名称">{{ supplier?.supplierName }}</a-descriptions-item>
            <a-descriptions-item label="简称">{{ supplier?.shortName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="供应商等级">
              <a-tag :color="getLevelColor(supplier?.supplierLevel)">{{ supplier?.supplierLevel }}级</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="合作状态">
              <a-tag :color="getStatusColor(supplier?.cooperationStatus)">
                {{ getStatusLabel(supplier?.cooperationStatus) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="门户状态">
              <a-tag :color="supplier?.portalStatus === 1 ? 'purple' : 'default'">
                {{ supplier?.portalStatus === 1 ? '已激活' : supplier?.portalStatus === 2 ? '已禁用' : '未激活' }}
              </a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-col>
        <a-col :span="12">
          <a-descriptions :column="1" bordered size="small" title="联系信息">
            <a-descriptions-item label="联系人">{{ supplier?.contactPerson || '-' }}</a-descriptions-item>
            <a-descriptions-item label="联系电话">{{ supplier?.contactPhone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ supplier?.email || '-' }}</a-descriptions-item>
            <a-descriptions-item label="地址">{{ supplier?.province }}{{ supplier?.city }} {{ supplier?.address || '-' }}</a-descriptions-item>
          </a-descriptions>
          <a-descriptions :column="1" bordered size="small" title="财务信息" style="margin-top: 16px">
            <a-descriptions-item label="开户银行">{{ supplier?.bankName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="银行账号">{{ supplier?.bankAccount || '-' }}</a-descriptions-item>
            <a-descriptions-item label="税号">{{ supplier?.taxNumber || '-' }}</a-descriptions-item>
          </a-descriptions>
        </a-col>
      </a-row>
      <a-row :gutter="24" style="margin-top: 16px">
        <a-col :span="24">
          <a-descriptions :column="1" bordered size="small" title="评分信息">
            <a-descriptions-item label="综合评分">
              <a-rate :value="Math.round((supplier?.comprehensiveScore || 0) / 20)" disabled allow-half style="font-size: 16px" />
              <span style="margin-left: 8px; font-weight: 600">{{ formatScore(supplier?.comprehensiveScore) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="总积分">{{ supplier?.totalPoints || 0 }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ supplier?.createTime || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注">{{ supplier?.remark || '-' }}</a-descriptions-item>
          </a-descriptions>
        </a-col>
      </a-row>
    </template>

    <!-- 绩效记录 Tab -->
    <template #tab-performance>
      <VxeTableList
        :columns="performanceVxeColumns"
        :data-source="performances"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #comprehensiveScoreCell="{ record }">
          <a-rate :value="Math.round(record.comprehensiveScore / 20)" disabled allow-half style="font-size: 12px" />
        </template>
      </VxeTableList>
    </template>

    <!-- 询价报价 Tab -->
    <template #tab-inquiry>
      <VxeTableList
        :columns="inquiryVxeColumns"
        :data-source="inquiries"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #quotationStatusCell="{ record }">
          <a-tag :color="getQuotationStatusColor(record.quotationStatus)">
            {{ getQuotationStatusLabel(record.quotationStatus) }}
          </a-tag>
        </template>
        <template #quotationAmountCell="{ record }">
          ¥{{ record.quotationAmount?.toFixed(2) || '-' }}
        </template>
      </VxeTableList>
    </template>

    <!-- 积分记录 Tab -->
    <template #tab-points>
      <VxeTableList
        :columns="pointsVxeColumns"
        :data-source="pointsRecords"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #changeAmountCell="{ record }">
          <span :style="{ color: record.changeAmount > 0 ? '#07c160' : '#ff4d4f', fontWeight: 600 }">
            {{ record.changeAmount > 0 ? '+' : '' }}{{ record.changeAmount }}
          </span>
        </template>
        <template #balanceCell="{ record }">
          <span style="font-weight: 600">{{ record.balance }}</span>
        </template>
      </VxeTableList>
    </template>
  </DetailLayout>

  <a-modal
    v-model:open="addPointsVisible"
    title="增加积分"
    width="460px"
    @ok="handleAddPointsOk"
    @cancel="addPointsVisible = false"
    :confirm-loading="addPointsSubmitting"
  >
    <a-form :model="addPointsForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="当前积分">{{ supplier?.totalPoints ?? 0 }}</a-form-item>
      <a-form-item label="增加积分" required>
        <a-input-number v-model:value="addPointsForm.points" :min="1" style="width: 100%" placeholder="请输入增加积分数" />
      </a-form-item>
      <a-form-item label="原因" required>
        <a-input v-model:value="addPointsForm.reason" placeholder="请输入积分增加原因" />
      </a-form-item>
      <a-form-item label="日期">
        <a-date-picker v-model:value="addPointsForm.date" style="width: 100%" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="consumePointsVisible"
    title="消费积分"
    width="460px"
    @ok="handleConsumePointsOk"
    @cancel="consumePointsVisible = false"
    :confirm-loading="consumePointsSubmitting"
  >
    <a-form :model="consumePointsForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="当前积分">{{ supplier?.totalPoints ?? 0 }}</a-form-item>
      <a-form-item label="消费积分" required>
        <a-input-number v-model:value="consumePointsForm.points" :min="1" :max="supplier?.totalPoints ?? 0" style="width: 100%" placeholder="请输入消费积分数" />
      </a-form-item>
      <a-form-item label="用途" required>
        <a-input v-model:value="consumePointsForm.reason" placeholder="请输入积分消费用途" />
      </a-form-item>
      <a-form-item label="日期">
        <a-date-picker v-model:value="consumePointsForm.date" style="width: 100%" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import request from '@/utils/request'
import { supplierApi, type Supplier } from '@/api/supplier'

const router = useRouter()
const route = useRoute()

const supplier = ref<Supplier | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const currentIndex = ref(1)
const totalCount = ref(1)
const activeTab = ref('basic')

const addPointsVisible = ref(false)
const addPointsSubmitting = ref(false)
const addPointsForm = ref({ points: 0, reason: '', date: new Date().toISOString().slice(0, 10) })

const consumePointsVisible = ref(false)
const consumePointsSubmitting = ref(false)
const consumePointsForm = ref({ points: 0, reason: '', date: new Date().toISOString().slice(0, 10) })

const performances = ref<any[]>([])
const inquiries = ref<any[]>([])
const pointsRecords = ref<any[]>([])

const breadcrumbItems = computed(() => [
  { text: '供应商管理', path: '/supplier' },
  { text: supplier.value?.supplierName || '' }
])

const tabs = [
  { key: 'basic', label: '基本信息' },
  { key: 'performance', label: '绩效记录', count: performances.value.length },
  { key: 'inquiry', label: '询价报价', count: inquiries.value.length },
  { key: 'points', label: '积分记录', count: pointsRecords.value.length }
]

const activityLogs = computed(() => {
  const logs = []
  if (supplier.value) {
    logs.push({ id: 1, time: supplier.value.createTime || '', user: '系统', action: '创建供应商' })
    if (supplier.value.updateTime && supplier.value.updateTime !== supplier.value.createTime) {
      logs.push({ id: 2, time: supplier.value.updateTime, user: '管理员', action: '更新供应商信息' })
    }
  }
  performances.value.slice(0, 3).forEach((p, i) => {
    logs.push({ id: i + 10, time: p.evaluateTime || '', user: p.evaluator || '系统', action: `绩效评估: ${formatScore(p.comprehensiveScore)}分` })
  })
  return logs
})

const performanceVxeColumns = [
  { field: 'period', title: '评估周期', width: 120 },
  { field: 'qualityScore', title: '质量评分', width: 80 },
  { field: 'deliveryScore', title: '交付评分', width: 80 },
  { field: 'priceScore', title: '价格评分', width: 80 },
  { field: 'serviceScore', title: '服务评分', width: 80 },
  { field: 'comprehensiveScore', title: '综合评分', width: 120, slotName: 'comprehensiveScoreCell' },
  { field: 'evaluateTime', title: '评估时间', width: 140 },
  { field: 'evaluator', title: '评估人', width: 100 }
]

const inquiryVxeColumns = [
  { field: 'inquiryNo', title: '询价单号', width: 160 },
  { field: 'inquiryTitle', title: '询价标题' },
  { field: 'quotationAmount', title: '报价金额', width: 120, slotName: 'quotationAmountCell' },
  { field: 'quotationStatus', title: '报价状态', width: 100, slotName: 'quotationStatusCell' },
  { field: 'createTime', title: '创建时间', width: 150 }
]

const pointsVxeColumns = [
  { field: 'changeAmount', title: '变动金额', width: 100, slotName: 'changeAmountCell' },
  { field: 'balance', title: '余额', width: 80, slotName: 'balanceCell' },
  { field: 'description', title: '来源/用途' },
  { field: 'createTime', title: '时间', width: 150 }
]

// ── 数据加载 ──────────────────────────────────────────
const loadSupplierDetail = async () => {
  loading.value = true; error.value = null
  try {
    const id = Number(route.params.id)
    if (isNaN(id)) { error.value = '无效的供应商ID'; return }
    supplier.value = await supplierApi.getById(id)
  } catch (err: any) {
    console.warn('[供应商] 获取详情失败', err)
    error.value = err?.message || '获取供应商详情失败'
  } finally {
    loading.value = false
  }
}

const loadPerformances = async () => {
  try {
    const id = Number(route.params.id)
    if (isNaN(id)) return
    performances.value = (await supplierApi.getPerformanceHistory(id)) || []
  } catch (err) { console.warn('[供应商] 加载绩效记录失败', err); performances.value = [] }
}

const loadInquiries = async () => {
  try {
    const id = Number(route.params.id)
    if (isNaN(id)) return
    inquiries.value = (await supplierApi.getInquiries(id)) || []
  } catch (err) { console.warn('[供应商] 加载询价记录失败', err); inquiries.value = [] }
}

const loadPointsRecords = async () => {
  try {
    const id = Number(route.params.id)
    if (isNaN(id)) return
    pointsRecords.value = (await supplierApi.getPointsRecords(id)) || []
  } catch (err) { console.warn('[供应商] 加载积分记录失败', err); pointsRecords.value = [] }
}

// ── 操作 ──────────────────────────────────────────────
const handleBreadcrumbClick = (item: any) => { if (item.path) router.push(item.path) }

const handlePrev = async () => {
  const prevId = supplier.value ? supplier.value.id - 1 : 0
  if (prevId < 1) {
    message.warning('已是第一条')
    return
  }
  try {
    await supplierApi.getById(prevId)
    router.push(`/supplier/detail/${prevId}`)
  } catch (err) {
    console.warn('[供应商] 上一条记录不存在', err)
    message.warning('已是第一条')
  }
}

const handleNext = async () => {
  const nextId = supplier.value ? supplier.value.id + 1 : 1
  try {
    await supplierApi.getById(nextId)
    router.push(`/supplier/detail/${nextId}`)
  } catch (err) {
    console.warn('[供应商] 下一条记录不存在', err)
    message.warning('已是最后一条')
  }
}
const handleTabChange = (key: string) => { activeTab.value = key }
const handleEdit = () => router.push(`/supplier/edit/${route.params.id}`)
const handleEvaluate = () => router.push(`/supplier/performance/${route.params.id}/evaluate`)

const handleAddPoints = () => {
  addPointsForm.value = { points: 0, reason: '', date: new Date().toISOString().slice(0, 10) }
  addPointsVisible.value = true
}

const handleAddPointsOk = async () => {
  if (!addPointsForm.value.points || addPointsForm.value.points < 1) {
    message.warning('请输入有效的积分数')
    return
  }
  if (!addPointsForm.value.reason.trim()) {
    message.warning('请输入积分增加原因')
    return
  }
  addPointsSubmitting.value = true
  try {
    await request.post(`/supplier-portal/points/${supplier.value!.id}/add`, {
      points: addPointsForm.value.points,
      reason: addPointsForm.value.reason,
      date: addPointsForm.value.date
    })
    console.warn('[供应商] 操作成功: 增加积分')
    message.success(`成功增加 ${addPointsForm.value.points} 积分`)
    addPointsVisible.value = false
    loadSupplierDetail()
    loadPointsRecords()
  } catch (err: any) {
    console.warn('[供应商] 增加积分失败', err)
    message.error(err?.message || '增加积分失败')
  } finally {
    addPointsSubmitting.value = false
  }
}

const handleConsumePoints = () => {
  consumePointsForm.value = { points: 0, reason: '', date: new Date().toISOString().slice(0, 10) }
  consumePointsVisible.value = true
}

const handleConsumePointsOk = async () => {
  const available = supplier.value?.totalPoints ?? 0
  if (!consumePointsForm.value.points || consumePointsForm.value.points < 1) {
    message.warning('请输入有效的积分数')
    return
  }
  if (consumePointsForm.value.points > available) {
    message.warning(`消费积分不能超过当前积分(${available})`)
    return
  }
  if (!consumePointsForm.value.reason.trim()) {
    message.warning('请输入积分消费用途')
    return
  }
  consumePointsSubmitting.value = true
  try {
    await request.post(`/supplier-portal/points/${supplier.value!.id}/consume`, {
      points: consumePointsForm.value.points,
      reason: consumePointsForm.value.reason,
      date: consumePointsForm.value.date
    })
    console.warn('[供应商] 操作成功: 消费积分')
    message.success(`成功消费 ${consumePointsForm.value.points} 积分`)
    consumePointsVisible.value = false
    loadSupplierDetail()
    loadPointsRecords()
  } catch (err: any) {
    console.warn('[供应商] 积分消费失败', err)
    message.error(err?.message || '积分消费失败')
  } finally {
    consumePointsSubmitting.value = false
  }
}

// ── 辅助 ──────────────────────────────────────────────
const formatScore = (score?: number) => score ? score.toFixed(1) : '0.0'
const getLevelColor = (level?: string) => {
  const colors: Record<string, string> = { A: '#07c160', B: '#1890ff', C: '#faad14', D: '#ff4d4f', E: '#999' }
  return level ? (colors[level] || '#999') : '#999'
}
const getStatusColor = (status?: number) => {
  const colors: Record<number, string> = { 1: 'green', 2: 'orange', 3: 'red', 4: 'default' }
  return status ? (colors[status] || 'default') : 'default'
}
const getStatusLabel = (status?: number) => {
  const labels: Record<number, string> = { 1: '正常合作', 2: '暂停合作', 3: '终止合作', 4: '潜在供应商' }
  return status ? (labels[status] || '未知') : '未知'
}
const getStatusType = (status?: number): 'success' | 'warning' | 'danger' | 'info' | 'default' => {
  const types: Record<number, any> = { 1: 'success', 2: 'warning', 3: 'danger', 4: 'info' }
  return status ? (types[status] || 'default') : 'default'
}
const getQuotationStatusColor = (status?: number) => {
  const colors: Record<number, string> = { 0: 'default', 1: 'blue', 2: 'green', 3: 'red' }
  return status !== undefined ? (colors[status] || 'default') : 'default'
}
const getQuotationStatusLabel = (status?: number) => {
  const labels: Record<number, string> = { 0: '待报价', 1: '已报价', 2: '已接受', 3: '已拒绝' }
  return status !== undefined ? (labels[status] || '未知') : '未知'
}

onMounted(() => {
  loadSupplierDetail()
  loadPerformances()
  loadInquiries()
  loadPointsRecords()
})

defineExpose({ handleQuery: loadSupplierDetail })
</script>
