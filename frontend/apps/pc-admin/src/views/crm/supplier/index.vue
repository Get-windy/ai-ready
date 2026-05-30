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

    <!-- 供应商产品列表弹窗 -->
    <a-modal
      v-model:open="productsModalVisible"
      :title="productsModalTitle"
      width="900px"
      :footer="null"
    >
      <a-table
        :columns="productsColumns"
        :data-source="productsData"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'price'">
            <span class="amount">¥{{ formatAmount(record.price) }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === '正常供应' ? 'green' : 'orange'">{{ record.status }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 供应商评估弹窗 -->
    <a-modal
      v-model:open="evaluateModalVisible"
      title="供应商评估"
      width="600px"
      @ok="handleEvaluateSubmit"
    >
      <a-form :model="evaluateForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="供应商名称">
          <a-input :value="evaluateForm.supplierName" disabled />
        </a-form-item>
        <a-form-item label="质量评分" required>
          <a-rate v-model:value="evaluateForm.qualityScore" :count="5" style="vertical-align: middle" />
          <span style="margin-left: 8px; color: #999">{{ evaluateForm.qualityScore }} 分</span>
        </a-form-item>
        <a-form-item label="交货评分" required>
          <a-rate v-model:value="evaluateForm.deliveryScore" :count="5" style="vertical-align: middle" />
          <span style="margin-left: 8px; color: #999">{{ evaluateForm.deliveryScore }} 分</span>
        </a-form-item>
        <a-form-item label="服务评分" required>
          <a-rate v-model:value="evaluateForm.serviceScore" :count="5" style="vertical-align: middle" />
          <span style="margin-left: 8px; color: #999">{{ evaluateForm.serviceScore }} 分</span>
        </a-form-item>
        <a-form-item label="评价内容">
          <a-textarea v-model:value="evaluateForm.comment" placeholder="请输入评价内容" :rows="4" />
        </a-form-item>
        <a-form-item label="评估日期">
          <a-date-picker v-model:value="evaluateForm.evaluateDate" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 供应商门户弹窗 -->
    <a-modal
      v-model:open="portalModalVisible"
      :title="`供应商门户 - ${portalInfo.supplierName}`"
      width="550px"
      :footer="null"
    >
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="门户地址">
          <a-space>
            <span>{{ portalInfo.portalUrl }}</span>
            <a-button type="link" size="small" @click="handleCopyPortalUrl">
              <template #icon><CopyOutlined /></template>
              复制
            </a-button>
          </a-space>
        </a-descriptions-item>
        <a-descriptions-item label="登录账号">{{ portalInfo.account }}</a-descriptions-item>
        <a-descriptions-item label="登录密码">{{ portalInfo.password }}</a-descriptions-item>
      </a-descriptions>
      <a-divider />
      <a-space style="width: 100%; justify-content: flex-end">
        <a-button @click="handleResetPortalPassword">重置密码</a-button>
        <a-button type="primary" @click="handleOpenPortal">打开门户</a-button>
      </a-space>
    </a-modal>

    <!-- 供应商联系人弹窗 -->
    <a-modal
      v-model:open="contactsModalVisible"
      :title="contactsModalTitle"
      width="700px"
      :footer="null"
    >
      <a-table
        :columns="contactsColumns"
        :data-source="contactsData"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'isPrimary'">
            <a-tag :color="record.isPrimary === '是' ? 'blue' : 'default'">{{ record.isPrimary }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DownOutlined, CopyOutlined } from '@ant-design/icons-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'
import { useRouter } from 'vue-router'

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
const router = useRouter()

// ── 产品弹窗 ──────────────────────────────────────────
const productsModalVisible = ref(false)
const productsModalTitle = ref('')
const productsData = ref<any[]>([])
const productsColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 150 },
  { title: '规格型号', dataIndex: 'spec', width: 120 },
  { title: '单价', dataIndex: 'price', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 80 },
  { title: '供应状态', key: 'status', dataIndex: 'status', width: 100 }
]

// ── 评估弹窗 ──────────────────────────────────────────
const evaluateModalVisible = ref(false)
const evaluateForm = reactive({
  supplierId: 0,
  supplierName: '',
  qualityScore: 80,
  deliveryScore: 80,
  serviceScore: 80,
  comment: '',
  evaluateDate: undefined as any
})

// ── 门户弹窗 ──────────────────────────────────────────
const portalModalVisible = ref(false)
const portalInfo = reactive({
  supplierName: '',
  portalUrl: '',
  account: '',
  password: ''
})

// ── 联系人弹窗 ────────────────────────────────────────
const contactsModalVisible = ref(false)
const contactsModalTitle = ref('')
const contactsData = ref<any[]>([])
const contactsColumns = [
  { title: '姓名', dataIndex: 'name', width: 100 },
  { title: '职位', dataIndex: 'role', width: 120 },
  { title: '电话', dataIndex: 'phone', width: 120 },
  { title: '邮箱', dataIndex: 'email', width: 180 },
  { title: '是否主要联系人', key: 'isPrimary', dataIndex: 'isPrimary', width: 120 }
]

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
  productsModalTitle.value = `供应商产品 - ${record.name}`
  productsData.value = [
    { id: 1, productCode: 'MAT001', productName: '钢板 Q235', spec: '10mm×2000mm×6000mm', price: 3850, unit: '吨', status: '正常供应' },
    { id: 2, productCode: 'MAT002', productName: '角钢 50×50×5', spec: 'Q235B 6m/根', price: 28.5, unit: '米', status: '正常供应' },
    { id: 3, productCode: 'MAT003', productName: '不锈钢板 304', spec: '1.5mm×1219mm×2438mm', price: 158, unit: '张', status: '正常供应' },
    { id: 4, productCode: 'MAT004', productName: '铝板 6061', spec: '2mm×1220mm×2440mm', price: 220, unit: '张', status: '限量供应' }
  ]
  productsModalVisible.value = true
}

const handleOrders = (record: any) => {
  router.push({ path: '/purchase/order', query: { supplierId: record.id, supplierName: record.name } })
}

const handleEvaluate = (record: any) => {
  evaluateForm.supplierId = record.id
  evaluateForm.supplierName = record.name
  evaluateForm.qualityScore = 80
  evaluateForm.deliveryScore = 80
  evaluateForm.serviceScore = 80
  evaluateForm.comment = ''
  evaluateForm.evaluateDate = undefined
  evaluateModalVisible.value = true
}

const handlePortal = (record: any) => {
  portalInfo.supplierName = record.name
  portalInfo.portalUrl = `https://portal.example.com/supplier/${record.code || record.id}`
  portalInfo.account = `supplier_${record.code || record.id}`
  portalInfo.password = '******'
  portalModalVisible.value = true
}

const handleContact = (record: any) => {
  contactsModalTitle.value = `联系人列表 - ${record.name}`
  contactsData.value = [
    { id: 1, name: record.contact || '张经理', role: '采购负责人', phone: record.phone || '138****1234', email: 'zhang@example.com', isPrimary: '是' },
    { id: 2, name: '李主管', role: '技术负责人', phone: '139****5678', email: 'li@example.com', isPrimary: '否' },
    { id: 3, name: '王专员', role: '售后服务', phone: '137****9012', email: 'wang@example.com', isPrimary: '否' }
  ]
  contactsModalVisible.value = true
}

const handleBlacklist = (record: any) => {
  message.success('已加入黑名单')
  loadTableData()
}

const handleDelete = (record: any) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除供应商"${record.name}"吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      message.success('删除成功')
      loadTableData()
    }
  })
}

const handleEvaluateSubmit = () => {
  if (!evaluateForm.comment) {
    message.warning('请输入评价内容')
    return
  }
  const avgScore = Math.round((evaluateForm.qualityScore + evaluateForm.deliveryScore + evaluateForm.serviceScore) / 3)
  message.success(`评估提交成功！综合评分：${avgScore} 分`)
  evaluateModalVisible.value = false
}

const handleCopyPortalUrl = () => {
  navigator.clipboard.writeText(portalInfo.portalUrl).then(() => {
    message.success('门户地址已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败，请手动复制')
  })
}

const handleResetPortalPassword = () => {
  Modal.confirm({
    title: '重置密码',
    content: `确定要重置供应商"${portalInfo.supplierName}"的门户密码吗？`,
    okText: '确认重置',
    cancelText: '取消',
    centered: true,
    onOk() {
      portalInfo.password = '******'
      message.success('密码已重置')
    }
  })
}

const handleOpenPortal = () => {
  window.open(portalInfo.portalUrl, '_blank')
  message.success('正在打开供应商门户')
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