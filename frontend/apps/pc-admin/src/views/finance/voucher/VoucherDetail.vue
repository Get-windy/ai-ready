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
      <a-button
        v-if="voucher?.status === 0"
        type="primary"
        :loading="auditing"
        @click="handleAudit"
      >
        审核
      </a-button>
      <a-button
        v-if="voucher?.status === 1"
        type="primary"
        :loading="posting"
        @click="handlePost"
      >
        过账
      </a-button>
      <a-button
        v-if="voucher?.status === 2"
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
            <a-menu-item @click="handlePrint">打印凭证</a-menu-item>
            <a-menu-item :disabled="voucher?.status !== 0" @click="handleEdit">
              编辑
              <span v-if="voucher?.status !== 0" style="color: #999; font-size: 12px; margin-left: 4px">(仅草稿可编辑)</span>
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
        :pagination="false"
        row-key="id"
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
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { voucherApi } from '@/api/finance'

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

const statusColorMap: Record<number, string> = {
  0: 'default',
  1: 'processing',
  2: 'success',
  3: 'error'
}

const statusLabelMap: Record<number, string> = {
  0: '草稿',
  1: '已审核',
  2: '已过账',
  3: '已冲销'
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

const getStatusType = (status?: number): 'success' | 'warning' | 'danger' | 'info' | 'default' => {
  const types: Record<number, any> = { 0: 'default', 1: 'warning', 2: 'success', 3: 'danger' }
  return status !== undefined ? (types[status] || 'default') : 'default'
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

const handlePrint = () => {
  message.info('打印功能开发中')
}

const handleEdit = () => {
  message.info('编辑功能开发中')
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
      } catch {
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
      } catch {
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
  } catch {
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
})
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
</style>
