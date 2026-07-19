<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">
        外部订单
      </h2>
    </div>
    <div class="page-container__body">
      <a-card
        :bordered="false"
        class="table-card"
      >
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'channelCode'">
              <a-tag :color="getChannelColor(record.channelCode)">
                {{ getChannelName(record.channelCode) }}
              </a-tag>
            </template>
            <template v-if="column.key === 'processStatus'">
              <a-tag :color="STATUS_COLOR_MAP[record.processStatus]">
                {{ STATUS_TEXT_MAP[record.processStatus] }}
              </a-tag>
            </template>
            <template v-if="column.key === 'rawData'">
              <a-button
                type="link"
                size="small"
                @click="showRawData(record)"
              >
                查看原始数据
              </a-button>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button
                  v-if="record.processStatus === 3"
                  type="link"
                  size="small"
                  @click="handleRetry(record)"
                >
                  重试
                </a-button>
                <a-button
                  v-if="record.processStatus === 2"
                  type="link"
                  size="small"
                >
                  查看订单
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal
      v-model:open="rawDataModalVisible"
      title="原始订单数据"
      width="700px"
      :footer="null"
    >
      <pre style="background: #f5f5f5; padding: 16px; overflow: auto; max-height: 400px">{{ rawJsonData }}</pre>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { externalOrderApi, CHANNEL_CODE_MAP, type ExternalOrderRaw } from '@/api/trade'

const loading = ref(false)
const tableData = ref<ExternalOrderRaw[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const rawDataModalVisible = ref(false)
const rawJsonData = ref('')

const STATUS_TEXT_MAP: Record<number, string> = { 0: '待处理', 1: '已转换', 2: '已入库', 3: '失败' }
const STATUS_COLOR_MAP: Record<number, string> = { 0: 'warning', 1: 'processing', 2: 'success', 3: 'error' }

const columns: any[] = [
  { title: '渠道', dataIndex: 'channelCode', key: 'channelCode', width: 100 },
  { title: '外部订单号', dataIndex: 'externalOrderId', key: 'externalOrderId', width: 150 },
  { title: '接收时间', dataIndex: 'receiveTime', key: 'receiveTime', width: 150 },
  { title: '处理状态', dataIndex: 'processStatus', key: 'processStatus', width: 80 },
  { title: '内部订单ID', dataIndex: 'internalOrderId', key: 'internalOrderId', width: 120 },
  { title: '重试次数', dataIndex: 'retryCount', key: 'retryCount', width: 80 },
  { title: '原始数据', key: 'rawData', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
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
    const result = await externalOrderApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.data.data.records
    pagination.total = result.data.data.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showRawData(record: any) {
  try {
    rawJsonData.value = JSON.stringify(JSON.parse(record.rawData), null, 2)
  } catch {
    rawJsonData.value = record.rawData
  }
  rawDataModalVisible.value = true
}

async function handleRetry(record: any) {
  await externalOrderApi.retry(record.id)
  message.success('已重新加入处理队列')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.table-card { background: #fff; }
</style>