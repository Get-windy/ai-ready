<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">
        API监控
      </h2>
    </div>
    <div class="page-container__body">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-card
            title="API状态"
            :bordered="false"
          >
            <a-statistic
              title="服务状态"
              :value="healthStatus"
              :value-style="{ color: healthStatus === 'UP' ? '#3f8600' : '#cf1322' }"
            >
              <template #suffix>
                <a-tag :color="healthStatus === 'UP' ? 'success' : 'error'">
                  {{ healthStatus }}
                </a-tag>
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card
            title="待处理订单"
            :bordered="false"
          >
            <a-statistic
              title="待处理数量"
              :value="pendingCount"
              suffix="笔"
            >
              <template #suffix>
                <a-button
                  type="link"
                  size="small"
                  @click="goToExternalOrder"
                >
                  查看
                </a-button>
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card
            title="库存查询"
            :bordered="false"
          >
            <a-statistic
              title="今日查询"
              :value="1234"
              suffix="次"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card
            title="同步成功率"
            :bordered="false"
          >
            <a-statistic
              title="成功率"
              :value="98.5"
              suffix="%"
              :value-style="{ color: '#3f8600' }"
            />
          </a-card>
        </a-col>
      </a-row>

      <a-card
        title="快速测试"
        :bordered="false"
        style="margin-top: 16px"
      >
        <a-form layout="inline">
          <a-form-item label="SKU编码">
            <a-input
              v-model:value="testSkuCode"
              placeholder="输入SKU编码"
              style="width: 200px"
            />
          </a-form-item>
          <a-form-item>
            <a-button
              type="primary"
              @click="testInventoryQuery"
            >
              库存查询
            </a-button>
          </a-form-item>
          <a-form-item>
            <a-button @click="testHealth">
              健康检查
            </a-button>
          </a-form-item>
        </a-form>
        <div
          v-if="testResult"
          style="margin-top: 16px"
        >
          <a-descriptions
            title="查询结果"
            :column="2"
            bordered
          >
            <a-descriptions-item label="SKU编码">
              {{ testResult.skuCode }}
            </a-descriptions-item>
            <a-descriptions-item label="可用库存">
              {{ testResult.availableQuantity }}
            </a-descriptions-item>
            <a-descriptions-item label="锁定库存">
              {{ testResult.lockedQuantity }}
            </a-descriptions-item>
            <a-descriptions-item label="查询时间">
              {{ new Date(testResult.queryTimestamp).toLocaleString() }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </a-card>

      <a-card
        title="库存同步记录"
        :bordered="false"
        style="margin-top: 16px"
      >
        <a-table
          :columns="syncColumns"
          :data-source="syncData"
          :loading="syncLoading"
          :pagination="syncPagination"
          row-key="id"
          @change="handleSyncTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'syncType'">
              <a-tag :color="record.syncType === 'PUSH' ? 'blue' : 'green'">
                {{ record.syncType === 'PUSH' ? '推送' : '查询' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'syncStatus'">
              <a-tag :color="record.syncStatus === 1 ? 'success' : 'error'">
                {{ record.syncStatus === 1 ? '成功' : '失败' }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { apiMonitorApi, inventorySyncApi, externalOrderApi, type InventoryQueryResult, type InventorySyncRecord } from '@/api/trade'

const router = useRouter()
const healthStatus = ref('UP')
const pendingCount = ref(0)
const testSkuCode = ref('SKU001')
const testResult = ref<InventoryQueryResult | null>(null)
const syncLoading = ref(false)
const syncData = ref<InventorySyncRecord[]>([])
const syncPagination = reactive({ current: 1, pageSize: 10, total: 0 })

const syncColumns: any[] = [
  { title: '渠道', dataIndex: 'channelCode', key: 'channelCode', width: 100 },
  { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode', width: 120 },
  { title: '同步数量', dataIndex: 'syncQty', key: 'syncQty', width: 80 },
  { title: '类型', dataIndex: 'syncType', key: 'syncType', width: 80 },
  { title: '状态', dataIndex: 'syncStatus', key: 'syncStatus', width: 80 },
  { title: '同步时间', dataIndex: 'syncTime', key: 'syncTime', width: 150 },
  { title: '错误信息', dataIndex: 'errorMsg', key: 'errorMsg', width: 200 }
]

async function loadHealth() {
  const result = await apiMonitorApi.health()
  healthStatus.value = result.data.data.status || 'UP'
}

async function loadPendingCount() {
  const result = await externalOrderApi.countPending()
  pendingCount.value = result.data.data
}

async function loadSyncRecords() {
  syncLoading.value = true
  try {
    const result = await inventorySyncApi.page({ pageNum: syncPagination.current, pageSize: syncPagination.pageSize })
    syncData.value = result.data.data.records
    syncPagination.total = result.data.data.total
  } finally { syncLoading.value = false }
}

async function testInventoryQuery() {
  if (!testSkuCode.value) { message.warning('请输入SKU编码'); return }
  const result = await inventorySyncApi.query(testSkuCode.value)
  testResult.value = result.data.data
}

async function testHealth() {
  await loadHealth()
  message.success('API健康')
}

function handleSyncTableChange(p: any) {
  syncPagination.current = p.current
  syncPagination.pageSize = p.pageSize
  loadSyncRecords()
}

function goToExternalOrder() {
  router.push('/trade/external-order')
}

onMounted(() => {
  loadHealth()
  loadPendingCount()
  loadSyncRecords()
})
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
</style>