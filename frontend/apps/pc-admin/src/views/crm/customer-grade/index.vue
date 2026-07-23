<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="客户分级"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      :stat-cards="levelCards"
      page-param-style="pageNum"
      export-file-name="客户分级"
      row-key="id"
    >
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'customerLevel'">
          <a-tag :color="levelColor(text)">
            {{ levelText(text, record.customerLevelDesc) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="text === 1 ? 'green' : 'default'">
            {{ text === 1 ? '正常' : '停用' }}
          </a-tag>
        </template>
        <template v-else-if="['creditLimit', 'currentDebt', 'tradeAmount'].includes(column.dataIndex as string)">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button
            type="link"
            size="small"
            @click="openLevelModal(record)"
          >
            调整等级
          </a-button>
        </template>
      </template>
    </ARReportPage>

    <!-- ═══ 调整等级弹窗 ═══ -->
    <a-modal
      v-model:open="levelModalVisible"
      title="调整客户等级"
      :confirm-loading="levelSaving"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleLevelSave"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="客户">
          <span>{{ currentCustomer?.customerName }}</span>
        </a-form-item>
        <a-form-item label="当前等级">
          <a-tag :color="levelColor(currentCustomer?.customerLevel)">
            {{ levelText(currentCustomer?.customerLevel, currentCustomer?.customerLevelDesc) }}
          </a-tag>
        </a-form-item>
        <a-form-item
          label="新等级"
          required
        >
          <a-select
            v-model:value="newLevel"
            placeholder="请选择新等级"
            :options="LEVEL_OPTIONS"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, StatCardItem } from '@/components/ARReportPage/types'
import { crmCustomerApi, type CrmCustomer } from '@/api/crm'

// ═══ 客户等级（与现有客户页保持一致：1VIP 2重要 3普通 4潜在） ═══
const LEVEL_OPTIONS = [
  { label: 'VIP客户', value: 1 },
  { label: '重要客户', value: 2 },
  { label: '普通客户', value: 3 },
  { label: '潜在客户', value: 4 }
]
const LEVEL_TEXT: Record<number, string> = { 1: 'VIP客户', 2: '重要客户', 3: '普通客户', 4: '潜在客户' }
const LEVEL_COLOR: Record<number, string> = { 1: 'gold', 2: 'orange', 3: 'blue', 4: 'default' }

function levelText(v: number | undefined, desc?: string): string {
  return desc || (v && LEVEL_TEXT[v]) || '未分级'
}
function levelColor(v: number | undefined): string {
  return (v && LEVEL_COLOR[v]) || 'default'
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '客户名称/编码', width: 200 },
  {
    key: 'customerLevel',
    type: 'select',
    label: '等级',
    placeholder: '全部等级',
    options: LEVEL_OPTIONS
  }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '客户编码', dataIndex: 'customerCode', key: 'customerCode', width: 130 },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 180, ellipsis: true },
  { title: '等级', dataIndex: 'customerLevel', key: 'customerLevel', width: 100 },
  { title: '信用额度', dataIndex: 'creditLimit', key: 'creditLimit', width: 110, align: 'right' },
  { title: '当前欠款', dataIndex: 'currentDebt', key: 'currentDebt', width: 110, align: 'right' },
  { title: '交易次数', dataIndex: 'tradeCount', key: 'tradeCount', width: 90, align: 'right' },
  { title: '交易金额', dataIndex: 'tradeAmount', key: 'tradeAmount', width: 120, align: 'right' },
  { title: '最近交易', dataIndex: 'lastTradeDate', key: 'lastTradeDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '负责人', dataIndex: 'salesPersonName', key: 'salesPersonName', width: 100 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
const reportRef = ref<InstanceType<typeof ARReportPage>>()

function fetcher(params: Record<string, any>) {
  return crmCustomerApi.page(params)
}

// ═══ 等级分布卡片（后端无客户统计端点，由 /customer/export 全量实时聚合） ═══
const allCustomers = ref<CrmCustomer[]>([])

const levelCards = computed<StatCardItem[]>(() => {
  const list = allCustomers.value
  const cards: StatCardItem[] = [{ label: '客户总数', value: list.length, suffix: '家' }]
  for (const opt of LEVEL_OPTIONS) {
    cards.push({
      label: opt.label,
      value: list.filter(c => c.customerLevel === opt.value).length,
      suffix: '家'
    })
  }
  return cards
})

async function loadLevelStats() {
  try {
    const list = await crmCustomerApi.exportList()
    allCustomers.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.warn('[客户分级] 等级分布统计获取失败', e)
  }
}

// ═══ 调整等级 ═══
const levelModalVisible = ref(false)
const levelSaving = ref(false)
const currentCustomer = ref<CrmCustomer | null>(null)
const newLevel = ref<number>()

function openLevelModal(record: any) {
  currentCustomer.value = record
  newLevel.value = record.customerLevel
  levelModalVisible.value = true
}

async function handleLevelSave() {
  if (!currentCustomer.value) return
  if (!newLevel.value) {
    message.warning('请选择新等级')
    return
  }
  levelSaving.value = true
  try {
    await crmCustomerApi.update(currentCustomer.value.id, {
      ...currentCustomer.value,
      customerLevel: newLevel.value,
      customerLevelDesc: LEVEL_TEXT[newLevel.value]
    })
    message.success('客户等级已更新')
    levelModalVisible.value = false
    reportRef.value?.reload()
    loadLevelStats()
  } catch (e: any) {
    message.error(e?.message || '更新失败')
  } finally {
    levelSaving.value = false
  }
}

onMounted(() => {
  loadLevelStats()
})
</script>
