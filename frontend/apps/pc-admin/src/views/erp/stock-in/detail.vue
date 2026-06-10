<template>
  <div class="detail-page">
    <!-- 固定顶栏 -->
    <div class="detail-header">
      <div class="detail-header__left">
        <a-button type="text" @click="goBack">
          <LeftOutlined /> 返回
        </a-button>
        <span class="detail-title">入库单详情</span>
        <span v-if="detailData?.inboundNo" class="detail-code">({{ detailData.inboundNo }})</span>
      </div>
      <div class="detail-header__right">
        <a-space>
          <PrintButton
            v-if="detailData && detailData.status >= 2"
            templateType="stock_in"
            :businessId="detailData.id"
            businessType="stock_in"
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
          <StatusTag :status="detailData.status" :map="INBOUND_STATUS" size="large" />
          <span class="action-bar-divider" />
          <a-button v-if="detailData.status === 0" type="primary" @click="handleApprove">
            <CheckCircleOutlined /> 审核
          </a-button>
          <a-button v-if="detailData.status === 1" type="primary" @click="handleExecuteInbound">
            <ImportOutlined /> 执行入库
          </a-button>
        </div>

        <!-- 基本信息 -->
        <a-card title="基本信息" class="detail-card" v-if="detailData">
          <a-descriptions :column="3" size="small">
            <a-descriptions-item label="入库单号">{{ detailData.inboundNo }}</a-descriptions-item>
            <a-descriptions-item label="采购订单号">{{ detailData.purchaseOrderNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="入库类型">{{ inboundTypeText(detailData.inboundType) }}</a-descriptions-item>
            <a-descriptions-item label="供应商">{{ detailData.supplierName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="仓库">{{ detailData.warehouseName }}</a-descriptions-item>
            <a-descriptions-item label="入库日期">{{ detailData.inboundDate || '-' }}</a-descriptions-item>
            <a-descriptions-item label="采购员">{{ detailData.purchaserName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="部门">{{ detailData.departmentName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="物流公司">{{ detailData.logisticsCompany || '-' }}</a-descriptions-item>
            <a-descriptions-item label="运单号">{{ detailData.trackingNumber || '-' }}</a-descriptions-item>
            <a-descriptions-item label="操作人">{{ detailData.operator || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</a-descriptions-item>
          </a-descriptions>
          <div v-if="detailData.remark" style="margin-top: 12px;">
            <strong>备注：</strong>{{ detailData.remark }}
          </div>
        </a-card>

        <!-- 入库明细 -->
        <a-card title="入库明细" class="detail-card" v-if="detailItems.length > 0">
          <a-table
            :data-source="detailItems"
            :columns="itemColumns"
            :pagination="false"
            size="small"
            bordered
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'amount'">
                <span class="amount-cell">¥{{ ((record.orderQuantity || 0) * (record.unitPrice || 0)).toFixed(2) }}</span>
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
          <p>未找到入库单信息</p>
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
  LeftOutlined, CheckCircleOutlined, ImportOutlined, WarningOutlined
} from '@ant-design/icons-vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { INBOUND_STATUS } from '@/utils/statusConfig'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detailData = ref<any>(null)
const detailItems = ref<any[]>([])

const itemColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 200 },
  { title: '规格', dataIndex: 'productSpec', width: 100 },
  { title: '数量', dataIndex: 'orderQuantity', width: 80, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', width: 100, align: 'right' },
  { title: '金额', dataIndex: 'amount', width: 120, align: 'right' },
]

const detailTotal = computed(() => {
  return detailItems.value.reduce((sum: number, item: any) => {
    return sum + (item.orderQuantity || 0) * (item.unitPrice || 0)
  }, 0).toFixed(2)
})

function inboundTypeText(type: number) {
  const map: Record<number, string> = { 1: '采购入库', 2: '采购退货', 3: '销售退货', 4: '调拨入库', 5: '其他' }
  return map[type] || '-'
}

function goBack() {
  router.back()
}

async function fetchDetail() {
  const id = Number(route.params.id)
  if (!id) { message.error('参数错误'); return }
  loading.value = true
  try {
    const [orderRes, itemsRes] = await Promise.all([
      request.get(`/erp/purchase/inbound/${id}`),
      request.get(`/erp/purchase/inbound/${id}/items`).catch(() => ({ data: [] })),
    ])
    detailData.value = orderRes.data || null
    detailItems.value = itemsRes.data || []
  } catch (err) {
    console.warn('[入库详情] 获取数据失败', err)
    message.error('获取入库单详情失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = () => {
  const record = detailData.value
  if (!record) return
  Modal.confirm({
    title: '审核确认',
    content: `确认审核入库单 ${record.inboundNo} 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.put(`/erp/purchase/inbound/${record.id}/approve`)
        message.success('审核成功')
        fetchDetail()
      } catch (error) {
        console.warn('[入库详情] 审核失败', error)
        message.error('审核失败')
      }
    }
  })
}

const handleExecuteInbound = () => {
  const record = detailData.value
  if (!record) return
  Modal.confirm({
    title: '入库确认',
    content: `确认执行入库单 ${record.inboundNo} 吗？`,
    okText: '确认入库',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.put(`/erp/purchase/inbound/${record.id}/execute`)
        message.success('入库完成')
        fetchDetail()
      } catch (error) {
        console.warn('[入库详情] 入库失败', error)
        message.error('入库失败')
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
</style>
