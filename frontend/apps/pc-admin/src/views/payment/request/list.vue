<template>
  <PageContainer full-height>
    <template #header>
      <div class="payment-page-header">
        <div class="payment-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>支付管理</a-breadcrumb-item>
            <a-breadcrumb-item>支付请求</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="payment-page-header-title">支付请求</h2>
        </div>
        <div class="payment-page-header-right">
          <a-space :size="8">
            <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
            <a-button size="small" :loading="loading" @click="() => fetchData()">
              <template #icon><ReloadOutlined /></template>刷新
            </a-button>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            </span>
          </a-space>
        </div>
      </div>
    </template>

    <ErrorBoundary @error="handleError" @reset="fetchData">
      <div class="stats-cards">
        <a-row :gutter="16">
          <a-col :span="6">
            <div class="stat-card stat-card-blue">
              <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"><DollarOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">待支付</div>
                <div class="stat-value">{{ stats.pending }}</div>
                <div class="stat-desc">等待支付</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-green">
              <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"><CheckCircleOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">成功</div>
                <div class="stat-value">{{ stats.success }}</div>
                <div class="stat-desc">支付成功</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-red">
              <div class="stat-icon" style="background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);"><CloseCircleOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">失败</div>
                <div class="stat-value">{{ stats.failed }}</div>
                <div class="stat-desc">支付失败</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);"><BarChartOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">总金额</div>
                <div class="stat-value">¥{{ formatAmount(stats.totalAmount) }}</div>
                <div class="stat-desc">累计金额</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 筛选 -->
      <a-collapse v-model:active-key="filterExpanded" class="filter-collapse">
        <a-collapse-panel key="1" header="筛选条件">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-form-item label="业务单号">
                <a-input v-model:value="searchForm.bizNo" placeholder="请输入" allow-clear size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="渠道">
                <a-select v-model:value="searchForm.channel" placeholder="全部渠道" allow-clear size="small">
                  <a-select-option v-for="[key, val] in Object.entries(PAYMENT_CHANNEL_MAP)" :key="key" :value="key">{{ val.name }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="状态">
                <a-select v-model:value="searchForm.status" placeholder="全部状态" allow-clear size="small">
                  <a-select-option v-for="[key, val] in Object.entries(PAYMENT_STATUS_MAP)" :key="key" :value="Number(key)">{{ val.text }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="6" class="filter-actions">
              <a-space>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-collapse-panel>
      </a-collapse>

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
          :selectable="true"
          :min-empty-rows="10"
          row-key="id"
          @refresh="fetchData"
          @page-change="handlePageChange"
          @filter-change="handleFilterChange"
          @selection-change="handleSelectionChange"
          @cell-dblclick="handleView"
        >
          <template #empty>
            <div class="table-empty">
              <InboxOutlined v-if="!hasError" class="table-empty-icon" />
              <WarningOutlined v-else class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">{{ hasError ? '数据加载失败' : '暂无支付请求数据' }}</p>
            </div>
          </template>
          <template #amountCell="{ record }">
            <span style="color: #52c41a; font-weight: bold">¥{{ formatAmount(record.amount) }}</span>
          </template>
          <template #channelCell="{ record }">
            <a-tag :color="PAYMENT_CHANNEL_MAP[record.channel]?.color">{{ PAYMENT_CHANNEL_MAP[record.channel]?.name || record.channel }}</a-tag>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="PAYMENT_STATUS_MAP[record.status]?.color">{{ PAYMENT_STATUS_MAP[record.status]?.text || record.status }}</a-tag>
          </template>
          <template #action="{ record }">
            <a-space>
              <a-button size="small" type="link" @click="handleView(record)">详情</a-button>
              <a-button v-if="record.status === 1 && (record.channel === 'CASH' || record.channel === 'BANK')" size="small" type="link" @click="showConfirmModal(record)">确认收款</a-button>
            </a-space>
          </template>
        </BillTableList>
      </div>
    </ErrorBoundary>

    <!-- 确认收款弹窗 -->
    <a-modal v-model:open="confirmModalVisible" title="确认线下收款" @ok="handleConfirm">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="收款金额">
          <span style="font-weight: 500">¥{{ formatAmount(confirmForm.amount) }}</span>
        </a-form-item>
        <a-form-item label="收款单号">
          <a-input v-model:value="confirmForm.channelOrderNo" placeholder="请输入收款单号/流水号" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="支付请求详情" placement="right" width="560px" :footer="null">
      <a-spin :spinning="detailLoading">
        <template v-if="detailData">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="业务类型">{{ detailData.bizType }}</a-descriptions-item>
            <a-descriptions-item label="业务单号">{{ detailData.bizNo }}</a-descriptions-item>
            <a-descriptions-item label="支付金额"><span style="color:#52c41a;font-weight:bold">¥{{ formatAmount(detailData.amount) }}</span></a-descriptions-item>
            <a-descriptions-item label="支付渠道"><a-tag :color="PAYMENT_CHANNEL_MAP[detailData.channel]?.color">{{ PAYMENT_CHANNEL_MAP[detailData.channel]?.name }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="状态"><a-tag :color="PAYMENT_STATUS_MAP[detailData.status]?.color">{{ PAYMENT_STATUS_MAP[detailData.status]?.text }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="渠道订单号">{{ detailData.channelOrderNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
            <a-descriptions-item label="完成时间">{{ detailData.completeTime || '-' }}</a-descriptions-item>
          </a-descriptions>
        </template>
      </a-spin>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, DollarOutlined, CheckCircleOutlined, CloseCircleOutlined,
  BarChartOutlined, WarningOutlined, InboxOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { paymentApi, PAYMENT_CHANNEL_MAP, PAYMENT_STATUS_MAP } from '@/api/payment'

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const filterExpanded = ref<string[]>([])
const selectedRowKeys = ref<number[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({ bizNo: '', channel: undefined as string | undefined, status: undefined as number | undefined })

const stats = reactive({ pending: 0, success: 0, failed: 0, totalAmount: 0 })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>(null)
const confirmModalVisible = ref(false)
const confirmForm = reactive({ id: 0, amount: 0, channelOrderNo: '' })

const vxeColumns = computed(() => [
  { field: 'bizType', title: '业务类型', width: 100 },
  { field: 'bizNo', title: '业务单号', width: 130 },
  { field: 'amount', title: '支付金额', width: 110, align: 'right', slotName: 'amountCell' },
  { field: 'channel', title: '支付渠道', width: 100, slotName: 'channelCell' },
  { field: 'channelOrderNo', title: '渠道订单号', width: 150 },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { field: 'createTime', title: '创建时间', width: 150 },
  { field: 'action', title: '操作', width: 150, fixed: 'right', type: 'action' }
])

function formatAmount(val: number | undefined | null): string {
  return (val ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchForm.bizNo) params.bizNo = searchForm.bizNo
    if (searchForm.channel) params.channel = searchForm.channel
    if (searchForm.status !== undefined) params.status = searchForm.status
    const result = await paymentApi.pageRequest(params)
    const data = (result as any).data || result
    tableData.value = data.records || data.content || []
    pagination.total = data.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    updateStats()
  } catch (err) {
    if (!silent) { hasError.value = true; message.error('获取支付请求失败') }
    tableData.value = []
  } finally {
    if (!silent) loading.value = false
  }
}

function updateStats() {
  const list = tableData.value
  stats.pending = list.filter(r => r.status === 0 || r.status === 1).length
  stats.success = list.filter(r => r.status === 2).length
  stats.failed = list.filter(r => r.status === 3 || r.status === 4).length
  stats.totalAmount = list.reduce((s, r) => s + (r.amount || 0), 0)
}

function showConfirmModal(record: any) {
  confirmForm.id = record.id
  confirmForm.amount = record.amount
  confirmForm.channelOrderNo = ''
  confirmModalVisible.value = true
}

async function handleConfirm() {
  if (!confirmForm.channelOrderNo) { message.warning('请输入收款单号'); return }
  try {
    await paymentApi.confirmPayment(confirmForm.id, { channelOrderNo: confirmForm.channelOrderNo })
    message.success('确认收款成功')
    confirmModalVisible.value = false
    fetchData()
  } catch (e: any) { message.error(e?.message || '确认收款失败') }
}

function handleView(record: any) {
  detailData.value = record
  detailVisible.value = true
}

function handleSearch() { pagination.current = 1; fetchData() }
function handleReset() { Object.assign(searchForm, { bizNo: '', channel: undefined, status: undefined }); pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchForm, filters); pagination.current = 1; fetchData() }
function handleSelectionChange(rows: any[], ids: any[]) { selectedRowKeys.value = ids }
function handleError(err: any) { console.warn('[支付请求]', err) }

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); fetchData() }
}

onMounted(() => { fetchData(); document.addEventListener('keydown', handleKeydown) })
onUnmounted(() => { document.removeEventListener('keydown', handleKeydown) })
</script>

<style scoped>
.payment-page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.payment-page-header-left { display: flex; align-items: center; gap: 12px; }
.payment-page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.payment-page-header-right { display: flex; align-items: center; gap: 12px; }
.stats-cards { flex-shrink: 0; margin-bottom: 16px; }
.update-time { font-size: 12px; color: #999; }
.stat-card { display: flex; align-items: center; padding: 16px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); transition: all 0.3s; }
.stat-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.12); transform: translateY(-2px); }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-red { background: linear-gradient(135deg, #fff2f0 0%, #ffd8d2 100%); border: 1px solid #ffbcb3; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px; margin-right: 16px; }
.stat-content { flex: 1; }
.stat-title { font-size: 14px; color: #666; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; }
.stat-desc { font-size: 12px; color: #999; margin-top: 4px; }
.filter-collapse { flex-shrink: 0; margin-bottom: 16px; }
.filter-actions { display: flex; align-items: flex-end; padding-bottom: 4px; }
.table-card { background: #fff; border-radius: 8px; }
.table-empty { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.table-empty-icon { font-size: 48px; color: #d9d9d9; }
.table-empty-text { color: #999; margin-top: 12px; }
.shortcut-hints { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; user-select: none; }
.shortcut-hint { display: inline-flex; align-items: center; gap: 2px; padding: 1px 4px; border-radius: 3px; background: #f5f7fa; }
.shortcut-hint kbd { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 3px; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 11px; color: #606266; background: #fff; border: 1px solid #d0d5dd; border-radius: 3px; box-shadow: 0 1px 0 #d0d5dd; line-height: 18px; }
</style>
