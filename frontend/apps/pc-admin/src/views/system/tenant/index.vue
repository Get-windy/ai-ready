<template>
  <div class="tenant-management">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">租户总数</div>
        </div>
        <ShopOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-active">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ activeCount }}</div>
          <div class="stat-card-label">正常租户</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-disabled">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ disabledCount }}</div>
          <div class="stat-card-label">停用租户</div>
        </div>
        <StopOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-users">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ totalUsers }}</div>
          <div class="stat-card-label">用户总数</div>
        </div>
        <TeamOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :show-search="false"
      :selectable="true"
      add-text="新增租户"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @batch-delete="handleBatchDelete"
      @refresh="fetchData"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="(keys: any) => { selectedRowKeys.value = keys as number[] }"
    >
      <template #statusCell="{ record }">
        <a-tag :color="record.status === 0 ? 'success' : 'error'">
          {{ record.status === 0 ? '正常' : '停用' }}
        </a-tag>
      </template>

      <template #action="{ record }">
        <a-space>
          <a-button
            type="link"
            size="small"
            @click="handleEdit(record)"
          >
            编辑
          </a-button>
          <a-button
            type="link"
            size="small"
            @click="handleConfig(record)"
          >
            配置
          </a-button>
          <a-dropdown>
            <a-button
              type="link"
              size="small"
            >
              更多<DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="handleToggleStatus(record)">
                  <StopOutlined /> {{ record.status === 0 ? '停用' : '启用' }}
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item
                  danger
                  @click="handleDelete(record)"
                >
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 新增/编辑租户弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="submittingLoading"
      width="700px"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="租户编码"
              name="tenantCode"
            >
              <a-input
                v-model:value="formState.tenantCode"
                placeholder="请输入租户编码"
                :disabled="isEdit"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="租户名称"
              name="tenantName"
            >
              <a-input
                v-model:value="formState.tenantName"
                placeholder="请输入租户名称"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="联系人"
              name="contactName"
            >
              <a-input
                v-model:value="formState.contactName"
                placeholder="请输入联系人"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="联系电话"
              name="contactPhone"
            >
              <a-input
                v-model:value="formState.contactPhone"
                placeholder="请输入联系电话"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="联系邮箱"
              name="contactEmail"
            >
              <a-input
                v-model:value="formState.contactEmail"
                placeholder="请输入联系邮箱"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="最大用户数"
              name="maxUsers"
            >
              <a-input-number
                v-model:value="formState.maxUsers"
                placeholder="请输入最大用户数"
                :min="1"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item
          label="地址"
          name="address"
        >
          <a-input
            v-model:value="formState.address"
            placeholder="请输入地址"
          />
        </a-form-item>
        <a-form-item
          label="描述"
          name="description"
        >
          <a-textarea
            v-model:value="formState.description"
            placeholder="请输入描述信息"
            :rows="3"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="过期日期"
              name="expireDate"
            >
              <a-date-picker
                v-model:value="formState.expireDate"
                placeholder="请选择过期日期"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="状态"
              name="status"
            >
              <a-radio-group v-model:value="formState.status">
                <a-radio :value="0">
                  正常
                </a-radio>
                <a-radio :value="1">
                  停用
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 配置弹窗 -->
    <a-modal
      v-model:open="configModalVisible"
      title="租户配置"
      :confirm-loading="configLoading"
      width="600px"
      @ok="handleConfigSave"
      @cancel="configModalVisible = false"
    >
      <a-form
        :model="configForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="Logo">
          <a-input
            v-model:value="configForm.logo"
            placeholder="请输入 Logo 地址"
          />
        </a-form-item>
        <a-form-item label="主题色">
          <a-input
            v-model:value="configForm.themeColor"
            placeholder="请输入主题色（如 #1890ff）"
          />
          <div
            v-if="configForm.themeColor"
            class="color-preview"
            :style="{ backgroundColor: configForm.themeColor }"
          />
        </a-form-item>
        <a-form-item label="最大用户数">
          <a-input-number
            v-model:value="configForm.maxUsers"
            placeholder="请输入最大用户数"
            :min="1"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="过期日期">
          <a-date-picker
            v-model:value="configForm.expireDate"
            placeholder="请选择过期日期"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
      <a-divider />
      <a-descriptions
        title="当前配置信息"
        :column="1"
        bordered
        size="small"
      >
        <a-descriptions-item label="租户编码">
          {{ currentConfigTenant?.tenantCode }}
        </a-descriptions-item>
        <a-descriptions-item label="租户名称">
          {{ currentConfigTenant?.tenantName }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="currentConfigTenant?.status === 0 ? 'success' : 'error'">
            {{ currentConfigTenant?.status === 0 ? '正常' : '停用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ currentConfigTenant?.createTime }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  DownOutlined,
  StopOutlined,
  DeleteOutlined,
  ShopOutlined,
  CheckCircleOutlined,
  TeamOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { tenantApi, type TenantInfo } from '@/api/tenant'
import { useSubmitLock } from '@/composables'

// ── 搜索表单 ──────────────────────────────────────────────
const searchForm = reactive({
  tenantName: '',
  tenantCode: '',
  status: undefined as number | undefined
})

// ── 表格数据 ──────────────────────────────────────────────
const tableData = ref<TenantInfo[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])
const { isSubmitting: submittingLoading, withSubmitLock } = useSubmitLock()
const { isSubmitting: batchDeleteLoading } = useSubmitLock()

// ── 统计数据 ────────────────────────────────────────────
const activeCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const disabledCount = computed(() => tableData.value.filter(r => r.status === 1).length)
const totalUsers = computed(() => tableData.value.reduce((s, r) => s + (r.maxUsers || 0), 0))

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

// ── 分页配置 ──────────────────────────────────────────────
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// ── 表格列定义 ────────────────────────────────────────────
const vxeColumns = computed(() => [
  { field: 'tenantCode', title: '租户编码', width: 150 },
  { field: 'tenantName', title: '租户名称', width: 200 },
  { field: 'contactName', title: '联系人' },
  { field: 'contactPhone', title: '联系电话', width: 130 },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { field: 'createTime', title: '创建时间', width: 170 },
  { type: 'action', title: '操作', width: 180, fixed: 'right' }
])

// ── 筛选字段 ──────────────────────────────────────────────
const filterFields: FilterField[] = [
  { key: 'tenantName', label: '租户名称', type: 'input', placeholder: '请输入租户名称' },
  { key: 'tenantCode', label: '租户编码', type: 'input', placeholder: '请输入租户编码' },
  { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: 0 }, { label: '停用', value: 1 }] },
]

// ── 新增/编辑弹窗 ─────────────────────────────────────────
const modalVisible = ref(false)
const modalTitle = computed(() => isEdit.value ? '编辑租户' : '新增租户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({
  id: 0,
  tenantCode: '',
  tenantName: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  description: '',
  status: 0,
  maxUsers: undefined as number | undefined,
  expireDate: undefined as string | undefined
})

const formRules = {
  tenantCode: [
    { required: true, message: '请输入租户编码', trigger: 'blur' },
    { min: 2, max: 20, message: '租户编码长度为 2-20 个字符', trigger: 'blur' }
  ],
  tenantName: [
    { required: true, message: '请输入租户名称', trigger: 'blur' }
  ],
  contactEmail: [
    { type: 'email', message: '请输入有效邮箱地址', trigger: 'blur' }
  ],
  contactPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号', trigger: 'blur' }
  ]
}

// ── 配置弹窗 ──────────────────────────────────────────────
const configModalVisible = ref(false)
const configLoading = ref(false)
const currentConfigTenant = ref<TenantInfo | null>(null)
const configForm = reactive({
  logo: '',
  themeColor: '',
  maxUsers: undefined as number | undefined,
  expireDate: undefined as string | undefined
})

// ── 数据加载 ──────────────────────────────────────────────
const fetchData = async () => {
  loading.value = true
  try {
    const res = await tenantApi.getPage({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ── 搜索 ──────────────────────────────────────────────────
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, { tenantName: '', tenantCode: '', status: undefined })
  handleSearch()
}

// ── 筛选变化 ──────────────────────────────────────────────
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { tenantName: '', tenantCode: '', status: undefined })
  } else {
    Object.assign(searchForm, filters)
  }
  pagination.current = 1
  fetchData()
}

// ── 分页变化 ──────────────────────────────────────────────
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ── 新增 ──────────────────────────────────────────────────
const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, {
    id: 0,
    tenantCode: '',
    tenantName: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    address: '',
    description: '',
    status: 0,
    maxUsers: undefined,
    expireDate: undefined
  })
  modalVisible.value = true
}

// ── 编辑 ──────────────────────────────────────────────────
const handleEdit = (record: TenantInfo) => {
  isEdit.value = true
  Object.assign(formState, {
    id: record.id,
    tenantCode: record.tenantCode,
    tenantName: record.tenantName,
    contactName: record.contactName || '',
    contactPhone: record.contactPhone || '',
    contactEmail: record.contactEmail || '',
    address: record.address || '',
    description: record.description || '',
    status: record.status,
    maxUsers: record.maxUsers,
    expireDate: record.expireDate || undefined
  })
  modalVisible.value = true
}

// ── 提交表单 ──────────────────────────────────────────────
const handleModalOk = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()

      if (isEdit.value) {
        await tenantApi.update(formState.id, formState)
        message.success('更新成功')
      } else {
        await tenantApi.create(formState as any)
        message.success('创建成功')
      }

      modalVisible.value = false
      fetchData()
    })
    // result 为 undefined 表示操作被锁定（防重复提交）
    void result
  } catch (error: any) {
    if (error) {
      message.error(error?.message || '操作失败')
    }
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// ── 删除 ──────────────────────────────────────────────────
const handleDelete = (record: TenantInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除租户 "${record.tenantName}" 吗？`,
    onOk: async () => {
      try {
        await tenantApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        message.error(error?.message || '删除失败')
      }
    }
  })
}

// ── 批量删除 ──────────────────────────────────────────────
const handleBatchDelete = (deleteKeys?: number[]) => {
  if (batchDeleteLoading.value) return

  const idsToDelete = deleteKeys || [...selectedRowKeys.value]

  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${idsToDelete.length} 个租户吗？`,
    onOk: async () => {
      batchDeleteLoading.value = true
      try {
        await tenantApi.batchDelete(idsToDelete)
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchData()
      } catch (error: any) {
        message.error(error?.message || '批量删除失败')
      } finally {
        batchDeleteLoading.value = false
      }
    }
  })
}

// ── 启停状态 ──────────────────────────────────────────────
const handleToggleStatus = async (record: TenantInfo) => {
  const newStatus = record.status === 0 ? 1 : 0
  try {
    await tenantApi.updateStatus(record.id, newStatus)
    record.status = newStatus
    message.success(newStatus === 0 ? '已启用' : '已停用')
  } catch (error: any) {
    message.error(error?.message || '操作失败')
  }
}

// ── 配置 ──────────────────────────────────────────────────
const handleConfig = async (record: TenantInfo) => {
  currentConfigTenant.value = record
  try {
    const res = await tenantApi.getConfig(record.id)
    if (res.data) {
      Object.assign(configForm, {
        logo: res.data.logo || '',
        themeColor: res.data.themeColor || '',
        maxUsers: res.data.maxUsers,
        expireDate: res.data.expireDate || undefined
      })
    }
  } catch {
    // 配置不存在時使用默認值
    Object.assign(configForm, {
      logo: '',
      themeColor: '',
      maxUsers: undefined,
      expireDate: undefined
    })
  }
  configModalVisible.value = true
}

const handleConfigSave = async () => {
  if (!currentConfigTenant.value) return
  configLoading.value = true
  try {
    await tenantApi.updateConfig(currentConfigTenant.value.id, configForm)
    message.success('配置保存成功')
    configModalVisible.value = false
  } catch (error: any) {
    message.error(error?.message || '配置保存失败')
  } finally {
    configLoading.value = false
  }
}

// ── 初始加载 ──────────────────────────────────────────────
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.tenant-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-active { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-disabled { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-users { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

.color-preview {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  margin-left: 8px;
  vertical-align: middle;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
