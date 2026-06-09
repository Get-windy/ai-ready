<template>
  <div class="erp-page" style="padding: 16px; height: 100%; display: flex; flex-direction: column;">
    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <FileTextOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">入库单总数</div>
            <div class="summary-value">{{ statistics.totalCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
            <ClockCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待审核</div>
            <div class="summary-value warning">{{ statistics.pendingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已完成</div>
            <div class="summary-value">{{ statistics.completedCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
            <DollarOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">入库金额</div>
            <div class="summary-value">¥{{ formatAmount(statistics.totalAmount) }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card title="入库管理" style="flex: 1; overflow: hidden;" :bodyStyle="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form layout="inline" :model="searchParams">
          <a-form-item label="入库单号">
            <a-input v-model:value="searchParams.inboundNo" placeholder="请输入入库单号" allow-clear />
          </a-form-item>
          <a-form-item label="采购订单">
            <a-input v-model:value="searchParams.purchaseOrderNo" placeholder="请输入采购订单号" allow-clear />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchParams.status" placeholder="请选择" allow-clear style="width: 120px">
              <a-select-option v-for="(v, k) in INBOUND_STATUS" :key="k" :value="Number(k)">{{ v.text }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">查询</a-button>
              <a-button @click="handleReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 操作按钮 -->
      <div class="action-area">
        <a-space>
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新建入库单
          </a-button>
          <a-button @click="handleExport">
            <template #icon><ExportOutlined /></template>
            导出
          </a-button>
        </a-space>
      </div>

      <!-- 数据表格 -->
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        @page-change="handlePageChange"
      >
        <template #statusCell="{ record }">
          <StatusTag :status="record.status" :map="INBOUND_STATUS" />
        </template>
        <template #totalAmountCell="{ record }">
          ¥{{ record.totalAmount?.toFixed(2) }}
        </template>
        <template #action="{ record }">
          <a-space>
            <a @click="handleView(record)">查看</a>
            <a v-if="record.status === 0" @click="handleApprove(record)">审核</a>
            <a v-if="record.status === 1" @click="handleExecuteInbound(record)">入库</a>
            <PrintButton
              v-if="record.status >= 2"
              templateType="stock_in"
              :businessId="record.id"
              businessType="stock_in"
              buttonText="打印"
              buttonSize="small"
              @print-success="() => message.success(`入库单 ${record.inboundNo} 打印成功`)"
              @print-error="(e: any) => message.error(`打印失败: ${e.message || '未知错误'}`)"
            />
          </a-space>
        </template>
      </VxeTableList>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="入库单详情" width="700px" :footer="null">
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="入库单号">{{ currentRecord.inboundNo }}</a-descriptions-item>
        <a-descriptions-item label="采购订单">{{ currentRecord.purchaseOrderNo }}</a-descriptions-item>
        <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="入库金额">¥{{ currentRecord.totalAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <StatusTag :status="currentRecord.status" :map="INBOUND_STATUS" />
        </a-descriptions-item>
        <a-descriptions-item label="入库日期">{{ currentRecord.inboundDate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ currentRecord.operator || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined, FileTextOutlined, ClockCircleOutlined, CheckCircleOutlined, DollarOutlined } from '@ant-design/icons-vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import request from '@/utils/request'
import { INBOUND_STATUS } from '@/utils/statusConfig'

const loading = ref(false)
const tableData = ref<any[]>([])
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const tableRef = ref()

// 统计数据
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  completedCount: 0,
  totalAmount: 0
})

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const searchParams = reactive({
  inboundNo: '',
  purchaseOrderNo: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const vxeColumns = computed(() => [
  { field: 'inboundNo', title: '入库单号', width: 150 },
  { field: 'purchaseOrderNo', title: '采购订单', width: 150 },
  { field: 'supplierName', title: '供应商', width: 150 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'totalAmount', title: '入库金额', width: 120, slotName: 'totalAmountCell' },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'inboundDate', title: '入库日期', width: 120 },
  { field: 'operator', title: '操作人', width: 100 },
  { field: 'action', title: '操作', width: 220, fixed: 'right', type: 'action' },
])

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/erp/purchase/inbound/page', {
      params: { ...searchParams, pageNum: pagination.current, pageSize: pagination.pageSize }
    })
    const data = res.data || res
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
    // 更新统计
    statistics.value.totalCount = tableData.value.length
    statistics.value.pendingCount = tableData.value.filter(r => r.status === 0).length
    statistics.value.completedCount = tableData.value.filter(r => r.status >= 2).length
    statistics.value.totalAmount = tableData.value.reduce((sum, r) => sum + (r.totalAmount || 0), 0)
  } catch { message.error('获取数据失败') }
  finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { Object.assign(searchParams, { inboundNo: '', purchaseOrderNo: '', status: undefined }); handleSearch() }
const handlePageChange = (page: number, size: number) => { pagination.current = page; pagination.pageSize = size; fetchData() }

const handleCreate = () => message.info('新建入库单功能开发中')
const handleView = (record: any) => { currentRecord.value = record; detailVisible.value = true }

const handleApprove = async (record: any) => {
  try {
    await request.put(`/erp/purchase/inbound/${record.id}/approve`)
    message.success('审核成功'); fetchData()
  } catch { message.error('审核失败') }
}

const handleExecuteInbound = async (record: any) => {
  try {
    await request.put(`/erp/purchase/inbound/${record.id}/execute`)
    message.success('入库完成'); fetchData()
  } catch { message.error('入库操作失败') }
}

const handleExport = () => message.info('导出功能开发中')

onMounted(() => { fetchData() })
</script>

<style scoped>
.erp-page { padding: 16px; height: 100%; display: flex; flex-direction: column; overflow: hidden; min-height: 0; }
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.warning {
  color: #faad14;
}


</style>
