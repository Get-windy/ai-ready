<template>
  <PageContainer full-height>
    <template #header>
      <div class="payment-page-header">
        <div class="payment-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>支付管理</a-breadcrumb-item>
            <a-breadcrumb-item>支付记录</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="payment-page-header-title">支付记录</h2>
        </div>
        <div class="payment-page-header-right">
          <a-space :size="8">
            <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
            <a-button size="small" :loading="loading" @click="() => fetchData()"><template #icon><ReloadOutlined /></template>刷新</a-button>
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
                <div class="stat-desc">累计支付</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-blue">
              <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"><DollarOutlined /></div>
              <div class="stat-content">
                <div class="stat-title">笔数</div>
                <div class="stat-value">{{ pagination.total }}</div>
                <div class="stat-desc">总记录数</div>
              </div>
            </div>
          </a-col>
        </a-row>
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
              <p class="table-empty-text">{{ hasError ? '数据加载失败' : '暂无支付记录' }}</p>
            </div>
          </template>
          <template #amountCell="{ record }">
            <span style="color: #52c41a; font-weight: bold">¥{{ formatAmount(record.amount) }}</span>
          </template>
          <template #channelCell="{ record }">
            <a-tag :color="PAYMENT_CHANNEL_MAP[record.channel]?.color">{{ PAYMENT_CHANNEL_MAP[record.channel]?.name || record.channel }}</a-tag>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="record.status === 2 ? 'success' : 'error'">{{ record.status === 2 ? '成功' : '失败' }}</a-tag>
          </template>
          <template #action="{ record }">
            <a-button size="small" type="link" @click="handleView(record)">详情</a-button>
          </template>
        </BillTableList>
      </div>
    </ErrorBoundary>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="支付记录详情" placement="right" width="560px" :footer="null">
      <a-descriptions v-if="detailData" :column="2" bordered size="small">
        <a-descriptions-item label="渠道订单号">{{ detailData.channelOrderNo }}</a-descriptions-item>
        <a-descriptions-item label="渠道交易号">{{ detailData.channelTradeNo }}</a-descriptions-item>
        <a-descriptions-item label="支付渠道"><a-tag :color="PAYMENT_CHANNEL_MAP[detailData.channel]?.color">{{ PAYMENT_CHANNEL_MAP[detailData.channel]?.name }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="支付金额"><span style="color:#52c41a;font-weight:bold">¥{{ formatAmount(detailData.amount) }}</span></a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="detailData.status === 2 ? 'success' : 'error'">{{ detailData.status === 2 ? '成功' : '失败' }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="回调时间">{{ detailData.callbackTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, DollarOutlined, CheckCircleOutlined, CloseCircleOutlined, BarChartOutlined, WarningOutlined, InboxOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { paymentApi, PAYMENT_CHANNEL_MAP } from '@/api/payment'

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const stats = reactive({ success: 0, failed: 0, totalAmount: 0 })
const detailVisible = ref(false)
const detailData = ref<any>(null)

const vxeColumns = computed(() => [
  { field: 'channelOrderNo', title: '渠道订单号', width: 150 },
  { field: 'channelTradeNo', title: '渠道交易号', width: 150 },
  { field: 'channel', title: '支付渠道', width: 100, slotName: 'channelCell' },
  { field: 'amount', title: '支付金额', width: 110, align: 'right', slotName: 'amountCell' },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { field: 'callbackTime', title: '回调时间', width: 150 },
  { field: 'createTime', title: '创建时间', width: 150 },
  { field: 'action', title: '操作', width: 80, fixed: 'right', type: 'action' }
])

function formatAmount(val: number | undefined | null): string {
  return (val ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const result = await paymentApi.pageRecord({ pageNum: pagination.current, pageSize: pagination.pageSize })
    const data = (result as any).data || result
    tableData.value = data.records || data.content || []
    pagination.total = data.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    stats.success = tableData.value.filter(r => r.status === 2).length
    stats.failed = tableData.value.filter(r => r.status !== 2).length
    stats.totalAmount = tableData.value.reduce((s, r) => s + (r.amount || 0), 0)
  } catch (err) {
    if (!silent) { hasError.value = true; message.error('获取支付记录失败') }
    tableData.value = []
  } finally { if (!silent) loading.value = false }
}

function handleView(record: any) { detailData.value = record; detailVisible.value = true }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleError(err: any) { console.warn('[支付记录]', err) }

onMounted(() => fetchData())
</script>

<style scoped>
.payment-page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.payment-page-header-left { display: flex; align-items: center; gap: 12px; }
.payment-page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.payment-page-header-right { display: flex; align-items: center; gap: 12px; }
.stats-cards { flex-shrink: 0; margin-bottom: 16px; }
.update-time { font-size: 12px; color: #999; }
.stat-card { display: flex; align-items: center; padding: 16px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-red { background: linear-gradient(135deg, #fff2f0 0%, #ffd8d2 100%); border: 1px solid #ffbcb3; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px; margin-right: 16px; }
.stat-content { flex: 1; }
.stat-title { font-size: 14px; color: #666; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; }
.stat-desc { font-size: 12px; color: #999; margin-top: 4px; }
.table-card { background: #fff; border-radius: 8px; }
.table-empty { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.table-empty-icon { font-size: 48px; color: #d9d9d9; }
.table-empty-text { color: #999; margin-top: 12px; }
</style>
