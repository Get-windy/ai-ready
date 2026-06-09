<template>
  <PageContainer full-height>
    <template #header>
      <div class="config-page-header">
        <div class="config-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统配置</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="config-page-header-title">系统配置</h2>
        </div>
        <div class="config-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="fetchData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="config-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">配置总数</div>
          </div>
          <SettingOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-groups">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ groupCount }}</div>
            <div class="stat-card-label">分组数量</div>
          </div>
          <FolderOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-sensitive">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ sensitiveCount }}</div>
            <div class="stat-card-label">敏感配置</div>
          </div>
          <LockOutlined class="stat-card-icon" />
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
        add-text="新增配置"
        @add="handleAdd"
        @edit="handleEdit"
        @delete="handleDeleteConfirm"
        @batch-delete="handleBatchDelete"
        @refresh="fetchData"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="(keys: any) => { selectedRowKeys.value = keys as number[] }"
      >
        <template #toolbar-actions>
          <a-button @click="handleRefreshCache">
            <template #icon><SyncOutlined /></template>
            刷新缓存
          </a-button>
        </template>

        <template #groupNameCell="{ record }">
          <a-tag color="blue">{{ record.groupName }}</a-tag>
        </template>
        <template #configValueCell="{ record }">
          <span
            class="config-value"
            :class="{ sensitive: isSensitiveKey(record.configKey) }"
          >
            {{ isSensitiveKey(record.configKey) ? '******' : record.configValue }}
          </span>
          <a-tooltip title="复制" v-if="!isSensitiveKey(record.configKey)">
            <a-button
              type="link"
              size="small"
              :style="{ padding: '0 4px' }"
              @click="handleCopy(record.configValue)"
            >
              <CopyOutlined />
            </a-button>
          </a-tooltip>
        </template>

        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">
              编辑
            </a-button>
            <a-button type="link" size="small" danger @click="handleDeleteConfirm(record)">
              删除
            </a-button>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 配置表单弹窗 -->
      <a-modal
        v-model:open="modalVisible"
        :title="modalTitle"
        :confirm-loading="modalLoading"
        width="550px"
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
          <a-form-item label="配置键" name="configKey">
            <a-input
              v-model:value="formState.configKey"
              placeholder="请输入配置键，如 sys.upload.path"
              :disabled="isEdit"
            />
          </a-form-item>
          <a-form-item label="配置值" name="configValue">
            <a-textarea
              v-model:value="formState.configValue"
              placeholder="请输入配置值"
              :rows="4"
            />
          </a-form-item>
          <a-form-item label="描述" name="description">
            <a-textarea
              v-model:value="formState.description"
              placeholder="请输入配置描述"
              :rows="2"
            />
          </a-form-item>
          <a-form-item label="分组" name="groupName">
            <a-input
              v-model:value="formState.groupName"
              placeholder="请输入分组名称，如 SYS/UPLOAD/EMAIL"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  SyncOutlined,
  ReloadOutlined,
  CopyOutlined,
  SettingOutlined,
  FolderOutlined,
  LockOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { configApi, type ConfigInfo } from '@/api/config'
import { PageContainer } from '@/components'

// 搜索表单
const searchForm = reactive({
  configKey: '',
  groupName: undefined as string | undefined
})

const groupOptions = ref<string[]>([])

// 表格数据
const tableData = ref<ConfigInfo[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 统计数据 ────────────────────────────────────────────
const groupCount = computed(() => new Set(tableData.value.map(c => c.groupName)).size)
const sensitiveCount = computed(() => tableData.value.filter(c => isSensitiveKey(c.configKey)).length)

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 表格列
const vxeColumns = computed(() => [
  { field: 'configKey', title: '配置键', width: 200, showOverflow: 'tooltip' },
  { field: 'configValue', title: '配置值', width: 300, showOverflow: 'tooltip', slotName: 'configValueCell' },
  { field: 'description', title: '描述', width: 200, showOverflow: 'tooltip' },
  { field: 'groupName', title: '分组', width: 120, slotName: 'groupNameCell' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { type: 'action', title: '操作', width: 140, fixed: 'right' }
])

// 筛选字段
const filterFields = computed<FilterField[]>(() => [
  { key: 'configKey', label: '配置键', type: 'input', placeholder: '请输入配置键' },
  { key: 'groupName', label: '分组', type: 'select', options: groupOptions.value.map(g => ({ label: g, value: g })) },
])

// 弹窗
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const modalTitle = computed(() => isEdit.value ? '编辑配置' : '新增配置')

const formState = reactive({
  id: 0,
  configKey: '',
  configValue: '',
  description: '',
  groupName: ''
})

const formRules = {
  configKey: { required: true, message: '请输入配置键', trigger: 'blur' },
  configValue: { required: true, message: '请输入配置值', trigger: 'blur' },
  groupName: { required: true, message: '请输入分组', trigger: 'blur' }
}

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const res = await configApi.getPage({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    console.warn('[系统配置] 加载配置数据失败')
    message.error('加载配置失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const fetchGroups = async () => {
  try {
    const res = await configApi.getGroups()
    if (res.data) {
      groupOptions.value = res.data
    }
  } catch (err) {
    console.warn('[系统管理] 加载分组列表失败', err)
    message.error('加载分组失败')
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, { configKey: '', groupName: undefined })
  handleSearch()
}

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { configKey: '', groupName: undefined })
  } else {
    Object.assign(searchForm, filters)
  }
  pagination.current = 1
  fetchData()
}

// 分页变化
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, { id: 0, configKey: '', configValue: '', description: '', groupName: '' })
  modalVisible.value = true
}

// 编辑
const handleEdit = (record: ConfigInfo) => {
  isEdit.value = true
  Object.assign(formState, {
    id: record.id,
    configKey: record.configKey,
    configValue: record.configValue,
    description: record.description,
    groupName: record.groupName
  })
  modalVisible.value = true
}

// 删除
const handleDeleteConfirm = (record: ConfigInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除配置 "${record.configKey}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await configApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (err) {
        console.warn('[系统管理] 删除配置失败', err)
        message.error('删除失败')
      }
    }
  })
}

// 批量删除
const handleBatchDelete = (deleteKeys?: number[]) => {
  const ids = deleteKeys || selectedRowKeys.value
  if (!ids.length) return
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${ids.length} 个配置吗？`,
    async onOk() {
      try {
        await configApi.batchDelete(ids)
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchData()
      } catch (err) {
        console.warn('[系统管理] 批量删除失败', err)
        message.error('批量删除失败')
      }
    }
  })
}

// 刷新缓存
const handleRefreshCache = async () => {
  try {
    await configApi.refreshCache()
    message.success('缓存刷新成功')
  } catch (err) {
    console.warn('[系统管理] 刷新缓存失败', err)
    message.error('刷新缓存失败')
  }
}

// 复制
const handleCopy = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

// 敏感键
const isSensitiveKey = (key: string) => {
  const sensitiveKeywords = ['password', 'secret', 'token', 'key', 'private']
  return sensitiveKeywords.some(k => key.toLowerCase().includes(k))
}

// 提交表单
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (isEdit.value) {
      await configApi.update(formState as any)
      message.success('更新成功')
    } else {
      await configApi.create(formState as any)
      message.success('创建成功')
    }

    modalVisible.value = false
    fetchData()
  } catch (error) {
    message.error('操作失败')
  } finally {
    modalLoading.value = false
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

onMounted(() => {
  fetchData()
  fetchGroups()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.config-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.config-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.config-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.config-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.config-management {
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
.stat-groups { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-sensitive { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

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


.config-value.sensitive {
  color: #999;
  font-style: italic;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
