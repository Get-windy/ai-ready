<template>
  <PageContainer full-height>
    <template #header>
      <div class="chain-page-header">
        <div class="chain-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>打印管理</a-breadcrumb-item>
            <a-breadcrumb-item>打印链管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="chain-page-header-title">打印链管理</h2>
        </div>
        <div class="chain-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
<span class="shortcut-hints">
                                                <span class="shortcut-hint"><kbd>Ctrl+R</kbd> 刷新</span>
                                                <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
                                                <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
                                              </span>
        </div>
      </div>
    </template>

    <ErrorBoundary>
    <div class="chain-management">
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'chainId'"
        :filter-fields="filterFields"
        :show-search="false"
        :selectable="false"
        add-text="新增打印链"
        :min-empty-rows="12"
        @add="handleAdd"
        @edit="handleEdit"
        @delete="handleDeleteConfirm"
        @refresh="debounceClick('refresh', fetchData)"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
      >
        <template #statusCell="{ record }">
          <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'red'">
            {{ record.status === 'ACTIVE' ? '启用' : '禁用' }}
          </a-tag>
        </template>

        <template #stepCountCell="{ record }">
          {{ record.items?.length ?? '-' }}
        </template>

        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" v-permission="'printing:chain:edit'" @click="handleEdit(record)">
              编辑
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="debounceClick('toggle_' + record.chainId, () => handleToggleStatus(record))()"
            >
              {{ record.status === 'ACTIVE' ? '禁用' : '启用' }}
            </a-button>
            <a-button type="link" size="small" danger v-permission="'printing:chain:deleteconfirm'" @click="handleDeleteConfirm(record)">
              删除
            </a-button>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 创建/编辑表单 -->
      <FullScreenDetail
        :visible="modalVisible"
        :title="modalTitle"
        :save-loading="modalLoading"
        :show-save-and-new="!isEdit"
        @save="handleModalOk"
        @close="handleFormClose"
        @save-and-new="handleFormSaveAndNew"
      >
        <a-form
          ref="formRef"
          :model="formState"
          :rules="formRules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-form-item label="页面编码" name="pageCode">
            <a-input
              v-model:value="formState.pageCode"
              placeholder="请输入页面编码"
              :disabled="isEdit"
            />
          </a-form-item>
          <a-form-item label="打印链名称" name="chainName">
            <a-input
              v-model:value="formState.chainName"
              placeholder="请输入打印链名称"
            />
          </a-form-item>
          <a-form-item label="描述" name="description">
            <a-textarea
              v-model:value="formState.description"
              placeholder="请输入描述"
              :rows="2"
            />
          </a-form-item>
          <a-form-item label="排序号" name="sortOrder">
            <a-input-number
              v-model:value="formState.sortOrder"
              :min="0"
              placeholder="请输入排序号"
              style="width: 200px"
            />
          </a-form-item>

          <!-- 步骤编辑器 -->
          <a-form-item label="打印步骤" name="items">
            <div class="steps-editor">
              <div class="steps-editor-header">
                <span class="steps-editor-title">步骤列表（{{ formState.items.length }}/10）</span>
                <a-button
                  type="dashed"
                  size="small"
                  :disabled="formState.items.length >= 10"
                  @click="addStep"
                >
                  <template #icon><PlusOutlined /></template>
                  添加步骤
                </a-button>
              </div>
              <a-table
                v-if="formState.items.length > 0"
                :data-source="formState.items"
                :columns="stepColumns"
                :pagination="false as any"
                :row-key="'_key'"
                size="small"
                bordered
                class="steps-table"
              >
                <template #bodyCell="{ column, record, index }">
                  <template v-if="column.key === 'stepOrder'">
                    {{ index + 1 }}
                  </template>
                  <template v-else-if="column.key === 'templateId'">
                    <a-select
                      v-model:value="record.templateId"
                      placeholder="请选择模板"
                      style="width: 100%"
                      :options="templateOptions"
                      show-search
                      option-filter-prop="label"
                      @change="onStepTemplateChange(record as any)"
                    />
                  </template>
                  <template v-else-if="column.key === 'clientId'">
                    <a-select
                      v-model:value="record.clientId"
                      placeholder="请选择客户端"
                      style="width: 100%"
                      :options="clientOptions"
                      show-search
                      option-filter-prop="label"
                      @change="onStepClientChange(record as any)"
                    />
                  </template>
                  <template v-else-if="column.key === 'printerName'">
                    <a-input
                      v-model:value="record.printerName"
                      placeholder="请输入打印机名称"
                    />
                  </template>
                  <template v-else-if="column.key === 'screenshotMode'">
                    <a-select
                      v-model:value="record.screenshotMode"
                      placeholder="请选择模式"
                      style="width: 100%"
                      :options="screenshotModeOptions"
                      @change="onScreenshotModeChange(record as any)"
                    />
                  </template>
                  <template v-else-if="column.key === 'screenshotConfirmTimeout'">
                    <a-input-number
                      v-if="record.screenshotMode === 'AUTO_CONFIRM'"
                      v-model:value="record.screenshotConfirmTimeout"
                      :min="1"
                      :max="300"
                      placeholder="超时(秒)"
                      style="width: 100%"
                    />
                    <span v-else class="text-muted">-</span>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-button
                      type="link"
                      danger
                      size="small"
                      :disabled="formState.items.length <= 1"
                      @click="removeStep(index)"
                    >
                      删除
                    </a-button>
                  </template>
                </template>
              </a-table>
              <a-empty v-else description="暂无步骤，请点击「添加步骤」" />
            </div>
          </a-form-item>
        </a-form>
      </FullScreenDetail>
    </div>
    </ErrorBoundary>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { printingApi } from '@/api/printing'
import type {
  PrintChainVO,
  PrintChainCreateRequest,
  ChainItemRequest,
  ChainItemVO,
  PrintTemplateVO,
  PrintClientVO,
} from '@/api/printing'

// ────────────────────────────
// 类型定义
// ────────────────────────────

interface StepFormItem {
  _key: string
  itemId?: number
  stepOrder: number
  templateId: number | undefined
  templateName: string
  clientId: number | undefined
  clientName: string
  printerName: string
  screenshotMode: string
  screenshotConfirmTimeout: number | undefined
}

interface ChainFormState {
  chainId: number
  pageCode: string
  chainName: string
  description: string
  sortOrder: number | undefined
  items: StepFormItem[]
}

// ────────────────────────────
// 常量 & 选项
// ────────────────────────────

const screenshotModeOptions = [
  { label: '禁用', value: 'DISABLED' },
  { label: '手动确认', value: 'MANUAL_CONFIRM' },
  { label: '自动确认', value: 'AUTO_CONFIRM' },
]

const stepColumns = [
  { title: '序号', key: 'stepOrder', width: 50, align: 'center' },
  { title: '打印模板', key: 'templateId', width: 180 },
  { title: '打印客户端', key: 'clientId', width: 180 },
  { title: '打印机名称', key: 'printerName', width: 140 },
  { title: '截图模式', key: 'screenshotMode', width: 130 },
  { title: '确认超时(秒)', key: 'screenshotConfirmTimeout', width: 110 },
  { title: '操作', key: 'action', width: 60, align: 'center' },
] as any

// ────────────────────────────
// 状态
// ────────────────────────────

const tableRef = ref<any>(null)
const tableData = ref<PrintChainVO[]>([])
const loading = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)

// 模板 & 客户端选项（用于步骤编辑器的选择器）
const templateOptions = ref<{ label: string; value: number }[]>([])
const clientOptions = ref<{ label: string; value: number }[]>([])

// ── 防抖工具 ──────────────────
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

// ── 数据源 ─────────────────────
const tableDataSource = tableData

// ── 分页 ───────────────────────
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

// ── 筛选 ───────────────────────
const searchForm = reactive({
  pageCode: undefined as string | undefined,
})

const filterFields = computed<FilterField[]>(() => [
  { key: 'pageCode', label: '页面编码', type: 'input', placeholder: '请输入页面编码' },
])

// ── 表格列 ─────────────────────
const vxeColumns = computed(() => [
  { field: 'chainName', title: '打印链名称', width: 160, showOverflow: 'tooltip' },
  { field: 'pageCode', title: '页面编码', width: 120 },
  { field: 'description', title: '描述', width: 200, showOverflow: 'tooltip' },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { field: 'stepCount', title: '步骤数', width: 80, align: 'center', slotName: 'stepCountCell' },
  { field: 'createdAt', title: '创建时间', width: 160 },
  { type: 'action', title: '操作', width: 220, fixed: 'right' },
])

// ── 弹窗 ───────────────────────
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const modalTitle = computed(() => (isEdit.value ? '编辑打印链' : '新增打印链'))

let stepKeyCounter = 0
function nextStepKey(): string {
  return `step_${Date.now()}_${stepKeyCounter++}`
}

function createEmptyStep(): StepFormItem {
  return {
    _key: nextStepKey(),
    stepOrder: 0,
    templateId: undefined,
    templateName: '',
    clientId: undefined,
    clientName: '',
    printerName: '',
    screenshotMode: 'DISABLED',
    screenshotConfirmTimeout: undefined,
  }
}

const formState = reactive<ChainFormState>({
  chainId: 0,
  pageCode: '',
  chainName: '',
  description: '',
  sortOrder: undefined,
  items: [],
})

// ── 表单脏检测 ─────────────────
const initialFormSnapshot = ref('')
let watchReady = false
const formDirty = computed(() => {
  if (!watchReady) return false
  return JSON.stringify(formState) !== initialFormSnapshot.value
})
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify(formState)
}

// ── 离开守卫 ───────────────────
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

// ── 表单校验规则 ──────────────
const formRules = {
  pageCode: { required: true, message: '请输入页面编码', trigger: 'blur' },
  chainName: { required: true, message: '请输入打印链名称', trigger: 'blur' },
  sortOrder: { type: 'number' as const, message: '排序号必须为数字', trigger: 'blur' },
} as any

// ────────────────────────────
// 数据加载
// ────────────────────────────

const fetchData = async () => {
  loading.value = true
  try {
    const res = await printingApi.getChains({
      page: pagination.current,
      size: pagination.pageSize,
      pageCode: searchForm.pageCode,
    })
    if (res.data) {
      tableData.value = res.records
      pagination.total = res.total
    }
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    message.error('加载打印链列表失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const fetchTemplateOptions = async () => {
  try {
    const res = await printingApi.getTemplates({ page: 1, size: 9999 })
    if (res.data) {
      templateOptions.value = res.records.map((t: PrintTemplateVO) => ({
        label: t.templateName,
        value: t.templateId,
      }))
    }
  } catch {
    console.warn('[打印链] 加载模板列表失败')
  }
}

const fetchClientOptions = async () => {
  try {
    const res = await printingApi.getClients({ page: 1, size: 9999 })
    if (res.data) {
      clientOptions.value = res.records.map((c: PrintClientVO) => ({
        label: c.clientName,
        value: c.clientId,
      }))
    }
  } catch {
    console.warn('[打印链] 加载客户端列表失败')
  }
}

// ────────────────────────────
// 搜索 / 筛选 / 分页
// ────────────────────────────

const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    searchForm.pageCode = undefined
  } else {
    searchForm.pageCode = filters.pageCode
  }
  pagination.current = 1
  fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ────────────────────────────
// 步骤编辑器
// ────────────────────────────

function addStep() {
  if (formState.items.length >= 10) return
  formState.items.push(createEmptyStep())
}

function removeStep(index: number) {
  if (formState.items.length <= 1) return
  formState.items.splice(index, 1)
}

function onStepTemplateChange(record: StepFormItem) {
  const found = templateOptions.value.find((o) => o.value === record.templateId)
  record.templateName = found?.label ?? ''
}

function onStepClientChange(record: StepFormItem) {
  const found = clientOptions.value.find((o) => o.value === record.clientId)
  record.clientName = found?.label ?? ''
}

function onScreenshotModeChange(record: StepFormItem) {
  if (record.screenshotMode !== 'AUTO_CONFIRM') {
    record.screenshotConfirmTimeout = undefined
  }
}

// ────────────────────────────
// 新增 / 编辑
// ────────────────────────────

function resetFormState() {
  isEdit.value = false
  formState.chainId = 0
  formState.pageCode = ''
  formState.chainName = ''
  formState.description = ''
  formState.sortOrder = undefined
  formState.items = []
  addStep()
}

async function handleAdd() {
  resetFormState()
  modalVisible.value = true
  nextTick(() => {
    saveFormSnapshot()
    watchReady = true
  })
}

async function handleEdit(record: PrintChainVO) {
  isEdit.value = true
  watchReady = false
  const loadingMsg = message.loading('加载中...', 0)
  try {
    const res = await printingApi.getChain(record.chainId)
    const chain = res.data
    formState.chainId = chain.chainId
    formState.pageCode = chain.pageCode
    formState.chainName = chain.chainName
    formState.description = chain.description ?? ''
    formState.sortOrder = chain.sortOrder
    formState.items = (chain.items ?? []).map((item: ChainItemVO) => ({
      _key: nextStepKey(),
      itemId: item.itemId,
      stepOrder: item.stepOrder,
      templateId: item.templateId,
      templateName: item.templateName ?? '',
      clientId: item.clientId,
      clientName: item.clientName ?? '',
      printerName: item.printerName,
      screenshotMode: item.screenshotMode || 'DISABLED',
      screenshotConfirmTimeout: item.screenshotMode === 'AUTO_CONFIRM' ? item.screenshotConfirmTimeout : undefined,
    }))
    modalVisible.value = true
  } catch {
    message.error('获取打印链详情失败')
    return
  } finally {
    loadingMsg()
  }
  nextTick(() => {
    saveFormSnapshot()
    watchReady = true
  })
}

// ────────────────────────────
// 构建请求数据
// ────────────────────────────

function buildCreateRequest(): PrintChainCreateRequest {
  return {
    pageCode: formState.pageCode,
    chainName: formState.chainName,
    description: formState.description || undefined,
    sortOrder: formState.sortOrder,
    items: formState.items.map((item, index) => {
      const req: ChainItemRequest = {
        stepOrder: index + 1,
        templateId: item.templateId!,
        clientId: item.clientId!,
        printerName: item.printerName,
      }
      if (item.screenshotMode && item.screenshotMode !== 'DISABLED') {
        req.screenshotMode = item.screenshotMode
      }
      if (item.screenshotMode === 'AUTO_CONFIRM' && item.screenshotConfirmTimeout) {
        req.screenshotConfirmTimeout = item.screenshotConfirmTimeout
      }
      return req
    }),
  }
}

// ────────────────────────────
// 提交 / 保存
// ────────────────────────────

function validateSteps(): boolean {
  if (formState.items.length === 0) {
    message.warning('至少需要一个打印步骤')
    return false
  }
  for (let i = 0; i < formState.items.length; i++) {
    const item = formState.items[i]
    if (!item.templateId) {
      message.warning(`步骤 ${i + 1}：请选择打印模板`)
      return false
    }
    if (!item.clientId) {
      message.warning(`步骤 ${i + 1}：请选择打印客户端`)
      return false
    }
    if (!item.printerName?.trim()) {
      message.warning(`步骤 ${i + 1}：请输入打印机名称`)
      return false
    }
  }
  return true
}

async function handleModalOk() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!validateSteps()) return

  modalLoading.value = true
  try {
    const data = buildCreateRequest()
    if (isEdit.value) {
      await printingApi.updateChain(formState.chainId, data)
      message.success('更新成功')
    } else {
      await printingApi.createChain(data)
      message.success('创建成功')
    }
    modalVisible.value = false
    watchReady = false
    formRef.value?.resetFields()
    fetchData()
  } catch {
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
      onOk: () => {
        modalVisible.value = false
        watchReady = false
        formRef.value?.resetFields()
      },
    })
  } else {
    modalVisible.value = false
    watchReady = false
    formRef.value?.resetFields()
  }
}

const handleFormSaveAndNew = async () => {
  await handleModalOk()
  if (!modalVisible.value) {
    resetFormState()
    modalVisible.value = true
    nextTick(() => {
      saveFormSnapshot()
      watchReady = true
    })
  }
}

// ────────────────────────────
// 删除
// ────────────────────────────

const handleDeleteConfirm = (record: PrintChainVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除打印链 "${record.chainName}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await printingApi.deleteChain(record.chainId)
        message.success('删除成功')
        fetchData()
      } catch {
        message.error('删除失败')
      }
    },
  })
}

// ────────────────────────────
// 切换状态（启用/禁用）
// ────────────────────────────

const handleToggleStatus = async (record: PrintChainVO) => {
  const newStatus = record.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const label = newStatus === 'ACTIVE' ? '启用' : '禁用'
  try {
    await printingApi.updateChainStatus(record.chainId, newStatus)
    message.success(`${label}成功`)
    fetchData()
  } catch {
    message.error(`${label}失败`)
  }
}

// ────────────────────────────
// 键盘快捷键
// ────────────────────────────

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

// ────────────────────────────
// 生命周期
// ────────────────────────────

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchData()
  fetchTemplateOptions()
  fetchClientOptions()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.chain-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.chain-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.chain-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.chain-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chain-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.chain-management > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 步骤编辑器 */
.steps-editor {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.steps-editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.steps-editor-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.steps-table {
  width: 100%;
}

.text-muted {
  color: #bbb;
}

/* ── FullScreenDetail 内部紧凑样式 ── */
:deep(.fsd-body .ant-form-item) { margin-bottom: 8px; }
:deep(.fsd-body .ant-form-item-label > label) { font-size: 12px; height: 28px; }
:deep(.fsd-body .ant-input),
:deep(.fsd-body .ant-input-number),
:deep(.fsd-body .ant-select),
:deep(.fsd-body .ant-picker),
:deep(.fsd-body .ant-cascader-picker) { font-size: 12px; }
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

/* ── 更新时间显示 ──────────────────────── */
.update-time {
  font-size: 12px;
  color: #999;
}

/* ── 自动刷新倒计时 ──────────────────────── */
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

/* ── vxe-table 表头边框 ──────────────────────── */
:deep(.vxe-table--header-border) {
  border-bottom: 2px solid #e8e8e8 !important;
}

/* ── 空状态容器 ──────────────────────── */
:deep(.empty-state-wrapper) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}

</style>
