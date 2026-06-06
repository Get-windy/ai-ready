<template>
  <div class="erp-page">
    <a-card title="销售管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form layout="inline" :model="searchParams">
          <a-form-item label="订单号">
            <a-input v-model:value="searchParams.orderNo" placeholder="请输入订单号" allow-clear />
          </a-form-item>
          <a-form-item label="客户">
            <a-select v-model:value="searchParams.customerId" placeholder="请选择" allow-clear style="width: 150px">
              <a-select-option v-for="c in customerOptions" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchParams.status" placeholder="请选择" allow-clear style="width: 120px">
              <a-select-option v-for="(v, k) in ORDER_STATUS" :key="k" :value="Number(k)">{{ v.text }}</a-select-option>
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
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新建订单
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
            <StatusTag :status="record.status" :map="ORDER_STATUS" />
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            ¥{{ record.totalAmount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 0" @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 0" @click="handleSubmit(record)">提交</a>
              <a v-if="record.status === 1" @click="handleApprove(record)">审批</a>
              <PrintButton
                v-if="record.status >= 2"
                templateType="order"
                :businessId="record.id"
                businessType="sales_order"
                buttonText="打印"
                buttonSize="small"
                @print-success="() => message.success(`订单 ${record.orderNo} 打印成功`)"
                @print-error="(e: any) => message.error(`打印失败: ${e.message || '未知错误'}`)"
              />
              <a-popconfirm v-if="record.status === 0" title="确定要删除吗？" @confirm="handleDelete(record)">
                <a class="danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="销售订单详情" :width="700" :footer="null">
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="订单号">{{ currentRecord.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
        <a-descriptions-item label="订单日期">{{ currentRecord.orderDate }}</a-descriptions-item>
        <a-descriptions-item label="交货日期">{{ currentRecord.deliveryDate }}</a-descriptions-item>
        <a-descriptions-item label="销售员">{{ currentRecord.salesperson }}</a-descriptions-item>
        <a-descriptions-item label="订单金额">¥{{ currentRecord.totalAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <StatusTag :status="currentRecord.status" :map="ORDER_STATUS" />
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentRecord.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- 新建/编辑表单弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑销售订单' : '新建销售订单'"
      :width="600"
      :confirm-loading="formLoading"
      @ok="handleFormSubmit"
      @cancel="handleFormCancel"
    >
      <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="客户名称" name="customerId">
          <a-select v-model:value="formData.customerId" placeholder="请选择客户">
            <a-select-option v-for="c in customerOptions" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="订单日期" name="orderDate">
          <a-date-picker v-model:value="formData.orderDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="交货日期">
          <a-date-picker v-model:value="formData.deliveryDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="销售员" name="salesperson">
          <a-input v-model:value="formData.salesperson" placeholder="请输入销售员" />
        </a-form-item>
        <a-form-item label="订单金额" name="totalAmount">
          <a-input-number v-model:value="formData.totalAmount" :min="0" :precision="2" style="width: 100%" placeholder="请输入金额" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="formData.remark" :rows="3" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { salesOrderApi } from '@/api/order'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { useUserStore } from '@/stores/user'
import { PURCHASE_ORDER_STATUS as ORDER_STATUS } from '@/utils/statusConfig'
import { requiredSelectRule, requiredRule } from '@/utils/formRules'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref<any[]>([])
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const customerOptions = ref<{ id: number; name: string }[]>([])

const searchParams = reactive({
  orderNo: '',
  customerId: undefined as number | undefined,
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 150 },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 120 },
  { title: '交货日期', dataIndex: 'deliveryDate', key: 'deliveryDate', width: 120 },
  { title: '销售员', dataIndex: 'salesperson', key: 'salesperson', width: 100 },
  { title: '订单金额', key: 'totalAmount', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

const formRules = {
  customerId: [requiredSelectRule('客户')],
  orderDate: [requiredRule('订单日期')],
  salesperson: [requiredRule('销售员')],
  totalAmount: [requiredRule('订单金额')]
}

const formData = reactive({
  customerId: undefined as number | undefined,
  orderDate: undefined as any,
  deliveryDate: undefined as any,
  salesperson: '',
  totalAmount: undefined as number | undefined,
  remark: ''
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await salesOrderApi.getPage({
      tenantId: userStore.tenantId,
      ...searchParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { Object.assign(searchParams, { orderNo: '', customerId: undefined, status: undefined }); handleSearch() }
const handleTableChange = (pag: any) => { pagination.current = pag.current; fetchData() }

const handleView = (record: any) => { currentRecord.value = record; detailVisible.value = true }
const handleAdd = () => { isEdit.value = false; editingId.value = null; Object.assign(formData, { customerId: undefined, orderDate: undefined, deliveryDate: undefined, salesperson: '', totalAmount: undefined, remark: '' }); formVisible.value = true }
const handleEdit = (record: any) => { isEdit.value = true; editingId.value = record.id; Object.assign(formData, { customerId: record.customerId, orderDate: record.orderDate, deliveryDate: record.deliveryDate, salesperson: record.salesperson, totalAmount: record.totalAmount, remark: record.remark }); formVisible.value = true }

const handleSubmit = async (record: any) => {
  try { await salesOrderApi.submit(record.id); message.success('提交成功'); fetchData() }
  catch (error: any) { message.error(error?.response?.data?.message || '提交失败') }
}

const handleApprove = async (record: any) => {
  try { await salesOrderApi.approve(record.id, true); message.success('审批成功'); fetchData() }
  catch (error: any) { message.error(error?.response?.data?.message || '审批失败') }
}

const handleDelete = async (record: any) => {
  try { await salesOrderApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch (error: any) { message.error(error?.response?.data?.message || '删除失败') }
}

const handleExport = async () => {
  const hide = message.loading('正在导出...', 0)
  try {
    const blob = await salesOrderApi.export({ tenantId: userStore.tenantId, ...searchParams })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = `销售订单_${new Date().toISOString().slice(0, 10)}.xlsx`; a.click()
    window.URL.revokeObjectURL(url); message.success('导出成功')
  } catch (error: any) { message.error(error?.response?.data?.message || '导出失败') }
  finally { hide() }
}

const handleFormSubmit = async () => {
  try {
    await formRef.value?.validate()
    formLoading.value = true
    if (isEdit.value && editingId.value) {
      await salesOrderApi.update(editingId.value, formData as any)
      message.success('更新成功')
    } else {
      await salesOrderApi.create({ tenantId: userStore.tenantId, ...formData } as any)
      message.success('创建成功')
    }
    formVisible.value = false; fetchData()
  } catch (error: any) {
    if (error?.errorFields) return
    message.error(error?.response?.data?.message || '操作失败')
  } finally { formLoading.value = false }
}

const handleFormCancel = () => { formVisible.value = false }

onMounted(() => { fetchData() })
</script>

<style scoped>
.erp-page { padding: 24px; }
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
.danger { color: #ff4d4f; }
</style>
