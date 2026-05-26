<template>
  <div class="sales-order-page">
    <a-card title="销售订单管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form
          layout="inline"
          :model="queryParams"
        >
          <a-form-item label="订单号">
            <a-input
              v-model:value="queryParams.orderNo"
              placeholder="请输入订单号"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="客户">
            <a-select
              v-model:value="queryParams.customerId"
              placeholder="请选择客户"
              allow-clear
              style="width: 150px"
            >
              <a-select-option :value="1">
                客户A
              </a-select-option>
              <a-select-option :value="2">
                客户B
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="queryParams.status"
              placeholder="请选择状态"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="0">
                草稿
              </a-select-option>
              <a-select-option :value="1">
                待审批
              </a-select-option>
              <a-select-option :value="2">
                已审批
              </a-select-option>
              <a-select-option :value="3">
                部分发货
              </a-select-option>
              <a-select-option :value="4">
                完成
              </a-select-option>
              <a-select-option :value="5">
                已取消
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleSearch"
              >
                <template #icon>
                  <SearchOutlined />
                </template>
                查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ReloadOutlined />
                </template>
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 操作按钮 -->
      <div class="action-area">
        <a-space>
          <a-button
            type="primary"
            @click="handleAdd"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新建订单
          </a-button>
          <a-button @click="handleExport">
            <template #icon>
              <ExportOutlined />
            </template>
            导出
          </a-button>
        </a-space>
      </div>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            ¥{{ record.totalAmount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="handleView(record)"
              >
                查看
              </a-button>
              <a-button
                v-if="record.status === 0"
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                v-if="record.status === 0"
                type="link"
                size="small"
                @click="handleSubmit(record)"
              >
                提交
              </a-button>
              <a-button
                v-if="record.status === 1"
                type="link"
                size="small"
                @click="handleApprove(record)"
              >
                审批
              </a-button>
              <a-popconfirm
                v-if="record.status === 0"
                title="确定要删除此订单吗？"
                @confirm="handleDelete(record)"
              >
                <a-button
                  type="link"
                  size="small"
                  danger
                >
                  删除
                </a-button>
              </a-popconfirm>
              <PrintButton 
                v-if="record.status >= 2"
                templateType="order"
                :businessId="record.id"
                businessType="sales_order"
                buttonText="打印"
                buttonSize="small"
                @print-success="handlePrintSuccess(record)"
                @print-error="handlePrintError"
              />
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ReloadOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { salesOrderApi, type SalesOrder, OrderStatus } from '@/api/order'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

// 类型定义
const loading = ref(false)
const dataSource = ref<SalesOrder[]>([])

const queryParams = reactive({
  orderNo: '',
  customerId: undefined as number | undefined,
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  {
    title: '订单号',
    dataIndex: 'orderNo',
    key: 'orderNo',
    width: 150
  },
  {
    title: '客户名称',
    dataIndex: 'customerName',
    key: 'customerName',
    width: 150
  },
  {
    title: '订单日期',
    dataIndex: 'orderDate',
    key: 'orderDate',
    width: 120
  },
  {
    title: '交货日期',
    dataIndex: 'deliveryDate',
    key: 'deliveryDate',
    width: 120
  },
  {
    title: '销售员',
    dataIndex: 'salesperson',
    key: 'salesperson',
    width: 100
  },
  {
    title: '订单金额',
    key: 'totalAmount',
    width: 120
  },
  {
    title: '状态',
    key: 'status',
    width: 100
  },
  {
    title: '备注',
    dataIndex: 'remark',
    key: 'remark',
    ellipsis: true
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right'
  }
]

// 获取状态颜色
const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'warning',
    3: 'purple',
    4: 'success',
    5: 'error'
  }
  return colors[status] || 'default'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '待审批',
    2: '已审批',
    3: '部分发货',
    4: '完成',
    5: '已取消'
  }
  return texts[status] || '未知'
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

// 重置
const handleReset = () => {
  queryParams.orderNo = ''
  queryParams.customerId = undefined
  queryParams.status = undefined
  handleSearch()
}

// 新增订单
const handleAdd = () => {
  message.info('打开新增订单表单')
  // TODO: 打开新增订单表单
}

// 查看订单
const handleView = (record: SalesOrder) => {
  message.info(`查看订单: ${record.orderNo}`)
  // TODO: 打开查看订单详情
}

// 编辑订单
const handleEdit = (record: SalesOrder) => {
  message.info(`编辑订单: ${record.orderNo}`)
  // TODO: 打开编辑订单表单
}

// 提交订单
const handleSubmit = async (record: SalesOrder) => {
  try {
    await salesOrderApi.submit(record.id)
    message.success(`提交订单: ${record.orderNo}`)
    fetchData()
  } catch (error) {
    message.error('提交失败')
  }
}

// 审批订单
const handleApprove = async (record: SalesOrder) => {
  Modal.confirm({
    title: '审批订单',
    content: `确定要审批订单 "${record.orderNo}" 吗？`,
    async onOk() {
      try {
        await salesOrderApi.approve(record.id, true)
        message.success('审批成功')
        fetchData()
      } catch (error) {
        message.error('审批失败')
      }
    }
  })
}

// 删除订单
const handleDelete = async (record: SalesOrder) => {
  try {
    await salesOrderApi.delete(record.id)
    message.success(`删除订单: ${record.orderNo}`)
    fetchData()
  } catch (error) {
    message.error('删除失败')
  }
}

// 导出
const handleExport = () => {
  message.info('导出销售订单')
  // TODO: 实现导出功能
}

// 表格变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

// 获取订单
const fetchData = async () => {
  loading.value = true
  try {
    const res = await salesOrderApi.getPage({
      tenantId: 1,
      ...queryParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      dataSource.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handlePrintSuccess = (record: SalesOrder) => {
  message.success(`订单 ${record.orderNo} 打印成功`)
}

const handlePrintError = (error: any) => {
  message.error(`打印失败: ${error.message || '未知错误'}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.sales-order-page {
  padding: 24px;
}

.search-area {
  margin-bottom: 16px;
}

.action-area {
  margin-bottom: 16px;
}

:deep(.ant-table) {
  font-size: 14px;
}

.danger {
  color: #ff4d4f;
}
</style>