<template>
  <PageContainer full-height>
    <template #header>
      <div class="client-page-header">
        <div class="client-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>打印管理</a-breadcrumb-item>
            <a-breadcrumb-item>客户端管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="client-page-header-title">客户端管理</h2>
        </div>
        <div class="client-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <a-button size="small" :loading="loading" @click="debounceClick('refresh', fetchData)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="client-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">客户端总数</div>
          </div>
          <DesktopOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-online">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ onlineCount }}</div>
            <div class="stat-card-label">在线</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-offline">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ offlineCount }}</div>
            <div class="stat-card-label">离线</div>
          </div>
          <MinusCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-disabled">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ disabledCount }}</div>
            <div class="stat-card-label">已禁用</div>
          </div>
          <StopOutlined class="stat-card-icon" />
        </div>
      </div>

      <!-- 客户端列表 -->
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'clientId'"
        :filter-fields="filterFields"
        :show-search="false"
        :selectable="true"
        add-text="注册客户端"
        @add="handleAdd"
        @delete="handleDeleteConfirm"
        @batch-delete="handleBatchDelete"
        @refresh="debounceClick('refresh', fetchData)"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="(_rows: any, ids: any) => { selectedRowKeys.value = ids as number[] }"
      >
        <template #statusCell="{ record }">
          <a-tag :color="getStatusColor(record.status)">
            <span class="status-dot" :class="`status-dot--${(record.status || '').toLowerCase()}`"></span>
            {{ getStatusLabel(record.status) }}
          </a-tag>
        </template>

        <template #lastHeartbeatCell="{ record }">
          <span :title="record.lastHeartbeat || '-'">{{ formatRelativeTime(record.lastHeartbeat) }}</span>
        </template>

        <template #clientCodeCell="{ record }">
          <span class="code-text">{{ record.clientCode }}</span>
          <a-tooltip title="复制客户端编码">
            <a-button
              type="link"
              size="small"
              class="copy-btn"
              @click="handleCopy(record.clientCode)"
            >
              <CopyOutlined />
            </a-button>
          </a-tooltip>
        </template>

        <template #authKeyCell="{ record }">
          <span class="code-text masked">******</span>
          <a-tooltip title="复制认证密钥">
            <a-button
              type="link"
              size="small"
              class="copy-btn"
              @click="handleCopy(record.authKey)"
            >
              <CopyOutlined />
            </a-button>
          </a-tooltip>
        </template>

        <template #empty>
          <a-empty v-if="!hasError" description="暂无客户端数据" />
          <a-result v-else status="error" title="数据加载失败">
            <template #extra>
              <a-button type="primary" @click="debounceClick('refresh', fetchData)()">
                <template #icon><ReloadOutlined /></template>
                重新加载
              </a-button>
            </template>
          </a-result>
        </template>

        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="handleResetKey(record)">
              重置密钥
            </a-button>
            <a-button type="link" size="small" danger @click="handleDeleteConfirm(record)">
              删除
            </a-button>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 注册客户端弹窗 -->
      <FullScreenDetail
        :visible="modalVisible"
        :title="'注册客户端'"
        :save-loading="modalLoading"
        :show-save-and-new="true"
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
          <a-form-item label="客户端名称" name="clientName">
            <a-input
              v-model:value="formState.clientName"
              placeholder="请输入客户端名称"
            />
          </a-form-item>
          <a-form-item label="客户端版本" name="clientVersion">
            <a-input
              v-model:value="formState.clientVersion"
              placeholder="可选，如 1.0.0"
            />
          </a-form-item>
        </a-form>
      </FullScreenDetail>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, nextTick, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  ReloadOutlined,
  CopyOutlined,
  DesktopOutlined,
  CheckCircleOutlined,
  MinusCircleOutlined,
  StopOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { printingApi, type PrintClientVO } from '@/api/printing'
import { PageContainer, FullScreenDetail } from '@/components'

// ── 表格数据 ────────────────────────────────────────────
const tableData = ref<PrintClientVO[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])
const lastUpdateTime = ref('')
const hasError = ref(false)

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, number>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key) && Date.now() - clickLocks.get(key)! < 5000) return
    clickLocks.set(key, Date.now())
    try {
      const result = fn(...args)
      if (result instanceof Promise) {
        result.finally(() => clickLocks.delete(key))
        setTimeout(() => clickLocks.delete(key), 5000)
      } else {
        setTimeout(() => clickLocks.delete(key), 300)
      }
    } catch {
      setTimeout(() => clickLocks.delete(key), 300)
    }
  }
}

// ── 统计数据 ────────────────────────────────────────────
const onlineCount = computed(() => tableData.value.filter(c => c.status === 'ONLINE').length)
const offlineCount = computed(() => tableData.value.filter(c => c.status === 'OFFLINE').length)
const disabledCount = computed(() => tableData.value.filter(c => c.status === 'DISABLED').length)

// ── 数据源 ──────────────────────────────────────────────
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

// 筛选状态
const filterValues = reactive<{ status?: string }>({
  status: undefined
})

// ── 表格列定义 ──────────────────────────────────────────
const vxeColumns = computed(() => [
  { field: 'clientName', title: '客户端名称', width: 150, showOverflow: 'tooltip' },
  { field: 'clientCode', title: '客户端编码', width: 180, slotName: 'clientCodeCell' },
  { field: 'status', title: '状态', width: 90, slotName: 'statusCell' },
  { field: 'lastHeartbeat', title: '最后心跳', width: 150, slotName: 'lastHeartbeatCell' },
  { field: 'clientIp', title: 'IP地址', width: 140 },
  { field: 'clientVersion', title: '版本', width: 90 },
  { field: 'defaultPrinter', title: '默认打印机', width: 150, showOverflow: 'tooltip' },
  { field: 'authKey', title: '认证密钥', width: 140, slotName: 'authKeyCell' },
  { field: 'createdAt', title: '创建时间', width: 160 },
  { type: 'action', title: '操作', width: 180, fixed: 'right' }
])

// ── 筛选字段 ────────────────────────────────────────────
const filterFields = computed<FilterField[]>(() => [
  {
    key: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '全部', value: undefined },
      { label: '在线', value: 'ONLINE' },
      { label: '离线', value: 'OFFLINE' },
      { label: '已禁用', value: 'DISABLED' }
    ]
  }
])

// ── 状态工具函数 ────────────────────────────────────────
function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    ONLINE: 'green',
    OFFLINE: 'default',
    DISABLED: 'red'
  }
  return map[status] || 'default'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    ONLINE: '在线',
    OFFLINE: '离线',
    DISABLED: '已禁用'
  }
  return map[status] || status || '-'
}

// ── 相对时间格式化 ──────────────────────────────────────
function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return '-'
  const now = Date.now()
  const date = new Date(dateStr).getTime()
  const diff = now - date

  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`

  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`

  const months = Math.floor(days / 30)
  if (months < 12) return `${months}个月前`

  return `${Math.floor(months / 12)}年前`
}

// ── 注册表单 ────────────────────────────────────────────
const modalVisible = ref(false)
const modalLoading = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({
  clientName: '',
  clientVersion: ''
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
    onOk: () => next(),
    onCancel: () => next(false),
  })
})

// 表单校验规则
const formRules = {
  clientName: { required: true, message: '请输入客户端名称', trigger: 'blur' }
}

// ── 数据加载 ────────────────────────────────────────────
const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const params: Record<string, any> = {
      page: pagination.current,
      size: pagination.pageSize
    }
    if (filterValues.status) {
      params.status = filterValues.status
    }
    const res = await printingApi.getClients(params)
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[客户端管理] 加载客户端数据失败')
    message.error('加载客户端失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

// ── 筛选变化 ────────────────────────────────────────────
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    filterValues.status = undefined
  } else {
    filterValues.status = filters.status || undefined
  }
  pagination.current = 1
  fetchData()
}

// ── 分页变化 ────────────────────────────────────────────
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ── 注册新增 ────────────────────────────────────────────
const handleAdd = () => {
  Object.assign(formState, { clientName: '', clientVersion: '' })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

// ── 删除客户端 ──────────────────────────────────────────
const handleDeleteConfirm = (record: PrintClientVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除客户端 "${record.clientName}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await printingApi.deleteClient(record.clientId)
        message.success('删除成功')
        fetchData()
      } catch (err) {
        console.warn('[客户端管理] 删除客户端失败', err)
        message.error('删除失败')
      }
    }
  })
}

// ── 批量删除 ────────────────────────────────────────────
const handleBatchDelete = (deleteKeys?: number[]) => {
  const ids = deleteKeys || selectedRowKeys.value
  if (!ids.length) return
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${ids.length} 个客户端吗？`,
    async onOk() {
      try {
        await Promise.all(ids.map((id: number) => printingApi.deleteClient(id)))
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchData()
      } catch (err) {
        console.warn('[客户端管理] 批量删除失败', err)
        message.error('批量删除失败')
      }
    }
  })
}

// ── 重置密钥 ────────────────────────────────────────────
const handleResetKey = (record: PrintClientVO) => {
  Modal.confirm({
    title: '确认重置密钥',
    content: `确定要重置客户端 "${record.clientName}" 的认证密钥吗？重置后旧密钥将立即失效。`,
    okText: '确认重置',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        const res = await printingApi.resetClientKey(record.clientId)
        const newKey = res.data?.authKey
        if (newKey) {
          Modal.success({
            title: '密钥重置成功',
            content: h('div', [
              h('p', { style: { marginBottom: '8px' } }, '新的认证密钥为：'),
              h('div', {
                style: {
                  padding: '10px 14px',
                  background: '#f5f5f5',
                  borderRadius: '4px',
                  fontFamily: "'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace",
                  fontSize: '14px',
                  wordBreak: 'break-all',
                  marginTop: '8px'
                }
              }, newKey),
              h('p', {
                style: { marginTop: '14px', color: '#999', fontSize: '12px' }
              }, '请妥善保管此密钥，关闭后将无法再次查看。')
            ]),
            okText: '已复制，关闭',
            onOk() {
              navigator.clipboard.writeText(newKey).catch(() => {})
            }
          })
        }
        fetchData()
      } catch (err) {
        console.warn('[客户端管理] 重置密钥失败', err)
        message.error('重置密钥失败')
      }
    }
  })
}

// ── 复制到剪贴板 ────────────────────────────────────────
const handleCopy = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

// ── 提交注册表单 ────────────────────────────────────────
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    const data: {
      clientName: string
      clientVersion?: string
    } = {
      clientName: formState.clientName
    }
    if (formState.clientVersion) {
      data.clientVersion = formState.clientVersion
    }

    await printingApi.registerClient(data)
    modalVisible.value = false
    message.success('注册成功')
    fetchData()
  } catch (error) {
    if (error && (error as any).errorFields) {
      // 表单校验未通过，不弹错误提示
      return
    }
    message.error('注册失败')
  } finally {
    modalLoading.value = false
  }
}

// ── 弹窗关闭 ────────────────────────────────────────────
const handleFormClose = () => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { modalVisible.value = false; formRef.value?.resetFields() },
    })
  } else {
    modalVisible.value = false
    formRef.value?.resetFields()
  }
}

const handleFormSaveAndNew = async () => {
  await handleModalOk()
  if (!modalVisible.value) {
    handleAdd()
  }
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
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.client-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.client-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.client-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.client-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}

.client-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.client-management > :deep(.vxe-table-list-container) {
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
.stat-online { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-offline { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }
.stat-disabled { background: linear-gradient(135deg, #fff2f0 0%, #ffccc7 100%); }

.stat-card-body { flex: 1; }
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

/* 状态指示圆点 */
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.status-dot--online { background: #52c41a; }
.status-dot--offline { background: #d9d9d9; }
.status-dot--disabled { background: #ff4d4f; }

/* 代码文本 */
.code-text {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
}
.code-text.masked {
  color: #999;
  font-style: italic;
  letter-spacing: 1px;
}

/* 复制按钮 */
.copy-btn {
  padding: 0 4px !important;
  font-size: 12px;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* FullScreenDetail 内部紧凑样式 */
:deep(.fsd-body .ant-form-item) { margin-bottom: 8px; }
:deep(.fsd-body .ant-form-item-label > label) { font-size: 12px; height: 28px; }
:deep(.fsd-body .ant-input) { font-size: 12px; }
:deep(.fsd-body .ant-btn) { font-size: 12px; }
</style>
