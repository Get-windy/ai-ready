<template>
  <a-drawer
    :open="open"
    title="实名认证详情"
    :width="820"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <a-spin :spinning="loading">
      <a-descriptions
        v-if="data"
        :column="2"
        bordered
        size="small"
      >
        <a-descriptions-item label="配送员">
          {{ data.riderName }}
        </a-descriptions-item>
        <a-descriptions-item label="手机号">
          {{ data.riderPhone || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="身份类型">
          {{ data.riderTypeText || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="所属渠道">
          {{ data.channelName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="证件姓名">
          {{ data.realName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="身份证号">
          {{ data.idCardNo || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="认证状态">
          <a-tag :color="VERIFY_STATUS_MAP[data.verifyStatus]?.color">
            {{ data.verifyStatusText || VERIFY_STATUS_MAP[data.verifyStatus]?.text }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="接单资质">
          <a-tag :color="data.eligible ? 'green' : 'red'">
            {{ data.eligible ? '具备' : '不具备' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="背书/审查机构">
          {{ data.endorseOrg || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="背书结论">
          {{ data.endorseResult === 1 ? '通过' : (data.endorseResult === 0 ? '未通过' : '-') }}
        </a-descriptions-item>
        <a-descriptions-item label="有效期至">
          {{ data.endorseExpireDate || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="到期预警">
          {{ data.expiringCertCount || 0 }} 项
        </a-descriptions-item>
        <a-descriptions-item label="审核时间">
          {{ formatDateTime(data.auditTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="生效时间">
          {{ formatDateTime(data.effectiveTime) }}
        </a-descriptions-item>
        <a-descriptions-item
          label="审核意见"
          :span="2"
        >
          {{ data.auditRemark || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          v-if="!data.eligible && data.ineligibleReason"
          label="不具备原因"
          :span="2"
        >
          {{ data.ineligibleReason }}
        </a-descriptions-item>
        <a-descriptions-item
          label="备注"
          :span="2"
        >
          {{ data.remark || '-' }}
        </a-descriptions-item>
      </a-descriptions>

      <h4 class="drawer-sub-title">
        证照明细（{{ data?.certificates?.length || 0 }}）
      </h4>
      <a-table
        :data-source="data?.certificates || []"
        :columns="certReadonlyColumns"
        :pagination="false"
        row-key="id"
        size="small"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'certType'">
            {{ CERT_TYPE_OPTIONS.find(o => o.value === record.certType)?.label || record.certType }}
          </template>
          <template v-else-if="column.dataIndex === 'verifyStatus'">
            <a-tag :color="record.verifyStatus === 1 ? 'green' : (record.verifyStatus === 2 ? 'red' : 'default')">
              {{ CERT_STATUS_TEXT[record.verifyStatus] || '-' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { VERIFY_STATUS_MAP, CERT_TYPE_OPTIONS } from '../../config/verification-config'

defineProps<{
  open: boolean
  loading?: boolean
  data?: any
}>()

const emit = defineEmits<{ 'update:open': [value: boolean] }>()

const CERT_STATUS_TEXT: Record<number, string> = { 0: '待核验', 1: '有效', 2: '已过期', 3: '无效' }
const certReadonlyColumns = [
  { title: '证照类型', dataIndex: 'certType', width: 120 },
  { title: '证照编号', dataIndex: 'certNo', width: 160 },
  { title: '发证日期', dataIndex: 'issueDate', width: 120 },
  { title: '有效期至', dataIndex: 'expireDate', width: 120 },
  { title: '状态', dataIndex: 'verifyStatus', width: 90 },
  { title: '备注', dataIndex: 'remark' },
]

function formatDateTime(value?: string): string {
  if (!value) return '-'
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : String(value)
}
</script>

<style scoped>
.drawer-sub-title {
  margin: 18px 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
</style>
