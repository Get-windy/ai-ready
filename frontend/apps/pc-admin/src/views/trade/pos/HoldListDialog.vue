<template>
  <a-modal
    :open="open"
    title="挂单列表（取单）"
    :width="720"
    :footer="null"
    @cancel="emit('update:open', false)"
  >
    <a-table
      :data-source="list"
      :columns="columns"
      :loading="loading"
      row-key="id"
      size="small"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'amount'">
          ¥{{ fmtMoney(record.payableAmount ?? record.amount) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button
            type="link"
            size="small"
            :loading="resumingId === record.id"
            @click="handleResume(record as RetailOrder)"
          >
            取单
          </a-button>
        </template>
      </template>
      <template #emptyText>
        <a-empty description="暂无挂单" />
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { retailApi, type RetailOrder, type RetailOrderDetailVO } from '@/api/retail'
import { fmtMoney } from './pos-shared'

const props = defineProps<{
  open: boolean
  warehouseId?: number
}>()

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'resumed', detail: RetailOrderDetailVO): void
}>()

const columns = [
  { title: '零售单号', dataIndex: 'retailNo', key: 'retailNo' },
  { title: '挂单时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 80 },
  { title: '金额', key: 'amount', width: 110 },
  { title: '收银员', dataIndex: 'cashierName', key: 'cashierName', width: 100 },
  { title: '操作', key: 'action', width: 80 },
]

const list = ref<RetailOrder[]>([])
const loading = ref(false)
const resumingId = ref<number | null>(null)

watch(
  () => props.open,
  async (v) => {
    if (!v) return
    loading.value = true
    try {
      const res = await retailApi.holdList(props.warehouseId)
      list.value = Array.isArray(res) ? res : []
    } catch (e: any) {
      list.value = []
      message.error(e?.message || '挂单列表加载失败')
    } finally {
      loading.value = false
    }
  }
)

async function handleResume(record: RetailOrder) {
  resumingId.value = record.id
  try {
    await retailApi.unhold(record.id)
    const detail = await retailApi.getDetail(record.id)
    emit('resumed', detail)
    emit('update:open', false)
    message.success(`已取单 ${record.retailNo}`)
  } catch (e: any) {
    message.error(e?.message || '取单失败')
  } finally {
    resumingId.value = null
  }
}
</script>
