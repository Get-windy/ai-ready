<template>
  <div class="voucher-detail-page">
    <a-card :bordered="false">
      <template #title>
        <a-space>
          <FileTextOutlined /> 凭证详情
        </a-space>
      </template>
      <template #extra>
        <a-space>
          <a-button @click="handleBack">
            <template #icon><ArrowLeftOutlined /></template>
            返回
          </a-button>
          <a-button
            v-if="voucher.status === 0"
            type="primary"
            @click="handleAudit"
          >
            审核
          </a-button>
          <a-button
            v-if="voucher.status === 1"
            type="primary"
            @click="handlePost"
          >
            过账
          </a-button>
          <a-button
            v-if="voucher.status === 2"
            type="primary"
            danger
            @click="handleReverse"
          >
            冲销
          </a-button>
        </a-space>
      </template>

      <!-- 凭证头信息 -->
      <a-descriptions :column="4" bordered size="small">
        <a-descriptions-item label="凭证号">
          <a-tag color="blue">{{ voucher.voucherNo }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="日期">{{ voucher.voucherDate }}</a-descriptions-item>
        <a-descriptions-item label="年度">{{ voucher.fiscalYear }}</a-descriptions-item>
        <a-descriptions-item label="期间">{{ voucher.fiscalPeriod }}月</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColorMap[voucher.status] || 'default'">
            {{ statusLabelMap[voucher.status] || '未知' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="制单人">{{ voucher.createdBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审核人">{{ voucher.auditedBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="过账人">{{ voucher.postedBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="摘要" :span="4">
          {{ voucher.summary || (voucher.entries?.[0]?.summary || '-') }}
        </a-descriptions-item>
      </a-descriptions>

      <!-- 凭证分录 -->
      <a-divider orientation="left">分录明细</a-divider>

      <a-table
        :columns="columns"
        :data-source="voucher.entries || []"
        :pagination="false"
        size="small"
        bordered
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'debitAmount'">
            {{ formatAmount(record.debitAmount) }}
          </template>
          <template v-else-if="column.key === 'creditAmount'">
            {{ formatAmount(record.creditAmount) }}
          </template>
        </template>
        <template #summary>
          <a-table-summary-row>
            <a-table-summary-cell :index="0" :col-span="2">合计</a-table-summary-cell>
            <a-table-summary-cell :index="2">
              <strong>{{ formatAmount(voucher.debitTotal) }}</strong>
            </a-table-summary-cell>
            <a-table-summary-cell :index="3">
              <strong>{{ formatAmount(voucher.creditTotal) }}</strong>
            </a-table-summary-cell>
          </a-table-summary-row>
        </template>
      </a-table>

      <!-- 借贷平衡提示 -->
      <a-alert
        :type="isBalanced ? 'success' : 'error'"
        :message="isBalanced ? '借贷平衡' : '借贷不平衡'"
        show-icon
        style="margin-top: 16px"
      />
    </a-card>

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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import { FileTextOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue'
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

const isBalanced = computed(() => {
  return Math.abs((voucher.value.debitTotal || 0) - (voucher.value.creditTotal || 0)) < 0.01
})

const columns: TableProps['columns'] = [
  { title: '摘要', dataIndex: 'summary', key: 'summary' },
  { title: '会计科目', dataIndex: 'subjectName', key: 'subjectName' },
  { title: '借方金额', key: 'debitAmount', align: 'right', width: 150 },
  { title: '贷方金额', key: 'creditAmount', align: 'right', width: 150 }
]

const fetchVoucher = async (id: number) => {
  try {
    const res = await voucherApi.getById(id)
    if (res.data) {
      voucher.value = res.data
    }
  } catch {
    message.error('获取凭证详情失败')
    handleBack()
  }
}

const handleBack = () => {
  router.push('/erp/finance/voucher')
}

const handleAudit = () => {
  Modal.confirm({
    title: '确认审核',
    content: '确定要审核该凭证吗？',
    async onOk() {
      try {
        await voucherApi.audit(voucher.value.id)
        message.success('审核成功')
        voucher.value.status = 1
      } catch {
        message.error('审核失败')
      }
    }
  })
}

const handlePost = () => {
  Modal.confirm({
    title: '确认过账',
    content: '确定要将该凭证过账吗？',
    async onOk() {
      try {
        await voucherApi.post(voucher.value.id)
        message.success('过账成功')
        voucher.value.status = 2
      } catch {
        message.error('过账失败')
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
    voucher.value.status = 3
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
  const id = route.params.id as string
  if (id) {
    fetchVoucher(parseInt(id))
  }
})
</script>

<style scoped>
.voucher-detail-page {
  padding: 16px;
}
</style>
