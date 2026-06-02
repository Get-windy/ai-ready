<template>
  <div class="tenant-management">
    <!-- 搜索区域 -->
    <a-card
      class="search-card"
      :bordered="false"
    >
      <a-form
        layout="inline"
        :model="searchForm"
        class="search-form"
      >
        <a-row
          :gutter="16"
          style="width: 100%"
        >
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="租户名称">
              <a-input
                v-model:value="searchForm.tenantName"
                placeholder="请输入租户名称"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="租户编码">
              <a-input
                v-model:value="searchForm.tenantCode"
                placeholder="请输入租户编码"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="状态">
              <a-select
                v-model:value="searchForm.status"
                placeholder="请选择状态"
                allow-clear
                style="width: 100%"
              >
                <a-select-option :value="0">
                  正常
                </a-select-option>
                <a-select-option :value="1">
                  停用
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item>
              <a-space>
                <a-button
                  type="primary"
                  @click="handleSearch"
                >
                  <template #icon>
                    <SearchOutlined />
                  </template>
                  搜索
                </a-button>
                <a-button @click="handleReset">
                  <template #icon>
                    <ReloadOutlined />
                  </template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card
      class="table-card"
      :bordered="false"
    >
      <template #title>
        <div class="table-header">
          <span class="title">租户列表</span>
          <a-space>
            <a-button
              type="primary"
              :loading="submittingLoading"
              @click="handleAdd"
            >
              <template #icon>
                <PlusOutlined />
              </template>
              新增租户
            </a-button>
            <a-button
              danger
              :loading="batchDeleteLoading"
              :disabled="!selectedRowKeys.length || batchDeleteLoading"
              @click="handleBatchDelete"
            >
              <template #icon>
                <DeleteOutlined />
              </template>
              批量删除
            </a-button>
          </a-space>
        </div>
      </template>

      <!-- 骨架屏加载状态 -->
      <SkeletonTable
        v-if="loading"
        :rows="5"
        :columns="7"
      />

      <!-- 数据表格 -->
      <a-table
        v-else
        :columns="columns"
        :data-source="tableData"
        :pagination="pagination"
        :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 0 ? 'success' : 'error'">
              {{ record.status === 0 ? '正常' : '停用' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'action'">
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
        </template>
      </a-table>
    </a-card>

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
import type { TableProps, FormInstance } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  DownOutlined,
  StopOutlined
} from '@ant-design/icons-vue'
import { SkeletonTable } from '@/components/Skeleton'
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
const columns: TableProps['columns'] = [
  { title: '租户编码', dataIndex: 'tenantCode', width: 150 },
  { title: '租户名称', dataIndex: 'tenantName', width: 200 },
  { title: '联系人', dataIndex: 'contactName' },
  { title: '联系电话', dataIndex: 'contactPhone', width: 130 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
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

// ── 表格操作 ──────────────────────────────────────────────
const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  fetchData()
}

const onSelectChange = (keys: (string | number)[]) => {
  selectedRowKeys.value = keys as number[]
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
const handleBatchDelete = () => {
  if (batchDeleteLoading.value) return

  const idsToDelete = [...selectedRowKeys.value]

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
  padding: 0;
}

.search-card {
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: -24px;
}

.table-card :deep(.ant-card-head) {
  border-bottom: none;
  padding-bottom: 0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.table-header .title {
  font-size: 16px;
  font-weight: 500;
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

@media (max-width: 768px) {
  .search-form :deep(.ant-form-item) {
    margin-bottom: 16px;
  }

  .table-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
