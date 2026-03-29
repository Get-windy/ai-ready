<template>
  <div class="sale-order-page">
    <a-card title="销售订单管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form layout="inline" :model="queryParams">
          <a-form-item label="订单号">
            <a-input v-model:value="queryParams.orderNo" placeholder="请输入订单号" allow-clear />
          </a-form-item>
          <a-form-item label="客户">
            <a-select v-model:value="queryParams.customerId" placeholder="请选择" allow-clear style="width: 150px">
              <a-select-option :value="1">客户A</a-select-option>
              <a-select-option :value="2">客户B</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="queryParams.status" placeholder="请选择" allow-clear style="width: 120px">
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">待审批</a-select-option>
              <a-select-option :value="2">已审批</a-select-option>
              <a-select-option :value="3">部分出库</a-select-option>
              <a-select-option :value="4">完成</a-select-option>
              <a-select-option :value="5">已取消</a-select-option>
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
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新建订单
        </a-button>
      </div>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmountWithTax'">
            ¥{{ record.totalAmountWithTax?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)" v-if="record.status === 0">编辑</a>
              <a @click="handleSubmit(record)" v-if="record.status === 0">提交</a>
              <a @click="handleApprove(record)" v-if="record.status === 1">审批</a>
              <a-popconfirm title="确定要删除吗？" @confirm="handleDelete(record)" v-if="record.status === 0">
                <a class="danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
      width="800px"
    >
      <a-form :model="formState" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="客户" name="customerId">
              <a-select v-model:value="formState.customerId" placeholder="请选择客户">
                <a-select-option :value="1">客户A</a-select-option>
                <a-select-option :value="2">客户B</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="预计发货日期" name="expectedShipDate">
              <a-date-picker v-model:value="formState.expectedShipDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="收货人" name="receiverName">
              <a-input v-model:value="formState.receiverName" placeholder="请输入收货人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="receiverPhone">
              <a-input v-model:value="formState.receiverPhone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="收货地址" name="shippingAddress" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
              <a-textarea v-model:value="formState.shippingAddress" placeholder="请输入收货地址" :rows="2" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
              <a-textarea v-model:value="formState.remark" placeholder="请输入备注" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'

interface SaleOrder {
  id: number
  orderNo: string
  customerId: number
  customerName: string
  orderDate: string
  expectedShipDate: string
  status: number
  totalAmount: number
  totalAmountWithTax: number
  receiverName: string
  receiverPhone: string
  shippingAddress: string
  remark: string
}

const loading = ref(false)
const modalVisible = ref(false)
const modalTitle = ref('新建销售订单')

const queryParams = reactive({
  orderNo: '',
  customerId: undefined as number | undefined,
  status: undefined as number | undefined
})

const formState = reactive({
  customerId: undefined,
  expectedShipDate: null,
  receiverName: '',
  receiverPhone: '',
  shippingAddress: '',
  remark: ''
})

const dataSource = ref<SaleOrder[]>([
  { id: 1, orderNo: 'SO20260328001', customerId: 1, customerName: '客户A', orderDate: '2026-03-28', expectedShipDate: '2026-03-30', status: 0, totalAmount: 10000, totalAmountWithTax: 11300, receiverName: '张三', receiverPhone: '13800138000', shippingAddress: '北京市朝阳区xxx', remark: '' },
  { id: 2, orderNo: 'SO20260328002', customerId: 2, customerName: '客户B', orderDate: '2026-03-28', expectedShipDate: '2026-04-01', status: 1, totalAmount: 20000, totalAmountWithTax: 22600, receiverName: '李四', receiverPhone: '13900139000', shippingAddress: '上海市浦东新区xxx', remark: '' },
  { id: 3, orderNo: 'SO20260328003', customerId: 1, customerName: '客户A', orderDate: '2026-03-27', expectedShipDate: '2026-03-29', status: 2, totalAmount: 15000, totalAmountWithTax: 16950, receiverName: '王五', receiverPhone: '13700137000', shippingAddress: '广州市天河区xxx', remark: '' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 3,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 120 },
  { title: '预计发货', dataIndex: 'expectedShipDate', key: 'expectedShipDate', width: 120 },
  { title: '订单金额', dataIndex: 'totalAmountWithTax', key: 'totalAmountWithTax', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '收货人', dataIndex: 'receiverName', key: 'receiverName' },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue', 4: 'success', 5: 'red' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '部分出库', 4: '完成', 5: '已取消' }
  return texts[status] || '未知'
}

const handleSearch = () => message.info('查询')
const handleReset = () => { queryParams.orderNo = ''; queryParams.customerId = undefined; queryParams.status = undefined }
const handleTableChange = (pag: any) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize }

const handleAdd = () => {
  modalTitle.value = '新建销售订单'
  Object.assign(formState, { customerId: undefined, expectedShipDate: null, receiverName: '', receiverPhone: '', shippingAddress: '', remark: '' })
  modalVisible.value = true
}

const handleView = (record: SaleOrder) => message.info('查看: ' + record.orderNo)
const handleEdit = (record: SaleOrder) => message.info('编辑: ' + record.orderNo)
const handleSubmit = (record: SaleOrder) => { record.status = 1; message.success('提交成功') }
const handleApprove = (record: SaleOrder) => { record.status = 2; message.success('审批通过') }
const handleDelete = (record: SaleOrder) => { dataSource.value = dataSource.value.filter(i => i.id !== record.id); message.success('删除成功') }
const handleModalOk = () => { modalVisible.value = false; message.success('保存成功') }

onMounted(() => {})
</script>

<style scoped>
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
.danger { color: #ff4d4f; }
</style>