<template>
  <a-drawer
    :open="open"
    title="人车绑定详情"
    :width="1000"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <a-spin :spinning="loading">
      <a-descriptions
        v-if="data"
        title="绑定信息"
        :column="3"
        bordered
        size="small"
      >
        <a-descriptions-item label="配送员">
          {{ data.binding.riderName }}
        </a-descriptions-item>
        <a-descriptions-item label="手机号">
          {{ data.binding.riderPhone || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="车牌号">
          {{ data.binding.plateNo }}
        </a-descriptions-item>
        <a-descriptions-item label="绑定时间">
          {{ formatDateTime(data.binding.bindTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="绑定里程">
          {{ data.binding.bindMileage ?? '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="BINDING_STATUS_MAP[data.binding.status]?.color">
            {{ BINDING_STATUS_MAP[data.binding.status]?.text }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="交车时间">
          {{ formatDateTime(data.binding.handoverTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="交车里程">
          {{ data.binding.handoverMileage ?? '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="交车地点">
          {{ data.binding.handoverLocation || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="绑定原因">
          {{ data.binding.bindReason || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="备注">
          {{ data.binding.remark || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="核验次数/异常">
          {{ data.verifyCount }} / {{ data.abnormalCount }}
        </a-descriptions-item>
      </a-descriptions>

      <h4 class="drawer-sub-title">
        位置核验历史（{{ data?.verifications?.length || 0 }}）
      </h4>
      <a-table
        :data-source="data?.verifications || []"
        :columns="verifyColumns"
        :pagination="false"
        row-key="id"
        size="small"
        bordered
        :scroll="{ y: 240 }"
      />

      <h4 class="drawer-sub-title">
        关联巡检（{{ data?.inspections?.length || 0 }}）
      </h4>
      <a-table
        :data-source="data?.inspections || []"
        :columns="detailInspectionColumns"
        :pagination="false"
        row-key="id"
        size="small"
        bordered
      />

      <h4 class="drawer-sub-title">
        关联预警（{{ data?.alerts?.length || 0 }}）
      </h4>
      <a-table
        :data-source="data?.alerts || []"
        :columns="detailAlertColumns"
        :pagination="false"
        row-key="id"
        size="small"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'alertType'">
            <a-tag :color="ALERT_TYPE_MAP[record.alertType]?.color">
              {{ ALERT_TYPE_MAP[record.alertType]?.text }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'handleStatus'">
            <a-tag :color="HANDLE_STATUS_MAP[record.handleStatus]?.color">
              {{ HANDLE_STATUS_MAP[record.handleStatus]?.text }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import {
  BINDING_STATUS_MAP, ALERT_TYPE_MAP, HANDLE_STATUS_MAP, INSPECTION_TYPE_MAP,
} from '../../config/verification-config'

defineProps<{
  open: boolean
  loading?: boolean
  data?: any
}>()

const emit = defineEmits<{ 'update:open': [value: boolean] }>()

const verifyColumns = [
  { title: '核验时间', dataIndex: 'verifyTime', width: 160, customRender: ({ text }: any) => formatDateTime(text) },
  { title: '人车距离(米)', dataIndex: 'distanceMeters', width: 120 },
  { title: '阈值(米)', dataIndex: 'thresholdMeters', width: 100 },
  {
    title: '结论',
    dataIndex: 'isAbnormal',
    width: 90,
    customRender: ({ text }: any) => (text === 1 ? '异常' : '正常'),
  },
  { title: '核验说明', dataIndex: 'verifyDesc' },
]
const detailInspectionColumns = [
  { title: '巡检类型', dataIndex: 'inspectionType', width: 120, customRender: ({ text }: any) => INSPECTION_TYPE_MAP[text]?.text || text },
  { title: '结果', dataIndex: 'result', width: 90, customRender: ({ text }: any) => (text === 1 ? '通过' : '不通过') },
  { title: '巡检时间', dataIndex: 'inspectionTime', width: 160, customRender: ({ text }: any) => formatDateTime(text) },
  { title: '审核人', dataIndex: 'reviewer', width: 100 },
  { title: '备注', dataIndex: 'remark' },
]
const detailAlertColumns = [
  { title: '类型', dataIndex: 'alertType', width: 130 },
  { title: '内容', dataIndex: 'alertContent' },
  { title: '处理状态', dataIndex: 'handleStatus', width: 100 },
  { title: '发生时间', dataIndex: 'createTime', width: 160, customRender: ({ text }: any) => formatDateTime(text) },
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
