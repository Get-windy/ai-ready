<template>
  <PageContainer full-height>
    <template #header>
      <div class="template-page-header">
        <div class="template-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>打印管理</a-breadcrumb-item>
            <a-breadcrumb-item>打印模板</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="template-page-header-title">打印模板</h2>
        </div>
        <div class="template-page-header-right">
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
          <a-popconfirm
            title="将为本系统所有业务页面生成初始打印模板，是否继续？"
            @confirm="handleSeedTemplates"
          >
            <a-button size="small" :loading="seeding" type="primary">
              <template #icon><PrinterOutlined /></template>
              初始化模板
            </a-button>
          </a-popconfirm>
        </div>
      </div>
    </template>

    <ErrorBoundary>
    <div class="template-management">
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'templateId'"
        :filter-fields="filterFields"
        :show-search="false"
        :selectable="true"
        add-text="新增模板"
        :min-empty-rows="12"
        @add="handleAdd"
        @edit="handleEdit"
        @delete="handleDeleteConfirm"
        @batch-delete="handleBatchDelete"
        @refresh="debounceClick('refresh', fetchData)"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="(_rows: any, ids: any) => { selectedRowKeys = ids as number[] }"
      >
        <template #toolbar-actions>
          <a-tooltip title="当前状态筛选已应用">
            <span v-if="hasActiveFilter" class="active-filter-badge">
              <FilterOutlined /> 筛选中
            </span>
          </a-tooltip>
        </template>

        <template #pageCodeCell="{ record }">
          <a-tag color="blue">{{ record.pageCode }}</a-tag>
        </template>

        <template #statusCell="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'default'">
            {{ statusLabelMap[record.status] || '未知' }}
          </a-tag>
        </template>

        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" v-permission="'printing:template:edit'" @click="handleEdit(record)">编辑</a-button>
            <a-button
              v-if="record.status === 0"
              type="link"
              size="small"
              :style="{ color: '#52c41a' }"
 v-permission="'printing:template:publish'" @click="handlePublish(record)"
            >发布</a-button>
            <a-button type="link" size="small" v-permission="'printing:template:copy'" @click="handleCopy(record)">复制</a-button>
            <a-button type="link" size="small" danger v-permission="'printing:template:deleteconfirm'" @click="handleDeleteConfirm(record)">删除</a-button>
          </a-space>
        </template>

        <template #empty>
          <a-empty v-if="!hasError" description="暂无模板数据" />
          <a-result v-else status="error" title="数据加载失败">
            <template #extra>
              <a-button type="primary" @click="debounceClick('refresh', fetchData)()">
                <template #icon><ReloadOutlined /></template>
                重新加载
              </a-button>
            </template>
          </a-result>
        </template>
      </VxeTableList>

      <!-- 模板表单弹窗 -->
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
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="模板名称" name="templateName">
            <a-input
              v-model:value="formState.templateName"
              placeholder="请输入模板名称，如 出库单模板"
            />
          </a-form-item>

          <a-form-item label="页面编码" name="pageCode">
            <a-input
              v-model:value="formState.pageCode"
              placeholder="请输入页面编码，如 order.invoice"
              :disabled="isEdit"
            />
          </a-form-item>

          <a-form-item label="纸张大小" name="paperSize">
            <a-select
              v-model:value="formState.paperSize"
              :options="paperSizeOptions"
              @change="handlePaperSizeChange"
            />
          </a-form-item>

          <template v-if="formState.paperSize === 'CUSTOM'">
            <a-form-item label="纸张宽度(mm)" name="paperWidth">
              <a-input-number
                v-model:value="formState.paperWidth"
                :min="1"
                :max="2000"
                :precision="0"
                placeholder="宽度"
                style="width: 100%"
              />
            </a-form-item>

            <a-form-item label="纸张高度(mm)" name="paperHeight">
              <a-input-number
                v-model:value="formState.paperHeight"
                :min="1"
                :max="2000"
                :precision="0"
                placeholder="高度"
                style="width: 100%"
              />
            </a-form-item>
          </template>

          <a-divider>页面边距 (mm)</a-divider>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="上边距" name="marginTop" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
                <a-input-number
                  v-model:value="formState.marginTop"
                  :min="0"
                  :max="200"
                  :precision="1"
                  placeholder="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="下边距" name="marginBottom" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
                <a-input-number
                  v-model:value="formState.marginBottom"
                  :min="0"
                  :max="200"
                  :precision="1"
                  placeholder="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="左边距" name="marginLeft" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
                <a-input-number
                  v-model:value="formState.marginLeft"
                  :min="0"
                  :max="200"
                  :precision="1"
                  placeholder="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="右边距" name="marginRight" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
                <a-input-number
                  v-model:value="formState.marginRight"
                  :min="0"
                  :max="200"
                  :precision="1"
                  placeholder="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </FullScreenDetail>
    </div>
    </ErrorBoundary>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, h } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  SyncOutlined,
  ReloadOutlined,
  FilterOutlined,
  PrinterOutlined,
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { printingApi, type PrintTemplateVO, type PrintTemplateCreateRequest } from '@/api/printing'
import { seedAllTemplates } from '@/views/printing/seed-templates'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'

// ═══════════════════════════════════════════════════════════════
// 常量定义
// ═══════════════════════════════════════════════════════════════

/** 状态枚举映射 */
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'orange' },
  1: { label: '已发布', color: 'green' },
  2: { label: '已禁用', color: 'red' },
}

const statusLabelMap: Record<number, string> = {}
const statusColorMap: Record<number, string> = {}
for (const [key, val] of Object.entries(STATUS_MAP)) {
  const k = Number(key)
  statusLabelMap[k] = val.label
  statusColorMap[k] = val.color
}

/** 纸张大小选项 */
const paperSizeOptions = [
  { label: 'A4 (210 x 297mm)', value: 'A4' },
  { label: 'A5 (148 x 210mm)', value: 'A5' },
  { label: '自定义', value: 'CUSTOM' },
]

/** 状态筛选选项 */
const statusFilterOptions = computed(() => [
  { label: '全部', value: undefined },
  ...Object.entries(STATUS_MAP).map(([key, val]) => ({
    label: val.label,
    value: Number(key),
  })),
])

/** 默认纸张尺寸 */
const PAPER_DIMENSIONS: Record<string, { width: number; height: number }> = {
  A4: { width: 210, height: 297 },
  A5: { width: 148, height: 210 },
}

// ═══════════════════════════════════════════════════════════════
// 状态
// ═══════════════════════════════════════════════════════════════

// 搜索表单
const searchForm = reactive({
  pageCode: undefined as string | undefined,
  status: undefined as number | undefined,
})

// 表格数据
const tableData = ref<PrintTemplateVO[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 防抖工具 ──────────────────────────────────────────
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

// ── 数据源 ──────────────────────────────────────────
const tableDataSource = tableData

// 是否有激活的筛选条件
const hasActiveFilter = computed(() =>
  searchForm.pageCode !== undefined || searchForm.status !== undefined
)

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

// 表格列
const vxeColumns = computed(() => [
  { field: 'templateName', title: '模板名称', width: 200, showOverflow: 'tooltip' },
  { field: 'pageCode', title: '页面编码', width: 150, slotName: 'pageCodeCell' },
  { field: 'paperSize', title: '纸张大小', width: 140 },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'version', title: '版本', width: 80, align: 'center' },
  { field: 'createdAt', title: '创建时间', width: 170 },
  { type: 'action', title: '操作', width: 260, fixed: 'right' },
])

// 筛选字段
const filterFields = computed<FilterField[]>(() => [
  {
    key: 'pageCode',
    label: '页面编码',
    type: 'input',
    placeholder: '请输入页面编码',
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: statusFilterOptions.value,
    placeholder: '请选择状态',
  },
])

// 弹窗
const modalVisible = ref(false)
const modalLoading = ref(false)
const seeding = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const modalTitle = computed(() => (isEdit.value ? '编辑模板' : '新增模板'))

const formState = reactive({
  templateId: 0,
  templateName: '',
  pageCode: '',
  templateJson: '',
  paperSize: 'A4',
  paperWidth: 210,
  paperHeight: 297,
  marginTop: 0,
  marginBottom: 0,
  marginLeft: 0,
  marginRight: 0,
})

// ── 表单脏检测 ──────────────────────────────────────
const initialFormSnapshot = ref('')
let watchReady = false
const formDirty = computed(() => {
  if (!watchReady) return false
  return JSON.stringify(formState) !== initialFormSnapshot.value
})
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify(formState)
}

// ── 离开守卫 ────────────────────────────────────────
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
const validateCustomDimension = (_rule: any, value: number) => {
  if (formState.paperSize === 'CUSTOM' && (!value || value <= 0)) {
    return Promise.reject(new Error('请设置自定义纸张尺寸'))
  }
  return Promise.resolve()
}

const formRules: Record<string, any[]> = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  pageCode: [{ required: true, message: '请输入页面编码', trigger: 'blur' }],
  paperSize: [{ required: true, message: '请选择纸张大小', trigger: 'change' }],
  paperWidth: [
    { required: true, message: '请输入纸张宽度', trigger: 'blur' },
    { validator: validateCustomDimension, trigger: 'blur' },
  ],
  paperHeight: [
    { required: true, message: '请输入纸张高度', trigger: 'blur' },
    { validator: validateCustomDimension, trigger: 'blur' },
  ],
  marginTop: [{ required: true, message: '请输入上边距', trigger: 'blur' }],
  marginBottom: [{ required: true, message: '请输入下边距', trigger: 'blur' }],
  marginLeft: [{ required: true, message: '请输入左边距', trigger: 'blur' }],
  marginRight: [{ required: true, message: '请输入右边距', trigger: 'blur' }],
}

// ═══════════════════════════════════════════════════════════════
// API 操作
// ═══════════════════════════════════════════════════════════════

/** 构建创建/更新请求体 */
function buildCreateRequest(): PrintTemplateCreateRequest {
  return {
    pageCode: formState.pageCode,
    templateName: formState.templateName,
    templateJson: formState.templateJson,
    paperSize: formState.paperSize,
    paperWidth: formState.paperWidth,
    paperHeight: formState.paperHeight,
    margins: {
      top: formState.marginTop,
      bottom: formState.marginBottom,
      left: formState.marginLeft,
      right: formState.marginRight,
    },
  }
}

// 数据加载
const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await printingApi.getTemplates({
      page: pagination.current,
      size: pagination.pageSize,
      pageCode: searchForm.pageCode,
      status: searchForm.status,
    })
    if (res.data) {
      tableData.value = res.records
      pagination.total = res.total
    }
  } catch (error) {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[打印模板] 加载模板数据失败', error)
    message.error('加载模板数据失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// ── 事件处理 ────────────────────────────────────────

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { pageCode: undefined, status: undefined })
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

/** 初始化业务打印模板 */
const handleSeedTemplates = async () => {
  seeding.value = true
  try {
    const result = await seedAllTemplates()
    message.success(`模板初始化完成：成功 ${result.success} / 总计 ${result.total}`)
    fetchData()
  } catch (err: any) {
    console.error('[打印模板] 初始化模板失败', err)
    message.error('初始化模板失败: ' + (err?.message || '未知错误'))
  } finally {
    seeding.value = false
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  watchReady = false
  Object.assign(formState, {
    templateId: 0,
    templateName: '',
    pageCode: '',
    templateJson: '',
    paperSize: 'A4',
    paperWidth: 210,
    paperHeight: 297,
    marginTop: 0,
    marginBottom: 0,
    marginLeft: 0,
    marginRight: 0,
  })
  modalVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
    saveFormSnapshot()
    watchReady = true
  })
}

// 编辑
const handleEdit = (record: PrintTemplateVO) => {
  isEdit.value = true
  watchReady = false
  Object.assign(formState, {
    templateId: record.templateId,
    templateName: record.templateName,
    pageCode: record.pageCode,
    templateJson: record.templateJson,
    paperSize: record.paperSize,
    paperWidth: record.paperWidth,
    paperHeight: record.paperHeight,
    marginTop: record.marginTop,
    marginBottom: record.marginBottom,
    marginLeft: record.marginLeft,
    marginRight: record.marginRight,
  })
  modalVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
    saveFormSnapshot()
    watchReady = true
  })
}

// 纸张大小切换
const handlePaperSizeChange = (value: string) => {
  const dims = PAPER_DIMENSIONS[value]
  if (dims) {
    formState.paperWidth = dims.width
    formState.paperHeight = dims.height
  }
  // 清除自定义尺寸的验证错误
  nextTick(() => {
    formRef.value?.clearValidate(['paperWidth', 'paperHeight'])
  })
}

// 发布
const handlePublish = async (record: PrintTemplateVO) => {
  try {
    await printingApi.publishTemplate(record.templateId)
    message.success('模板发布成功')
    fetchData()
  } catch (err) {
    console.warn('[打印模板] 发布失败', err)
    message.error('发布失败')
  }
}

// 复制
const handleCopy = (record: PrintTemplateVO) => {
  let newName = `${record.templateName} - 副本`
  const inputStyle = {
    width: '100%',
    padding: '4px 11px',
    border: '1px solid #d9d9d9',
    borderRadius: '4px',
    height: '32px',
    fontSize: '14px',
    outline: 'none',
    boxSizing: 'border-box' as const,
  }

  Modal.confirm({
    title: '复制模板',
    content: h('div', [
      h('p', { style: 'margin-bottom: 8px; color: #666; font-size: 13px;' }, '请输入新模板名称：'),
      h('input', {
        style: inputStyle,
        value: newName,
        placeholder: '请输入模板名称',
        onInput: (e: any) => { newName = e.target.value },
      }),
    ]),
    async onOk() {
      if (!newName.trim()) {
        message.error('请输入模板名称')
        return Promise.reject()
      }
      try {
        await printingApi.copyTemplate(record.templateId, newName.trim())
        message.success('模板复制成功')
        fetchData()
      } catch (err) {
        console.warn('[打印模板] 复制失败', err)
        message.error('复制失败')
      }
    },
  })
}

// 删除
const handleDeleteConfirm = (record: PrintTemplateVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除模板 "${record.templateName}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await printingApi.deleteTemplate(record.templateId)
        message.success('删除成功')
        fetchData()
      } catch (err) {
        console.warn('[打印模板] 删除失败', err)
        message.error('删除失败')
      }
    },
  })
}

// 批量删除
const handleBatchDelete = (deleteKeys?: number[]) => {
  const ids = deleteKeys || selectedRowKeys.value
  if (!ids.length) return
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${ids.length} 个模板吗？此操作不可撤销。`,
    centered: true,
    async onOk() {
      try {
        await Promise.all(ids.map((id: number) => printingApi.deleteTemplate(id)))
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchData()
      } catch (err) {
        console.warn('[打印模板] 批量删除失败', err)
        message.error('批量删除失败')
      }
    },
  })
}

// 提交表单
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (isEdit.value) {
      await printingApi.updateTemplate(formState.templateId, buildCreateRequest())
      message.success('更新成功')
    } else {
      await printingApi.createTemplate(buildCreateRequest())
      message.success('创建成功')
    }

    modalVisible.value = false
    watchReady = false
    fetchData()
  } catch (error) {
    // 如果是验证错误，ant-design-vue 会内部处理
    if (error && typeof error === 'object' && 'errorFields' in error) return
    console.warn('[打印模板] 保存失败', error)
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
    // 保存成功后立即重置表单进入新建状态
    handleAdd()
  }
}

// ── 键盘快捷键 ──────────────────────────────────────
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

// ═══════════════════════════════════════════════════════════════
// 生命周期
// ═══════════════════════════════════════════════════════════════

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  startAutoRefresh()

  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearTimeout(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

function startAutoRefresh() {
  refreshTimer = setTimeout(async () => {
    await fetchData()
    autoRefreshCountdown.value = 30
    startAutoRefresh()
  }, 30000)
}

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.template-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.template-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.template-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.template-page-header-right {
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

.active-filter-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #1890ff;
  padding: 2px 8px;
  border-radius: 4px;
  background: #e6f7ff;
}

.template-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.template-management > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* ── FullScreenDetail 内部紧凑样式 ──────────────── */
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

/* 自定义纸张区域分割线间距 */
:deep(.fsd-body .ant-divider) {
  font-size: 12px;
  color: #666;
  margin: 12px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .template-page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

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

/* ── 加载骨架屏 ──────────────────────── */
:deep(.template-loading-skeleton) {
  padding: 24px;
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
