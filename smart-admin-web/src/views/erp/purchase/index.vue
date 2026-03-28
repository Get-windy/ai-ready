<template>
  <div class="purchase-order-page">
    <a-card title="采购订单管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form layout="inline" :model="queryParams">
          <a-form-item label="订单号">
            <a-input v-model:value="queryParams.orderNo" placeholder="请输入订单号" allow-clear />
          </a-form-item>
          <a-form-item label="供应商">
            <a-select v-model:value="queryParams.supplierId" placeholder="请选择" allow-clear style="width: 150px">
              <a-select-option :value="1">供应商A</a-select-option>
              <a-select-option :value="2">供应商B</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="queryParams.status" placeholder="请选择" allow-clear style="width: 120px">
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">待审批</a-select-option>
              <a-select-option :value="2">已审批</a-select-option>
              <a-select-option :value="3">部分入库</a-select-option>
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
              <a-popconfirm
                title="确定要删除吗？"
                @confirm="handleDelete(record)"
                v-if="record.status === 0"
              >
                <a class="danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { purchaseOrderApi, type PurchaseOrder } from '@/api/purchase'

const loading = ref(false)
const dataSource = ref<PurchaseOrder[]>([])

const queryParams = reactive({
  orderNo: '',
  supplierId: undefined as number | undefined,
  status: undefined as number | undefined,
  tenantId: 1
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 120 },
  { title: '订单金额', dataIndex: 'totalAmountWithTax', key: 'totalAmountWithTax', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '采购员', dataIndex: 'purchaserName', key: 'purchaserName' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'orange',
    2: 'green',
    3: 'blue',
    4: 'success',
    5: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '待审批',
    2: '已审批',
    3: '部分入库',
    4: '完成',
    5: '已取消'
  }
  return texts[status] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await purchaseOrderApi.page({
      current: pagination.current,
      size: pagination.pageSize,
      ...queryParams
    })
    dataSource.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.orderNo = ''
  queryParams.supplierId = undefined
  queryParams.status = undefined
  handleSearch()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const handleAdd = () => {
  message.info('新建订单')
}

const handleView = (record: PurchaseOrder) => {
  message.info('查看订单: ' + record.orderNo)
}

const handleEdit = (record: PurchaseOrder) => {
  message.info('编辑订单: ' + record.orderNo)
}

const handleSubmit = async (record: PurchaseOrder) => {
  try {
    await purchaseOrderApi.submit(record.id)
    message.success('提交成功')
    fetchData()
  } catch (error) {
    message.error('提交失败')
  }
}

const handleApprove = async (record: PurchaseOrder) => {
  try {
    await purchaseOrderApi.approve(record.id)
    message.success('审批成功')
    fetchData()
  } catch (error) {
    message.error('审批失败')
  }
}

const handleDelete = async (record: PurchaseOrder) => {
  try {
    await purchaseOrderApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    message.error('删除失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.purchase-order-page {
  padding: 0;
}

.search-area {
  margin-bottom: 16px;
}

.action-area {
  margin-bottom: 16px;
}

.danger {
  color: #ff4d4f;
}
</style>