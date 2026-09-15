<template>
  <a-drawer
    :open="open"
    title="巡检记录详情"
    :width="620"
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
        <a-descriptions-item label="车牌号">
          {{ data.plateNo }}
        </a-descriptions-item>
        <a-descriptions-item label="配送员">
          {{ data.riderName }}
        </a-descriptions-item>
        <a-descriptions-item label="巡检类型">
          {{ INSPECTION_TYPE_MAP[data.inspectionType]?.text }}
        </a-descriptions-item>
        <a-descriptions-item label="结果">
          <a-tag :color="data.result === 1 ? 'green' : 'red'">
            {{ data.result === 1 ? '通过' : '不通过' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="巡检时间">
          {{ formatDateTime(data.inspectionTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="里程(km)">
          {{ data.mileage ?? '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="油量(%)">
          {{ data.fuelLevel ?? '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="检查地点">
          {{ data.inspectionLocation || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          v-for="item in INSPECTION_ITEM_LABELS"
          :key="item.key"
          :label="item.label"
        >
          <a-tag :color="data[item.key] === 0 ? 'green' : 'red'">
            {{ data[item.key] === 0 ? '正常' : '异常' }}
          </a-tag>
          <span v-if="data[item.key] === 1 && data[item.remarkKey]">
            {{ data[item.remarkKey] }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item
          label="外观照片"
          :span="2"
        >
          <template v-if="data.exteriorPhotos">
            <a-image
              v-for="(url, i) in String(data.exteriorPhotos).split(',')"
              :key="i"
              :src="url"
              :width="72"
              style="margin-right: 6px"
            />
          </template>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item
          label="审核人"
          :span="1"
        >
          {{ data.reviewer || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          label="审核意见"
          :span="1"
        >
          {{ data.reviewRemark || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          label="备注"
          :span="2"
        >
          {{ data.remark || '-' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { INSPECTION_TYPE_MAP, INSPECTION_ITEM_LABELS } from '../../config/verification-config'

defineProps<{
  open: boolean
  loading?: boolean
  data?: any
}>()

const emit = defineEmits<{ 'update:open': [value: boolean] }>()

function formatDateTime(value?: string): string {
  if (!value) return '-'
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : String(value)
}
</script>
