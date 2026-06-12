<template>
  <div class="detail-page">
    <!-- 固定顶栏 -->
    <div class="detail-header">
      <div class="detail-header__left">
        <a-button type="text" @click="goBack">
          <LeftOutlined /> 返回
        </a-button>
        <span class="detail-title">出库单详情</span>
        <span v-if="detailData?.shipmentNo" class="detail-code">({{ detailData.shipmentNo }})</span>
      </div>
      <div class="detail-header__right">
        <a-space>
          <PrintButton
            v-if="detailData && detailData.status >= 2"
            templateType="shipment"
            :businessId="detailData.id"
            businessType="shipment"
            buttonText="打印"
            @print-success="message.success('打印成功')"
            @print-error="(e) => message.error('打印失败')"
          />
          <a-button @click="goBack">返回</a-button>
        </a-space>
      </div>
    </div>

    <div class="detail-body">
      <a-spin :spinning="loading">
        <!-- 状态操作栏 -->
        <div class="detail-action-bar" v-if="detailData">
          <StatusTag :status="detailData.status" :map="SHIPMENT_STATUS" size="large" />
          <span class="action-bar-divider" />
          <a-button v-if="detailData.status === 0" type="primary" @click="handleApprove">
            <CheckCircleOutlined /> 审核
          </a-button>
          <a-button v-if="detailData.status === 1" type="primary" @click="handleShip">
            <ExportOutlined /> 出库
          </a-button>
        </div>

        <!-- 基本信息 -->
        <a-card title="基本信息" class="detail-card" v-if="detailData">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="出库单号">{{ detailData.shipmentNo }}</a-descriptions-item>
            <a-descriptions-item label="销售订单">{{ detailData.orderNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="客户名称">{{ detailData.customerName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="仓库">{{ detailData.warehouseName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="出库类型">{{ detailData.outboundTypeName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="出库金额">
              <span class="amount-cell">¥{{ detailData.totalAmount?.toFixed(2) || '0.00' }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="出库日期">{{ detailData.shipmentDate || '-' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <StatusTag :status="detailData.status" :map="SHIPMENT_STATUS" />
            </a-descriptions-item>
            <a-descriptions-item label="联系人">{{ detailData.contactName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="联系电话">{{ detailData.contactPhone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="业务员">{{ detailData.salesPersonName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="部门">{{ detailData.departmentName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="收货人">{{ detailData.receiverName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="收货电话">{{ detailData.receiverPhone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="收货地址" :span="2">{{ detailData.shippingAddress || '-' }}</a-descriptions-item>
            <a-descriptions-item label="物流单号">{{ detailData.trackingNo || '未填写' }}</a-descriptions-item>
            <a-descriptions-item label="操作人">{{ detailData.operator || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '无' }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 出库明细 -->
        <a-card title="出库明细" class="detail-card" v-if="detailDataItems.length > 0">
          <a-table
            :data-source="detailDataItems"
            :columns="detailItemColumns"
            :pagination="false as any"
            size="small"
            bordered
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'unitPrice' || column.dataIndex === 'amount'">
                <span class="amount-cell">¥{{ (record[column.dataIndex] || 0).toFixed(2) }}</span>
              </template>
            </template>
          </a-table>
          <div class="detail-total">
            合计：<span class="amount-cell">¥{{ detailTotal }}</span>
          </div>
        </a-card>

        <!-- 空状态 -->
        <div v-if="!loading && !detailData" class="detail-empty">
          <WarningOutlined style="font-size: 48px; color: #d9d9d9;" />
          <p>未找到出库单信息</p>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  LeftOutlined, CheckCircleOutlined, ExportOutlined, WarningOutlined
} from '@ant-design/icons-vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { SHIPMENT_STATUS } from '@/utils/statusConfig'
import request from '@/utils/request'

interface ShipmentItem {
  id: number
  productName: string
  productCode: string
  productSpec?: string
  quantity: number
  unitPrice: number
  amount: number
}

interface Shipment {
  id: number
  shipmentNo: string
  orderNo: string
  customerName: string
  warehouseName: string
  totalAmount: number
  status: number
  shipmentDate: string
  operator: string
  trackingNo?: string
  remark?: string
  outboundType?: number
  outboundTypeName?: string
  contactName?: string
  contactPhone?: string
  salesPersonName?: string
  departmentName?: string
  shippingAddress?: string
  receiverName?: string
  receiverPhone?: string
  items?: ShipmentItem[]
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detailData = ref<Shipment | null>(null)

const detailDataItems = computed(() => {
  return detailData.value?.items || []
})

const detailItemColumns: any = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 200 },
  { title: '规格', dataIndex: 'productSpec', key: 'productSpec', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' },
  { title: '小计', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' },
]

const detailTotal = computed(() => {
  const items = detailDataItems.value
  return items.reduce((sum: number, item: ShipmentItem) => {
    return sum + (item.amount || item.quantity * item.unitPrice || 0)
  }, 0).toFixed(2)
})

function goBack() {
  router.back()
}

async function fetchDetail() {
  const id = Number(route.params.id)
  if (!id) { message.error('参数错误'); return }
  loading.value = true
  try {
    const res = await request.get(`/erp/sale/outbound/${id}`)
    detailData.value = res.data || null
  } catch (err) {
    console.warn('[出库详情] 获取数据失败', err)
    message.error('获取出库单详情失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = () => {
  const record = detailData.value
  if (!record) return
  Modal.confirm({
    title: '审核确认',
    content: `确认审核出库单 ${record.shipmentNo} 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.put(`/erp/sale/outbound/${record.id}/approve`)
        message.success('审核成功')
        fetchDetail()
      } catch (error) {
        console.warn('[出库详情] 审核失败', error)
        message.error('审核失败')
      }
    }
  })
}

const handleShip = () => {
  const record = detailData.value
  if (!record) return
  Modal.confirm({
    title: '出库确认',
    content: `确认执行出库单 ${record.shipmentNo} 吗？`,
    okText: '确认出库',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.put(`/erp/sale/outbound/${record.id}/ship`)
        message.success('出库完成')
        fetchDetail()
      } catch (error) {
        console.warn('[出库详情] 出库失败', error)
        message.error('出库失败')
      }
    }
  })
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.detail-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
  z-index: 10;
}

.detail-header__left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.detail-code {
  font-size: 13px;
  color: #999;
}

.detail-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-body {
  flex: 1;
  overflow: auto;
  padding: 16px 24px;
}

.detail-action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.action-bar-divider {
  flex: 1;
}

.detail-card {
  margin-bottom: 16px;
}

.detail-total {
  text-align: right;
  padding: 12px 16px;
  font-size: 16px;
  font-weight: 600;
  border-top: 1px solid #f0f0f0;
}

.amount-cell {
  color: #ff4d4f;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  gap: 12px;
  color: #999;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
