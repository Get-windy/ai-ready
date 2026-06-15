<template>
  <ErrorBoundary @error="handleError">
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
          <a-button size="small" :loading="refreshLoading" v-permission="'system:config:query'" @click="handleRefresh">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          </span>
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

      <a-skeleton v-if="loading && tableDataSource.length === 0" active :paragraph="{ rows: 8 }" style="padding: 20px;" />
      <VxeTableList v-else
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :min-empty-rows="12"
        :filter-fields="filterFields"
        :show-search="false"
        :selectable="true"
        add-text="新增配置"
        add-permission="system:config:create"
        edit-permission="system:config:update"
        delete-permission="system:config:delete"
        @add="handleAdd"
        @edit="handleEdit"
        @delete="handleDeleteConfirm"
        @batch-delete="handleBatchDelete"
        @refresh="debounceClick('refresh', fetchData)"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="(keys: any) => { (selectedRowKeys as any) = keys }"
        @cell-dblclick="handleView"
      >
        <template #toolbar-actions>
          <a-button @click="handleRefreshCache">
            <template #icon><SyncOutlined /></template>
            刷新缓存
          </a-button>
        </template>

        <template #empty>
          <div class="empty-state-wrapper">
            <a-empty v-if="!hasError" description="暂无配置数据">
              <template #image>
                <SettingOutlined style="font-size: 48px; color: #d9d9d9;" />
              </template>
              <a-button type="primary" size="small" v-permission="'system:config:create'" @click="handleAdd">
                <template #icon><PlusOutlined /></template>
                新增第一个配置
              </a-button>
            </a-empty>
            <a-result v-else status="error" title="数据加载失败">
              <template #extra>
                <a-button type="primary" @click="debounceClick('refresh', fetchData)()">
                  <template #icon><ReloadOutlined /></template>
                  重新加载
                </a-button>
              </template>
            </a-result>
          </div>
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
            <a-button type="link" size="small" v-permission="'system:config:update'" @click="handleEdit(record)">
              编辑
            </a-button>
            <a-button type="link" size="small" danger v-permission="'system:config:delete'" @click="handleDeleteConfirm(record)">
              删除
            </a-button>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 配置表单弹窗 -->
      <FullScreenDetail
        :visible="modalVisible"
        :title="modalTitle"
        :save-loading="modalLoading"
        :show-save-and-new="!isEdit"
        :dirty="formDirty"
        @save="handleModalOk"
        @close="handleFormClose"
        @save-and-new="handleFormSaveAndNew"
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
      </FullScreenDetail>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  WarningOutlined,
  SyncOutlined,
  ReloadOutlined,
  CopyOutlined,
  PlusOutlined,
  SettingOutlined,
  FolderOutlined,
  LockOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { configApi, type ConfigInfo } from '@/api/config'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'

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
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

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

// ── 表单脏检测 ──────────────────────────────────────────
const initialFormSnapshot = ref('')
let watchReady = false
const formDirty = computed(() => {
  if (!watchReady) return false
  return JSON.stringify(formState) !== initialFormSnapshot.value
})
function saveFormSnapshot() { initialFormSnapshot.value = JSON.stringify(formState) }

// ── 离开守卫 ────────────────────────────────────────────
onBeforeRouteLeave((to, from, next) => {
  if (!formDirty.value) { next(); return }
  Modal.confirm({
    title: '确认离开',
    content: '您有未保存的修改，确定要离开吗？',
    okText: '离开',
    cancelText: '继续编辑',
    onOk: () => { watchReady = false; next() },
    onCancel: () => next(false),
  })
})

const formRules: any = {
  configKey: { required: true, message: '请输入配置键', trigger: 'blur' },
  configValue: { required: true, message: '请输入配置值', trigger: 'blur' },
  groupName: { required: true, message: '请输入分组', trigger: 'blur' }
}

// ── 刷新 ──────────────────────────────────────────────
const handleRefresh = () => {
  refreshLoading.value = true
  fetchData()
}

// 数据加载
const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await configApi.getPage({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.records
      pagination.total = res.total
    }
  } catch (error) {
    hasError.value = true
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
  nextTick(() => { saveFormSnapshot(); watchReady = true })
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
  nextTick(() => { saveFormSnapshot(); watchReady = true })
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
const isSensitiveKey = (key: string | undefined | null): boolean => {
  if (!key) return false
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

const handleFormClose = () => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { modalVisible.value = false; watchReady = false; formRef.value?.resetFields() },
    })
  } else {
    modalVisible.value = false
    watchReady = false
    formRef.value?.resetFields()
  }
}

const handleFormSaveAndNew = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true
    await configApi.create(formState as any)
    message.success('创建成功')
    fetchData()
    handleAdd()
  } catch (error: any) {
    if (error) message.error(error?.message || '创建失败')
  } finally {
    modalLoading.value = false
  }
}

function handleView(record: any) {
  handleEdit(record)
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', fetchData)()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput) {
    e.preventDefault()
    handleAdd()
  }
}

onMounted(() => {
  fetchData()
  fetchGroups()
  document.addEventListener('keydown', handleKeydown)
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
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
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

.config-management > :deep(.vxe-table-list-container) {
  flex: 1;
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

/* ── FullScreenDetail 内部紧凑样式 ────────────────────── */
:deep(.fsd-body .ant-form-item) { margin-bottom: 8px; }
:deep(.fsd-body .ant-form-item-label > label) { font-size: 12px; height: 28px; }
:deep(.fsd-body .ant-input), :deep(.fsd-body .ant-input-number), :deep(.fsd-body .ant-select), :deep(.fsd-body .ant-picker), :deep(.fsd-body .ant-cascader-picker) { font-size: 12px; }
:deep(.fsd-body .ant-input-number-input) { font-size: 12px; }
:deep(.fsd-body .ant-select-selection-item) { font-size: 12px; }
:deep(.fsd-body .ant-btn) { font-size: 12px; }

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 空状态 ──────────────────────────── */
.empty-state-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  width: 100%;
}

/* ── vxe-table 表头 2px 粗边框 ──────────────────── */
.config-management :deep(.vxe-table-list-container .vxe-header--row .vxe-header--column) {
  border-bottom: 2px solid #d0d5dd !important;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
