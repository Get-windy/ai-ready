<template>
  <div class="erp-page">
    <a-card title="入库管理">
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
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <StatusTag :status="record.status" :map="INBOUND_STATUS" />
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            ¥{{ record.totalAmount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'action'">
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
        </template>
      </a-table>
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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import request from '@/utils/request'
import { INBOUND_STATUS } from '@/utils/statusConfig'

const loading = ref(false)
const tableData = ref<any[]>([])
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

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

const columns = [
  { title: '入库单号', dataIndex: 'inboundNo', key: 'inboundNo', width: 150 },
  { title: '采购订单', dataIndex: 'purchaseOrderNo', key: 'purchaseOrderNo', width: 150 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '入库金额', key: 'totalAmount', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '入库日期', dataIndex: 'inboundDate', key: 'inboundDate', width: 120 },
  { title: '操作人', dataIndex: 'operator', key: 'operator', width: 100 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/erp/purchase/inbound/page', {
      params: { ...searchParams, pageNum: pagination.current, pageSize: pagination.pageSize }
    })
    const data = res.data || res
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
  } catch { message.error('获取数据失败') }
  finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { Object.assign(searchParams, { inboundNo: '', purchaseOrderNo: '', status: undefined }); handleSearch() }
const handleTableChange = (pag: any) => { pagination.current = pag.current; fetchData() }

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
.erp-page { padding: 24px; }
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
</style>
