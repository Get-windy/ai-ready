<template>
  <PageContainer full-height>
    <template #header>
      <div class="recon-page-header">
        <div class="recon-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>支付管理</a-breadcrumb-item>
            <a-breadcrumb-item>支付对账</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="recon-page-header-title">支付对账</h2>
        </div>
        <div class="recon-page-header-right">
          <a-space :size="8">
            <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
            <a-button size="small" :loading="loading" @click="() => fetchData()"><template #icon><ReloadOutlined /></template>刷新</a-button>
            <a-button type="primary" size="small" @click="showExecuteModal"><template #icon><PlayCircleOutlined /></template>执行对账</a-button>
          </a-space>
        </div>
      </div>
    </template>

    <ErrorBoundary @error="handleError" @reset="fetchData">
      <div class="stats-cards">
        <a-row :gutter="16">
          <a-col :span="6">
            <div class="stat-card stat-card-green">
              <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"><CheckCircleOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">已对平</div>
                <div class="stat-value">{{ stats.matched }}</div>
                <div class="stat-desc">对账一致</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-orange">
              <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);"><WarningOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">有差异</div>
                <div class="stat-value">{{ stats.diff }}</div>
                <div class="stat-desc">需要处理</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-blue">
              <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"><SyncOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">处理中</div>
                <div class="stat-value">{{ stats.processing }}</div>
                <div class="stat-desc">正在处理</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);"><DollarOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">差异金额</div>
                <div class="stat-value">¥{{ formatAmount(stats.diffAmount) }}</div>
                <div class="stat-desc">待处理差异</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="渠道">
            <a-select v-model:value="searchForm.channel" placeholder="全部渠道" allow-clear style="width: 150px">
              <a-select-option v-for="[key, val] in Object.entries(PAYMENT_CHANNEL_MAP)" :key="key" :value="key">{{ val.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchForm.status" placeholder="全部状态" allow-clear style="width: 150px">
              <a-select-option v-for="[key, val] in Object.entries(RECON_STATUS_MAP)" :key="key" :value="Number(key)">{{ val.text }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="日期">
            <a-range-picker v-model:value="searchForm.dateRange" style="width: 240px" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" size="small" @click="handleSearch"><template #icon><SearchOutlined /></template>搜索</a-button>
              <a-button size="small" @click="handleReset"><template #icon><ClearOutlined /></template>重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <div class="table-card">
        <BillTableList
          ref="tableRef"
          :columns="vxeColumns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          :show-toolbar="false"
          :show-search="false"
          :show-add="false"
          :selectable="false"
          :min-empty-rows="10"
          row-key="id"
          @refresh="fetchData"
          @page-change="handlePageChange"
          @cell-dblclick="handleView"
        >
          <template #empty>
            <div class="table-empty">
              <InboxOutlined v-if="!hasError" class="table-empty-icon" />
              <WarningOutlined v-else class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">{{ hasError ? '数据加载失败' : '暂无对账记录' }}</p>
            </div>
          </template>
          <template #channelCell="{ record }">
            <a-tag :color="PAYMENT_CHANNEL_MAP[record.channel]?.color">{{ PAYMENT_CHANNEL_MAP[record.channel]?.name || record.channel }}</a-tag>
          </template>
          <template #totalAmountCell="{ record }">
            <span style="font-weight: 500">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template #successAmountCell="{ record }">
            <span style="color: #52c41a; font-weight: 500">¥{{ formatAmount(record.successAmount) }}</span>
          </template>
          <template #diffAmountCell="{ record }">
            <span v-if="record.diffAmount > 0" style="color: #f5222d; font-weight: bold">¥{{ formatAmount(record.diffAmount) }}</span>
            <span v-else style="color: #999">-</span>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="RECON_STATUS_MAP[record.status]?.color">{{ RECON_STATUS_MAP[record.status]?.text || record.status }}</a-tag>
          </template>
          <template #action="{ record }">
            <a-space>
              <a-button size="small" type="link" @click="handleView(record)">详情</a-button>
              <a-button v-if="record.status === 2" size="small" type="link" @click="showHandleModal(record)">处理差异</a-button>
            </a-space>
          </template>
        </BillTableList>
      </div>
    </ErrorBoundary>

    <!-- 执行对账弹窗 -->
    <a-modal v-model:open="executeModalVisible" title="执行日对账" @ok="handleExecute">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="对账日期">
          <a-date-picker v-model:value="executeForm.date" style="width: 100%" />
        </a-form-item>
        <a-form-item label="渠道">
          <a-select v-model:value="executeForm.channel" placeholder="全部渠道" allow-clear>
            <a-select-option v-for="[key, val] in Object.entries(PAYMENT_CHANNEL_MAP)" :key="key" :value="key">{{ val.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 处理差异弹窗 -->
    <a-modal v-model:open="handleModalVisible" title="处理差异" @ok="handleDifference">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="差异金额">
          <span v-if="handleForm.diffAmount > 0" style="color: #f5222d; font-weight: bold">¥{{ formatAmount(handleForm.diffAmount) }}</span>
        </a-form-item>
        <a-form-item label="处理方式">
          <a-radio-group v-model:value="handleForm.method">
            <a-radio value="manual">手工调账</a-radio>
            <a-radio value="ignore">忽略差异</a-radio>
            <a-radio value="reprocess">重新对账</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="处理备注">
          <a-textarea v-model:value="handleForm.remark" placeholder="请输入处理备注" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 对账详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="对账详情" placement="right" width="720px" :footer="null">
      <a-descriptions v-if="detailData" :column="2" bordered size="small">
        <a-descriptions-item label="对账日期">{{ detailData.reconcileDate }}</a-descriptions-item>
        <a-descriptions-item label="渠道"><a-tag :color="PAYMENT_CHANNEL_MAP[detailData.channel]?.color">{{ PAYMENT_CHANNEL_MAP[detailData.channel]?.name }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="总笔数">{{ detailData.totalCount }}</a-descriptions-item>
        <a-descriptions-item label="总金额">¥{{ formatAmount(detailData.totalAmount) }}</a-descriptions-item>
        <a-descriptions-item label="成功笔数">{{ detailData.successCount }}</a-descriptions-item>
        <a-descriptions-item label="成功金额"><span style="color:#52c41a;font-weight:500">¥{{ formatAmount(detailData.successAmount) }}</span></a-descriptions-item>
        <a-descriptions-item label="差异笔数">{{ detailData.diffCount }}</a-descriptions-item>
        <a-descriptions-item label="差异金额"><span style="color:#f5222d;font-weight:bold">¥{{ formatAmount(detailData.diffAmount) }}</span></a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="RECON_STATUS_MAP[detailData.status]?.color">{{ RECON_STATUS_MAP[detailData.status]?.text }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="对账时间">{{ detailData.createTime }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>差异明细</a-divider>
      <a-table :columns="diffColumns" :data-source="detailData.diffRecords || []" :pagination="false" size="small" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'amount'">
            <span style="color:#f5222d;font-weight:bold">¥{{ formatAmount(record.amount) }}</span>
          </template>
        </template>
      </a-table>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, SearchOutlined, ClearOutlined, PlayCircleOutlined,
  DollarOutlined, CheckCircleOutlined, WarningOutlined, SyncOutlined,
  InboxOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { reconciliationApi, PAYMENT_CHANNEL_MAP, RECON_STATUS_MAP } from '@/api/payment'
import dayjs from 'dayjs'

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({ channel: undefined as string | undefined, status: undefined as number | undefined, dateRange: undefined as [any, any] | undefined })
const detailVisible = ref(false)
const detailData = ref<any>(null)
const executeModalVisible = ref(false)
const handleModalVisible = ref(false)
const executeForm = reactive({ date: dayjs(), channel: undefined as string | undefined })
const handleForm = reactive({ id: 0, diffAmount: 0, method: 'manual', remark: '' })

const stats = reactive({ matched: 0, diff: 0, processing: 0, diffAmount: 0 })

const diffColumns = [
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '我方金额', dataIndex: 'ourAmount', key: 'ourAmount', width: 120, align: 'right' },
  { title: '渠道金额', dataIndex: 'channelAmount', key: 'channelAmount', width: 120, align: 'right' },
  { title: '差异金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' },
  { title: '说明', dataIndex: 'remark', key: 'remark', width: 200 }
]

const vxeColumns = computed(() => [
  { field: 'reconcileDate', title: '对账日期', width: 100 },
  { field: 'channel', title: '渠道', width: 100, slotName: 'channelCell' },
  { field: 'totalCount', title: '总笔数', width: 80, align: 'right' },
  { field: 'totalAmount', title: '总金额', width: 110, align: 'right', slotName: 'totalAmountCell' },
  { field: 'successCount', title: '成功笔数', width: 80, align: 'right' },
  { field: 'successAmount', title: '成功金额', width: 110, align: 'right', slotName: 'successAmountCell' },
  { field: 'diffCount', title: '差异笔数', width: 80, align: 'right' },
  { field: 'diffAmount', title: '差异金额', width: 110, align: 'right', slotName: 'diffAmountCell' },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { field: 'action', title: '操作', width: 140, fixed: 'right', type: 'action' }
])

function formatAmount(val: number | undefined | null): string {
  return (val ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchForm.channel) params.channel = searchForm.channel
    if (searchForm.status !== undefined) params.status = searchForm.status
    if (searchForm.dateRange?.[0]) params.startDate = dayjs(searchForm.dateRange[0]).format('YYYY-MM-DD')
    if (searchForm.dateRange?.[1]) params.endDate = dayjs(searchForm.dateRange[1]).format('YYYY-MM-DD')
    const result = await reconciliationApi.page(params)
    const data = (result as any).data || result
    tableData.value = data.records || data.content || []
    pagination.total = data.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    updateStats()
  } catch (err) {
    if (!silent) { hasError.value = true; message.error('获取对账数据失败') }
    tableData.value = []
  } finally { if (!silent) loading.value = false }
}

function updateStats() {
  const list = tableData.value
  stats.matched = list.filter(r => r.status === 1).length
  stats.diff = list.filter(r => r.status === 2).length
  stats.processing = list.filter(r => r.status === 3).length
  stats.diffAmount = list.reduce((s, r) => s + (r.diffAmount || 0), 0)
}

function handleView(record: any) {
  detailData.value = record
  detailVisible.value = true
}

function showExecuteModal() {
  executeForm.date = dayjs()
  executeForm.channel = undefined
  executeModalVisible.value = true
}

async function handleExecute() {
  try {
    const dateStr = executeForm.date.format('YYYY-MM-DD')
    await reconciliationApi.execute(dateStr, executeForm.channel)
    message.success('对账执行完成')
    executeModalVisible.value = false
    fetchData()
  } catch (e: any) { message.error(e?.message || '对账执行失败') }
}

function showHandleModal(record: any) {
  handleForm.id = record.id
  handleForm.diffAmount = record.diffAmount || 0
  handleForm.method = 'manual'
  handleForm.remark = ''
  handleModalVisible.value = true
}

async function handleDifference() {
  if (!handleForm.remark) { message.warning('请输入处理备注'); return }
  try {
    await reconciliationApi.handleDifference(handleForm.id, {
      method: handleForm.method,
      remark: handleForm.remark
    })
    message.success('处理完成')
    handleModalVisible.value = false
    fetchData()
  } catch (e: any) { message.error(e?.message || '处理失败') }
}

function handleSearch() { pagination.current = 1; fetchData() }
function handleReset() {
  Object.assign(searchForm, { channel: undefined, status: undefined, dateRange: undefined })
  pagination.current = 1; fetchData()
}
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleError(err: any) { console.warn('[支付对账]', err) }

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); fetchData() }
}

onMounted(() => { fetchData(); document.addEventListener('keydown', handleKeydown) })
onUnmounted(() => { document.removeEventListener('keydown', handleKeydown) })
</script>

<style scoped>
.recon-page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.recon-page-header-left { display: flex; align-items: center; gap: 12px; }
.recon-page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.recon-page-header-right { display: flex; align-items: center; gap: 12px; }
.stats-cards { flex-shrink: 0; margin-bottom: 16px; }
.update-time { font-size: 12px; color: #999; }
.stat-card { display: flex; align-items: center; padding: 16px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); border: 1px solid #ffd591; }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px; margin-right: 16px; }
.stat-content { flex: 1; }
.stat-title { font-size: 14px; color: #666; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; }
.stat-desc { font-size: 12px; color: #999; margin-top: 4px; }
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-card { background: #fff; border-radius: 8px; }
.table-empty { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.table-empty-icon { font-size: 48px; color: #d9d9d9; }
.table-empty-text { color: #999; margin-top: 12px; }
</style>
