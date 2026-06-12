<template>
  <DetailLayout
    :breadcrumb-items="breadcrumbItems"
    :title="voucher?.voucherNo || ''"
    :status="statusLabelMap[voucher?.status] || '未知'"
    :status-type="getStatusType(voucher?.status)"
    :tabs="tabs"
    :active-tab="activeTab"
    :loading="loading"
    :error="error"
    @breadcrumb-click="handleBreadcrumbClick"
    @tab-change="handleTabChange"
    @retry="fetchVoucher"
  >
    <template #header-extra>
      <a-tag v-if="voucher" :color="isBalanced ? 'green' : 'red'">
        {{ isBalanced ? '借贷平衡' : '借贷不平衡' }}
      </a-tag>
    </template>

    <template #actions>
      <PrintButton :business-id="voucher?.id" business-type="voucher" button-size="small" tooltip="打印凭证" />
      <a-button
        v-if="voucher?.status === 'draft'"
        type="primary"
        :loading="auditing"
        @click="handleAudit"
      >
        审核
      </a-button>
      <a-button
        v-if="voucher?.status === 'audited'"
        type="primary"
        :loading="posting"
        @click="handlePost"
      >
        过账
      </a-button>
      <a-button
        v-if="voucher?.status === 'posted'"
        danger
        @click="handleReverse"
      >
        冲销
      </a-button>
    </template>

    <template #more-actions>
      <a-dropdown>
        <a-button>更多操作</a-button>
        <template #overlay>
          <a-menu>
            <a-menu-item :disabled="voucher?.status !== 'draft'" @click="handleEdit">
              编辑
              <span v-if="voucher?.status !== 'draft'" style="color: #999; font-size: 12px; margin-left: 4px">(仅草稿可编辑)</span>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </template>

    <!-- 基本信息 Tab -->
    <template #tab-basic>
      <a-descriptions :column="4" bordered size="small">
        <a-descriptions-item label="凭证号">
          <a-tag color="blue">{{ voucher?.voucherNo }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="日期">{{ voucher?.voucherDate }}</a-descriptions-item>
        <a-descriptions-item label="年度">{{ voucher?.fiscalYear }}</a-descriptions-item>
        <a-descriptions-item label="期间">{{ voucher?.fiscalPeriod }}月</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColorMap[voucher?.status] || 'default'">
            {{ statusLabelMap[voucher?.status] || '未知' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="制单人">{{ voucher?.createdBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审核人">{{ voucher?.auditedBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="过账人">{{ voucher?.postedBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="摘要" :span="4">
          {{ voucher?.summary || (voucher?.entries?.[0]?.summary || '-') }}
        </a-descriptions-item>
      </a-descriptions>

      <a-alert
        :type="isBalanced ? 'success' : 'error'"
        :message="isBalanced ? '借贷平衡' : '借贷不平衡'"
        show-icon
        style="margin-top: 16px"
      />
    </template>

    <!-- 分录明细 Tab -->
    <template #tab-entries>
      <VxeTableList
        :columns="entryVxeColumns"
        :data-source="voucher?.entries || []"
        :pagination="false as any"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #debitAmountCell="{ record }">
          {{ formatAmount(record.debitAmount) }}
        </template>
        <template #creditAmountCell="{ record }">
          {{ formatAmount(record.creditAmount) }}
        </template>
      </VxeTableList>
      <div v-if="voucher" class="voucher-summary">
        <span class="voucher-summary-label">合计：</span>
        <span>借方：<strong>{{ formatAmount(voucher?.debitTotal) }}</strong></span>
        <span>贷方：<strong>{{ formatAmount(voucher?.creditTotal) }}</strong></span>
      </div>
    </template>
  </DetailLayout>

  <!-- 冲销原因弹窗 -->
  <a-modal
    v-model:open="reverseModalVisible"
    title="冲销凭证"
    :confirm-loading="reverseLoading"
    @ok="handleReverseConfirm"
    @cancel="reverseModalVisible = false"
  >
    <a-form layout="vertical">
      <a-form-item label="冲销原因" required>
        <a-textarea
          v-model:value="reverseReason"
          placeholder="请输入冲销原因"
          :rows="3"
          size="small"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { voucherApi } from '@/api/finance'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const route = useRoute()
const router = useRouter()

const voucher = ref<any>({
  voucherNo: '',
  voucherDate: '',
  fiscalYear: 0,
  fiscalPeriod: 0,
  status: 0,
  createdBy: '',
  auditedBy: '',
  postedBy: '',
  summary: '',
  debitTotal: 0,
  creditTotal: 0,
  entries: []
})

const loading = ref(false)
const error = ref<string | null>(null)
const activeTab = ref('basic')
const auditing = ref(false)
const posting = ref(false)
const reverseModalVisible = ref(false)
const reverseLoading = ref(false)
const reverseReason = ref('')

const statusColorMap: Record<string, string> = {
  draft: 'default',
  audited: 'processing',
  posted: 'success',
  reversed: 'error'
}

const statusLabelMap: Record<string, string> = {
  draft: '\u8349\u7a3f',
  audited: '\u5df2\u5ba1\u6838',
  posted: '\u5df2\u8fc7\u8d26',
  reversed: '\u5df2\u51b2\u9500'
}

const breadcrumbItems = computed(() => [
  { text: '财务管理', path: '/erp/finance' },
  { text: '凭证管理', path: '/erp/finance/voucher' },
  { text: voucher.value?.voucherNo || '' }
])

const tabs = [
  { key: 'basic', label: '基本信息' },
  { key: 'entries', label: '分录明细', count: voucher.value?.entries?.length || 0 }
]

const isBalanced = computed(() => {
  return Math.abs((voucher.value?.debitTotal || 0) - (voucher.value?.creditTotal || 0)) < 0.01
})

const getStatusType = (status?: string): any => {
  const types: Record<string, string> = { draft: 'default', audited: 'warning', posted: 'success', reversed: 'danger' }
  return status ? (types[status] || 'default') : 'default'
}

const entryVxeColumns = [
  { field: 'summary', title: '摘要' },
  { field: 'subjectName', title: '会计科目' },
  { field: 'debitAmount', title: '借方金额', align: 'right', width: 150, slotName: 'debitAmountCell' },
  { field: 'creditAmount', title: '贷方金额', align: 'right', width: 150, slotName: 'creditAmountCell' }
]

const fetchVoucher = async () => {
  const id = Number(route.params.id)
  if (isNaN(id)) {
    error.value = '无效的凭证ID'
    loading.value = false
    return
  }
  loading.value = true
  error.value = null
  try {
    const res = await voucherApi.getById(id)
    if (res.data) {
      voucher.value = res.data
    }
  } catch (err: any) {
    error.value = err?.message || '获取凭证详情失败'
  } finally {
    loading.value = false
  }
}

const handleBreadcrumbClick = (item: any) => {
  if (item.path) router.push(item.path)
}

const handleTabChange = (key: string) => {
  activeTab.value = key
}



const handleEdit = async () => {
  if (!voucher.value?.id) return
  try {
    // 导航回凭证列表页并传递编辑数据
    await router.push({
      path: '/erp/finance/voucher',
      query: { editVoucherId: String(voucher.value.id) }
    })
    // 导航完成后通过自定义事件传递完整凭证数据
    window.dispatchEvent(new CustomEvent('finance:edit-voucher', { detail: voucher.value }))
  } catch (err) {
    console.warn('[凭证详情] 编辑跳转失败', err)
    message.error('跳转编辑页面失败')
  }
}

const handleAudit = () => {
  Modal.confirm({
    title: '确认审核',
    content: '确定要审核该凭证吗？',
    async onOk() {
      auditing.value = true
      try {
        await voucherApi.audit(voucher.value.id)
        message.success('审核成功')
        fetchVoucher()
      } catch (err) {
        console.warn('[凭证详情] 审核失败', err)
        message.error('审核失败')
      } finally {
        auditing.value = false
      }
    }
  })
}

const handlePost = () => {
  Modal.confirm({
    title: '确认过账',
    content: '确定要将该凭证过账吗？',
    async onOk() {
      posting.value = true
      try {
        await voucherApi.post(voucher.value.id)
        message.success('过账成功')
        fetchVoucher()
      } catch (err) {
        console.warn('[凭证详情] 过账失败', err)
        message.error('过账失败')
      } finally {
        posting.value = false
      }
    }
  })
}

const handleReverse = () => {
  reverseReason.value = ''
  reverseModalVisible.value = true
}

const handleReverseConfirm = async () => {
  if (!reverseReason.value.trim()) {
    message.warning('请输入冲销原因')
    return
  }
  reverseLoading.value = true
  try {
    await voucherApi.reverse(voucher.value.id, reverseReason.value)
    message.success('冲销成功')
    reverseModalVisible.value = false
    fetchVoucher()
  } catch (err) {
    console.warn('[凭证详情] 冲销失败', err)
    message.error('冲销失败')
  } finally {
    reverseLoading.value = false
  }
}

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  fetchVoucher()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchVoucher)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchVoucher)
})

function handleParentCreate() { handleAdd() }
function handleAdd() {
  message.info('创建功能由父组件触发')
}

defineExpose({ handleQuery: fetchVoucher })

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchVoucher); return }
  if (e.ctrlKey && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}
</script>

<style scoped>
.voucher-summary {
  margin-top: 12px;
  padding: 8px 16px;
  background: #fafafa;
  border-radius: 4px;
  text-align: right;
}
.voucher-summary span {
  margin-right: 24px;
}
.voucher-summary-label {
  font-weight: 500;
}

/* Compact mode overrides */
:deep(.ant-table-thead > tr > th) {
  padding: 6px 8px !important;
  font-size: 12px;
}
:deep(.ant-table-tbody > tr > td) {
  padding: 4px 8px !important;
  font-size: 12px;
}
:deep(.ant-card-body) {
  padding: 12px;
}
:deep(.ant-form-item) {
  margin-bottom: 8px;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
