<template>
  <div class="supplier-management">
    <a-card :bordered="false">
      <div class="page-header">
        <h2>供应商管理</h2>
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新建供应商
        </a-button>
      </div>

      <a-form layout="inline" class="search-form">
        <a-form-item label="供应商名称">
          <a-input v-model:value="searchForm.name" placeholder="请输入供应商名称" allow-clear />
        </a-form-item>
        <a-form-item label="联系人">
          <a-input v-model:value="searchForm.contact" placeholder="请输入联系人" allow-clear />
        </a-form-item>
        <a-form-item label="供应商等级">
          <a-select v-model:value="searchForm.level" placeholder="请选择等级" allow-clear style="width: 120px">
            <a-select-option value="A">A级供应商</a-select-option>
            <a-select-option value="B">B级供应商</a-select-option>
            <a-select-option value="C">C级供应商</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="供应商状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态" allow-clear style="width: 120px">
            <a-select-option value="active">合作中</a-select-option>
            <a-select-option value="inactive">暂停合作</a-select-option>
            <a-select-option value="blacklist">黑名单</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="供应商类型">
          <a-select v-model:value="searchForm.type" placeholder="请选择类型" allow-clear style="width: 150px">
            <a-select-option value="material">原材料供应商</a-select-option>
            <a-select-option value="product">产品供应商</a-select-option>
            <a-select-option value="service">服务供应商</a-select-option>
            <a-select-option value="logistics">物流供应商</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="all" tab="全部供应商" />
        <a-tab-pane key="active" tab="合作中" />
        <a-tab-pane key="inactive" tab="暂停合作" />
      </a-tabs>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="handleView(record)">{{ record.name }}</a>
          </template>
          <template v-if="column.key === 'level'">
            <a-tag :color="getLevelColor(record.level)">{{ record.level }}级</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'purchaseAmount'">
            <span class="amount">¥{{ formatAmount(record.purchaseAmount) }}</span>
          </template>
          <template v-if="column.key === 'payableAmount'">
            <span class="amount payable">¥{{ formatAmount(record.payableAmount) }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)">编辑</a>
              <a @click="handleProducts(record)">产品</a>
              <a @click="handleOrders(record)">订单</a>
              <a @click="handleEvaluate(record)">评估</a>
              <a-dropdown>
                <a class="ant-dropdown-link" @click.prevent>
                  更多 <DownOutlined />
                </a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handlePortal(record)">供应商门户</a-menu-item>
                    <a-menu-item @click="handleContact(record)">联系记录</a-menu-item>
                    <a-menu-item @click="handleBlacklist(record)" v-if="record.status !== 'blacklist'">加入黑名单</a-menu-item>
                    <a-menu-item @click="handleDelete(record)" v-if="record.status === 'inactive'">删除</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="800px"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="供应商名称" name="name" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.name" placeholder="请输入供应商名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="供应商编码" name="code" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.code" placeholder="自动生成" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="供应商类型" name="type" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-select v-model:value="formData.type" placeholder="请选择供应商类型">
                <a-select-option value="material">原材料供应商</a-select-option>
                <a-select-option value="product">产品供应商</a-select-option>
                <a-select-option value="service">服务供应商</a-select-option>
                <a-select-option value="logistics">物流供应商</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="供应商等级" name="level" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-select v-model:value="formData.level" placeholder="请选择供应商等级">
                <a-select-option value="A">A级供应商</a-select-option>
                <a-select-option value="B">B级供应商</a-select-option>
                <a-select-option value="C">C级供应商</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="联系人" name="contact" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.contact" placeholder="请输入联系人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="phone" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.phone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="电子邮箱" name="email" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.email" placeholder="请输入电子邮箱" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属行业" name="industry" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.industry" placeholder="请输入所属行业" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="公司地址" name="address">
          <a-input v-model:value="formData.address" placeholder="请输入公司地址" />
        </a-form-item>
        <a-form-item label="银行信息" name="bankInfo">
          <a-input v-model:value="formData.bankInfo" placeholder="请输入银行账户信息" />
        </a-form-item>
        <a-form-item label="合作状态" name="status">
          <a-select v-model:value="formData.status" placeholder="请选择合作状态">
            <a-select-option value="active">合作中</a-select-option>
            <a-select-option value="inactive">暂停合作</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="detailVisible"
      title="供应商详情"
      width="900px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="供应商名称">{{ supplierDetail.name }}</a-descriptions-item>
        <a-descriptions-item label="供应商编码">{{ supplierDetail.code }}</a-descriptions-item>
        <a-descriptions-item label="供应商类型">{{ supplierDetail.typeLabel }}</a-descriptions-item>
        <a-descriptions-item label="供应商等级">
          <a-tag :color="getLevelColor(supplierDetail.level)">{{ supplierDetail.level }}级</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="联系人">{{ supplierDetail.contact }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ supplierDetail.phone }}</a-descriptions-item>
        <a-descriptions-item label="电子邮箱">{{ supplierDetail.email }}</a-descriptions-item>
        <a-descriptions-item label="所属行业">{{ supplierDetail.industry }}</a-descriptions-item>
        <a-descriptions-item label="公司地址" :span="2">{{ supplierDetail.address }}</a-descriptions-item>
        <a-descriptions-item label="银行信息">{{ supplierDetail.bankInfo }}</a-descriptions-item>
        <a-descriptions-item label="合作状态">
          <a-tag :color="getStatusColor(supplierDetail.status)">{{ getStatusText(supplierDetail.status) }}</a-tag>
        </a-descriptions-item>
      </a-descriptions>

      <a-divider>业务统计</a-divider>
      <a-row :gutter="16">
        <a-col :span="6">
          <a-statistic title="采购订单" :value="supplierDetail.orderCount" suffix="单" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="采购金额" :value="supplierDetail.purchaseAmount" :precision="2" prefix="¥" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="已付款" :value="supplierDetail.paidAmount" :precision="2" prefix="¥" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="待付款" :value="supplierDetail.payableAmount" :precision="2" prefix="¥" :value-style="{ color: '#f5222d' }" />
        </a-col>
      </a-row>

      <a-divider>供应商评估</a-divider>
      <a-row :gutter="16">
        <a-col :span="6">
          <a-statistic title="质量评分" :value="supplierDetail.qualityScore" suffix="分" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="交货评分" :value="supplierDetail.deliveryScore" suffix="分" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="服务评分" :value="supplierDetail.serviceScore" suffix="分" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="综合评分" :value="supplierDetail.totalScore" suffix="分" :value-style="{ color: '#1890ff' }" />
        </a-col>
      </a-row>

      <a-divider>最近采购订单</a-divider>
      <a-table
        :columns="orderColumns"
        :data-source="supplierDetail.recentOrders"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            <span class="amount">¥{{ formatAmount(record.amount) }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getOrderStatusColor(record.status)">{{ record.statusLabel }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, DownOutlined } from '@ant-design/icons-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'

const loading = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const modalTitle = ref('新建供应商')
const activeTab = ref('all')
const formRef = ref<FormInstance>()

const searchForm = reactive({
  name: '',
  contact: '',
  level: undefined,
  status: undefined,
  type: undefined
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
  { title: '供应商名称', key: 'name', dataIndex: 'name', width: 180 },
  { title: '供应商编码', dataIndex: 'code', width: 120 },
  { title: '供应商类型', dataIndex: 'typeLabel', width: 120 },
  { title: '等级', key: 'level', dataIndex: 'level', width: 80 },
  { title: '联系人', dataIndex: 'contact', width: 100 },
  { title: '联系电话', dataIndex: 'phone', width: 120 },
  { title: '采购金额', key: 'purchaseAmount', dataIndex: 'purchaseAmount', width: 120 },
  { title: '待付款', key: 'payableAmount', dataIndex: 'payableAmount', width: 120 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const orderColumns = [
  { title: '订单编号', dataIndex: 'orderNo', width: 150 },
  { title: '订单金额', key: 'amount', dataIndex: 'amount', width: 120 },
  { title: '下单日期', dataIndex: 'orderDate', width: 100 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 }
]

const tableData = ref<any[]>([])

const formData = reactive({
  id: undefined,
  name: '',
  code: '',
  type: undefined,
  level: 'B',
  contact: '',
  phone: '',
  email: '',
  industry: '',
  address: '',
  bankInfo: '',
  status: 'active',
  remark: ''
})

const formRules = {
  name: [{ required: true, message: '请输入供应商名称' }],
  type: [{ required: true, message: '请选择供应商类型' }],
  contact: [{ required: true, message: '请输入联系人' }],
  phone: [{ required: true, message: '请输入联系电话' }]
}

const supplierDetail = ref<any>({})

onMounted(() => {
  loadTableData()
})

const loadTableData = () => {
  loading.value = true
  tableData.value = [
    { id: 1, name: '北京办公用品有限公司', code: 'SUP001', type: 'product', typeLabel: '产品供应商', level: 'A', contact: '张经理', phone: '138****1234', purchaseAmount: 580000, payableAmount: 38000, status: 'active' },
    { id: 2, name: '上海电子设备公司', code: 'SUP002', type: 'product', typeLabel: '产品供应商', level: 'A', contact: '李总', phone: '139****5678', purchaseAmount: 1280000, payableAmount: 52000, status: 'active' },
    { id: 3, name: '广州物流运输公司', code: 'SUP003', type: 'logistics', typeLabel: '物流供应商', level: 'B', contact: '王主任', phone: '137****9012', purchaseAmount: 85000, payableAmount: 0, status: 'active' },
    { id: 4, name: '深圳IT服务商', code: 'SUP004', type: 'service', typeLabel: '服务供应商', level: 'B', contact: '赵经理', phone: '136****3456', purchaseAmount: 256000, payableAmount: 12800, status: 'inactive' }
  ]
  pagination.total = 4
  loading.value = false
}

const getLevelColor = (level: string) => {
  const colors: Record<string, string> = {
    A: 'green',
    B: 'blue',
    C: 'orange'
  }
  return colors[level] || 'default'
}

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    active: 'green',
    inactive: 'orange',
    blacklist: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    active: '合作中',
    inactive: '暂停合作',
    blacklist: '黑名单'
  }
  return texts[status] || status
}

const getOrderStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    pending: 'orange',
    confirmed: 'blue',
    received: 'green',
    completed: 'green'
  }
  return colors[status] || 'default'
}

const formatAmount = (amount: number) => {
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const handleSearch = () => {
  pagination.current = 1
  loadTableData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    contact: '',
    level: undefined,
    status: undefined,
    type: undefined
  })
  handleSearch()
}

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadTableData()
}

const handleAdd = () => {
  modalTitle.value = '新建供应商'
  generateSupplierCode()
  modalVisible.value = true
}

const generateSupplierCode = () => {
  const count = tableData.value.length + 1
  formData.code = `SUP${String(count).padStart(3, '0')}`
}

const handleView = (record: any) => {
  supplierDetail.value = {
    ...record,
    orderCount: 28,
    paidAmount: record.purchaseAmount - record.payableAmount,
    qualityScore: 92,
    deliveryScore: 88,
    serviceScore: 90,
    totalScore: 90,
    recentOrders: [
      { orderNo: 'PO20240115001', amount: 58000, orderDate: '2024-01-15', status: 'received', statusLabel: '已收货' },
      { orderNo: 'PO20240110002', amount: 32000, orderDate: '2024-01-10', status: 'completed', statusLabel: '已完成' },
      { orderNo: 'PO20240105003', amount: 45000, orderDate: '2024-01-05', status: 'pending', statusLabel: '待确认' }
    ]
  }
  detailVisible.value = true
}

const handleEdit = (record: any) => {
  modalTitle.value = '编辑供应商'
  Object.assign(formData, record)
  modalVisible.value = true
}

const handleProducts = (record: any) => {
  message.info('供应商产品功能开发中')
}

const handleOrders = (record: any) => {
  message.info('供应商订单功能开发中')
}

const handleEvaluate = (record: any) => {
  message.info('供应商评估功能开发中')
}

const handlePortal = (record: any) => {
  message.info('供应商门户功能开发中')
}

const handleContact = (record: any) => {
  message.info('联系记录功能开发中')
}

const handleBlacklist = (record: any) => {
  message.success('已加入黑名单')
  loadTableData()
}

const handleDelete = (record: any) => {
  message.success('删除成功')
  loadTableData()
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    message.success('保存成功')
    modalVisible.value = false
    loadTableData()
  } catch (error) {
    console.error('Validation failed:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleModalCancel = () => {
  formRef.value?.resetFields()
  modalVisible.value = false
}
</script>

<style scoped lang="scss">
.supplier-management {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    margin: 0;
  }
}

.search-form {
  margin-bottom: 16px;
}

.amount {
  color: #f5222d;
  font-weight: 500;

  &.payable {
    font-weight: 600;
  }
}
</style>