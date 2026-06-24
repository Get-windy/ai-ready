<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">库存同步</h2>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'channelCode'">
              <a-tag :color="getChannelColor(record.channelCode)">{{ getChannelName(record.channelCode) }}</a-tag>
            </template>
            <template v-if="column.key === 'syncType'">
              <a-tag :color="record.syncType === 'PUSH' ? 'blue' : record.syncType === 'QUERY' ? 'green' : 'orange'">
                {{ record.syncType === 'PUSH' ? '推送' : record.syncType === 'QUERY' ? '查询' : '拉取' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'syncStatus'">
              <a-tag :color="record.syncStatus === 1 ? 'success' : 'error'">{{ record.syncStatus === 1 ? '成功' : '失败' }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { inventorySyncApi, CHANNEL_CODE_MAP, type InventorySyncRecord } from '@/api/trade'

const loading = ref(false)
const tableData = ref<InventorySyncRecord[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns: any[] = [
  { title: '渠道', dataIndex: 'channelCode', key: 'channelCode', width: 100 },
  { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 120 },
  { title: '内部库存', dataIndex: 'internalQty', key: 'internalQty', width: 80 },
  { title: '外部库存', dataIndex: 'externalQty', key: 'externalQty', width: 80 },
  { title: '同步数量', dataIndex: 'syncQty', key: 'syncQty', width: 80 },
  { title: '同步类型', dataIndex: 'syncType', key: 'syncType', width: 80 },
  { title: '状态', dataIndex: 'syncStatus', key: 'syncStatus', width: 80 },
  { title: '同步时间', dataIndex: 'syncTime', key: 'syncTime', width: 150 },
  { title: '错误信息', dataIndex: 'errorMsg', key: 'errorMsg', width: 200 }
]

function getChannelName(code: string) {
  return CHANNEL_CODE_MAP[code]?.name || code
}

function getChannelColor(code: string) {
  const type = CHANNEL_CODE_MAP[code]?.type
  if (type === 'ECOMMERCE') return 'blue'
  if (type === 'SOCIAL') return 'green'
  if (type === 'ERP') return 'orange'
  return 'purple'
}

async function loadData() {
  loading.value = true
  try {
    const result = await inventorySyncApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.data.data.records
    pagination.total = result.data.data.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.table-card { background: #fff; }
</style>