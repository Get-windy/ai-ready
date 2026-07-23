<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="商城订单" full-height>
      <template #headerExtra>
        <a-space :size="12">
          <span class="data-status">
            <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
            <span v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</span>
          </span>
          <a-tooltip title="手动刷新"><a-button size="small" @click="fetchData"><template #icon><ReloadOutlined /></template></a-button></a-tooltip>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl+E</kbd> 导出</span>
          </span>
        </a-space>
      </template>

      <div class="stat-cards">
        <div class="stat-card stat-pending-pay">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.pendingPay }}</div>
            <div class="stat-card-label">待付款</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-pending-ship">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.pendingShip }}</div>
            <div class="stat-card-label">待发货</div>
          </div>
          <ShoppingCartOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-shipped">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.shipped }}</div>
            <div class="stat-card-label">已发货</div>
          </div>
          <CarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
            <div class="stat-card-label">交易总额</div>
          </div>
          <BarChartOutlined class="stat-card-icon" />
        </div>
      </div>

      <div class="search-area">
        <a-form layout="inline" :model="searchParams">
          <a-form-item label="订单编号">
            <a-input v-model:value="searchParams.orderNo" placeholder="请输入订单编号" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="订单状态">
            <a-select v-model:value="searchParams.status" placeholder="请选择状态" allow-clear style="width: 150px">
              <a-select-option v-for="[key, val] in Object.entries(ORDER_STATUS_MAP)" :key="key" :value="Number(key)">
                <a-tag :color="val.color" style="margin-right: 4px">{{ val.text }}</a-tag>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch"><template #icon><SearchOutlined /></template>搜索</a-button>
              <a-button @click="handleReset"><template #icon><ClearOutlined /></template>重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <div class="table-area">
        <BillTableList
          :columns="billColumns"
          :data-source="tableData"
          :loading="loading"
          :pagination="billPagination"
          :show-toolbar="false"
          :show-search="false"
          :show-add="true"
          :selectable="true"
          row-key="id"
          @page-change="handlePageChange"
          @selection-change="handleSelectionChange"
        >
          <template #batch-actions>
            <a-button v-if="selectedRowKeys.length > 0" size="small" @click="handleBatchApprove">批量审核 ({{ selectedRowKeys.length }})</a-button>
            <a-button v-if="selectedRowKeys.length > 0" size="small" @click="handleBatchShip">批量发货 ({{ selectedRowKeys.length }})</a-button>
          </template>
          <template #orderNoCell="{ record }">
            <a-button type="link" size="small" @click="handleView(record)">{{ record.orderNo }}</a-button>
          </template>
          <template #totalAmountCell="{ record }">
            <span class="currency-value">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template #actionCell="{ record }">
            <a-space :size="4">
              <a-button type="link" size="small" @click="handleView(record)">详情</a-button>
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleApprove(record)">审核</a-button>
              <a-button v-if="record.status === 1" type="link" size="small" @click="handlePay(record)">支付</a-button>
              <a-button v-if="record.status === 2" type="link" size="small" @click="openShipDialog(record)">发货</a-button>
              <a-button v-if="record.status === 2 || record.status === 3" type="link" size="small" @click="openRefundDialog(record)">退款</a-button>
            </a-space>
          </template>
        </BillTableList>
      </div>

      <!-- 订单详情抽屉 -->
      <a-drawer v-model:open="detailVisible" title="订单详情" placement="right" width="720px" :footer="null">
        <a-spin :spinning="detailLoading">
          <template v-if="detailData">
            <a-descriptions bordered :column="2" size="small">
              <a-descriptions-item label="订单编号">{{ detailData.orderNo }}</a-descriptions-item>
              <a-descriptions-item label="订单状态"><a-tag :color="getStatusColor(detailData.status)">{{ getStatusText(detailData.status) }}</a-tag></a-descriptions-item>
              <a-descriptions-item label="客户名称">{{ detailData.customerName }}</a-descriptions-item>
              <a-descriptions-item label="订单金额"><span class="currency-value">¥{{ formatAmount(detailData.totalAmount) }}</span></a-descriptions-item>
              <a-descriptions-item label="收货人">{{ detailData.consignee || '-' }}</a-descriptions-item>
              <a-descriptions-item label="联系电话">{{ detailData.consigneePhone || '-' }}</a-descriptions-item>
              <a-descriptions-item label="收货地址" :span="2">{{ detailData.shippingAddress || '-' }}</a-descriptions-item>
              <a-descriptions-item label="支付方式">{{ detailData.paymentMethod || '-' }}</a-descriptions-item>
              <a-descriptions-item label="物流公司">{{ detailData.logisticsCompany || '-' }}</a-descriptions-item>
              <a-descriptions-item label="物流单号">{{ detailData.trackingNo || '-' }}</a-descriptions-item>
              <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
              <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
            </a-descriptions>

            <a-divider>订单明细</a-divider>
            <a-table :columns="orderItemColumns" :data-source="detailData.items || []" :pagination="false" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'subtotal'">
                  <span class="currency-value">¥{{ formatAmount(record.subtotal) }}</span>
                </template>
              </template>
            </a-table>

            <!-- 审批流程 -->
            <a-divider>审批记录</a-divider>
            <a-timeline>
              <a-timeline-item v-for="(r, idx) in detailData.approvalRecords || []" :key="idx" :color="r.status === 'approved' ? 'green' : r.status === 'rejected' ? 'red' : 'blue'">
                <div class="timeline-content">
                  <div class="timeline-title">{{ r.stepName }}</div>
                  <div class="timeline-desc">{{ r.comment }}</div>
                  <div class="timeline-time">{{ r.createTime }} - {{ r.operatorName }}</div>
                </div>
              </a-timeline-item>
            </a-timeline>

            <div class="detail-footer">
              <a-space>
                <a-button v-if="detailData.status === 2" type="primary" @click="openShipDialog(detailData)">确认发货</a-button>
                <a-button v-if="detailData.status === 0" @click="handleApprove(detailData)">审核通过</a-button>
              </a-space>
            </div>
          </template>
        </a-spin>
      </a-drawer>

      <!-- 发货弹窗 -->
      <a-modal v-model:open="shipVisible" title="确认发货" :confirm-loading="shipLoading" @ok="handleShipConfirm">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="物流公司" required>
            <a-select v-model:value="shipForm.logisticsCompany" placeholder="请选择物流公司">
              <a-select-option value="SF">顺丰速运</a-select-option>
              <a-select-option value="YT">圆通速递</a-select-option>
              <a-select-option value="ZTO">中通快递</a-select-option>
              <a-select-option value="STO">申通快递</a-select-option>
              <a-select-option value="YUNDA">韵达快递</a-select-option>
              <a-select-option value="EMS">EMS</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="物流单号" required>
            <a-input v-model:value="shipForm.trackingNo" placeholder="请输入物流单号" />
          </a-form-item>
          <a-form-item label="发货备注">
            <a-textarea v-model:value="shipForm.remark" :rows="2" placeholder="选填" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 退款弹窗 -->
      <a-modal v-model:open="refundVisible" title="退款处理" :confirm-loading="refundLoading" @ok="handleRefundConfirm">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="订单金额">
            <span class="currency-value">¥{{ formatAmount(refundForm.totalAmount) }}</span>
          </a-form-item>
          <a-form-item label="退款金额" required>
            <a-input-number v-model:value="refundForm.refundAmount" :min="0.01" :max="refundForm.totalAmount" :precision="2" style="width: 100%" />
          </a-form-item>
          <a-form-item label="退款原因" required>
            <a-select v-model:value="refundForm.reason" placeholder="请选择退款原因">
              <a-select-option value="质量问题">质量问题</a-select-option>
              <a-select-option value="商品与描述不符">商品与描述不符</a-select-option>
              <a-select-option value="发错货">发错货</a-select-option>
              <a-select-option value="不想要了">不想要了</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="退款说明">
            <a-textarea v-model:value="refundForm.remark" :rows="2" placeholder="选填" />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined, SearchOutlined, ClearOutlined,
  DollarOutlined, ShoppingCartOutlined, CarOutlined, BarChartOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { mallOrderApi } from '@/api/erp/mall'
import { exportCsv } from '@/utils/exportCsv'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const selectedRowKeys = ref<any[]>([])

const ORDER_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'orange' },
  1: { text: '待付款', color: 'blue' },
  2: { text: '待发货', color: 'processing' },
  3: { text: '已发货', color: 'purple' },
  4: { text: '已完成', color: 'green' },
  5: { text: '已取消', color: 'default' },
  6: { text: '退款中', color: 'red' },
  7: { text: '已退款', color: 'magenta' }
}

function getStatusText(status: number) { return ORDER_STATUS_MAP[status]?.text || '未知' }
function getStatusColor(status: number) { return ORDER_STATUS_MAP[status]?.color || 'default' }

const stats = reactive({ pendingPay: 0, pendingShip: 0, shipped: 0, totalAmount: 0 })
const searchParams = reactive({ orderNo: '', status: undefined as number | undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))
const detailVisible = ref(false)
const detailData = ref<any>(null)
const detailLoading = ref(false)

const billColumns = [
  { title: '订单编号', field: 'orderNo', key: 'orderNo', width: 160, type: 'slot', slotName: 'orderNoCell' },
  { title: '客户名称', field: 'customerName', key: 'customerName', width: 180 },
  { title: '订单金额', field: 'totalAmount', key: 'totalAmount', width: 120, align: 'right', type: 'slot', slotName: 'totalAmountCell' },
  { title: '订单状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '物流单号', field: 'trackingNo', key: 'trackingNo', width: 150 },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', type: 'action', width: 200, fixed: 'right', slotName: 'actionCell' }
]

const orderItemColumns = [
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 200 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'right' },
  { title: '单价', dataIndex: 'price', key: 'price', width: 100, align: 'right' },
  { title: '小计', dataIndex: 'subtotal', key: 'subtotal', width: 100, align: 'right' }
]

const fetchData = async () => {
  loading.value = true; hasError.value = false
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchParams.orderNo) params.orderNo = searchParams.orderNo
    if (searchParams.status !== undefined) params.status = searchParams.status
    const res: any = await mallOrderApi.page(params)
    const data = res.data || res
    tableData.value = data.records || data.content || data.list || []
    pagination.total = data.total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (error: any) {
    hasError.value = true; console.warn('[商城订单] 获取列表失败', error)
  } finally { loading.value = false }
}

const fetchStats = async () => {
  try {
    const res: any = await mallOrderApi.stats()
    const data = res.data || res
    Object.assign(stats, {
      pendingPay: data.pendingPayCount || 0, pendingShip: data.pendingShipCount || 0,
      shipped: data.shippedCount || 0, totalAmount: data.totalAmount || 0
    })
  } catch (error: any) { console.warn('[商城订单] 获取统计失败', error) }
}

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const res: any = await mallOrderApi.getById(id)
    detailData.value = res.data || res || null
  } catch (error: any) { console.warn('[商城订单] 获取详情失败', error) }
  finally { detailLoading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { searchParams.orderNo = ''; searchParams.status = undefined; pagination.current = 1; fetchData() }
const handlePageChange = (page: number, pageSize: number) => { pagination.current = page; pagination.pageSize = pageSize; fetchData() }
const handleSelectionChange = (_rows: any[], ids: any[]) => { selectedRowKeys.value = ids }

const handleView = (record: any) => { detailVisible.value = true; fetchDetail(record.id) }

const handleApprove = async (record: any) => {
  try {
    await mallOrderApi.approve(record.id)
    message.success('审核通过'); fetchData(); fetchStats()
  } catch { message.error('操作失败') }
}

const handlePay = async (record: any) => {
  try {
    await mallOrderApi.pay(record.id)
    message.success('支付成功'); fetchData(); fetchStats()
  } catch { message.error('支付失败') }
}

// 发货
const shipVisible = ref(false)
const shipLoading = ref(false)
const shipForm = reactive({ orderId: undefined as number | undefined, logisticsCompany: '', trackingNo: '', remark: '' })

function openShipDialog(record: any) {
  shipForm.orderId = record.id; shipForm.logisticsCompany = ''; shipForm.trackingNo = ''; shipForm.remark = ''
  shipVisible.value = true
}

async function handleShipConfirm() {
  if (!shipForm.logisticsCompany) { message.warning('请选择物流公司'); return }
  if (!shipForm.trackingNo) { message.warning('请输入物流单号'); return }
  shipLoading.value = true
  try {
    await mallOrderApi.ship(shipForm.orderId!, { logisticsCompany: shipForm.logisticsCompany, trackingNo: shipForm.trackingNo, remark: shipForm.remark })
    message.success('发货成功'); shipVisible.value = false; fetchData(); fetchStats()
    if (detailVisible.value && detailData.value?.id === shipForm.orderId) { fetchDetail(shipForm.orderId!) }
  } catch { message.error('发货失败') }
  finally { shipLoading.value = false }
}

// 退款
const refundVisible = ref(false)
const refundLoading = ref(false)
const refundForm = reactive({ orderId: undefined as number | undefined, totalAmount: 0, refundAmount: 0, reason: '', remark: '' })

function openRefundDialog(record: any) {
  refundForm.orderId = record.id; refundForm.totalAmount = record.totalAmount || 0
  refundForm.refundAmount = record.totalAmount || 0; refundForm.reason = ''; refundForm.remark = ''
  refundVisible.value = true
}

async function handleRefundConfirm() {
  if (!refundForm.refundAmount || refundForm.refundAmount <= 0) { message.warning('请输入退款金额'); return }
  if (!refundForm.reason) { message.warning('请选择退款原因'); return }
  refundLoading.value = true
  try {
    await mallOrderApi.refund(refundForm.orderId!, { amount: refundForm.refundAmount, reason: refundForm.reason, remark: refundForm.remark })
    message.success('退款成功'); refundVisible.value = false; fetchData(); fetchStats()
    if (detailVisible.value && detailData.value?.id === refundForm.orderId) { fetchDetail(refundForm.orderId!) }
  } catch { message.error('退款失败') }
  finally { refundLoading.value = false }
}

function handleBatchApprove() {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择订单'); return }
  Modal.confirm({
    title: '批量审核', content: `确定审核 ${selectedRowKeys.value.length} 个订单?`,
    onOk: async () => {
      try {
        await mallOrderApi.batchApprove(selectedRowKeys.value)
        message.success('批量审核成功'); selectedRowKeys.value = []; fetchData(); fetchStats()
      } catch { message.error('批量审核失败') }
    }
  })
}

function handleBatchShip() {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择订单'); return }
  Modal.confirm({
    title: '批量发货', content: `确定批量发货 ${selectedRowKeys.value.length} 个订单?`,
    onOk: async () => {
      try {
        await mallOrderApi.batchShip(selectedRowKeys.value)
        message.success('批量发货成功'); selectedRowKeys.value = []; fetchData(); fetchStats()
      } catch { message.error('批量发货失败') }
    }
  })
}

function handleError(error: Error) { hasError.value = true; console.warn('[商城订单] 页面错误', error) }

function formatAmount(amount: number): string {
  if (!amount) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); fetchData() }
  if (e.ctrlKey && e.key === 'e') { e.preventDefault(); exportCsv(['订单编号', '客户名称', '订单金额', '状态', '创建时间'], tableData.value.map(r => [r.orderNo, r.customerName, r.totalAmount, getStatusText(r.status), r.createTime]), '商城订单') }
}

onMounted(() => { fetchData(); fetchStats(); document.addEventListener('keydown', handleKeydown) })
</script>

<style scoped>
.stat-cards { display: flex; gap: 16px; margin-bottom: 16px; }
.stat-card { flex: 1; display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-radius: 8px; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.stat-pending-pay { border-left: 4px solid #1890ff; }
.stat-pending-ship { border-left: 4px solid #fa8c16; }
.stat-shipped { border-left: 4px solid #722ed1; }
.stat-total { border-left: 4px solid #52c41a; }
.stat-card-value { font-size: 24px; font-weight: 600; font-family: 'SFMono-Regular', Consolas, monospace; color: #333; }
.stat-card-label { font-size: 13px; color: #666; margin-top: 4px; }
.stat-card-icon { font-size: 32px; color: rgba(0,0,0,.15); }
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; font-variant-numeric: tabular-nums; }
.data-status { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #666; }
.update-time { color: #999; }
.detail-footer { display: flex; justify-content: flex-end; margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0; }
.timeline-content .timeline-title { font-size: 14px; font-weight: 500; color: #303133; }
.timeline-content .timeline-desc { font-size: 12px; color: #606266; margin-top: 4px; }
.timeline-content .timeline-time { font-size: 12px; color: #909399; margin-top: 4px; }
.shortcut-hints { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; user-select: none; }
.shortcut-hint { display: inline-flex; align-items: center; gap: 2px; padding: 1px 4px; border-radius: 3px; background: #f5f7fa; }
.shortcut-hint kbd { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 3px; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 11px; color: #606266; background: #fff; border: 1px solid #d0d5dd; border-radius: 3px; box-shadow: 0 1px 0 #d0d5dd; line-height: 18px; }
</style>
