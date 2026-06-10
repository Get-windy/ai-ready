<template>
  <PageContainer full-height>
    <template #header>
      <div class="task-page-header">
        <div class="task-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>打印管理</a-breadcrumb-item>
            <a-breadcrumb-item>打印任务</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="task-page-header-title">打印任务</h2>
        </div>
        <div class="task-page-header-right">
          <a-tooltip title="开启后将每 30 秒自动刷新">
            <a-switch
              v-model:checked="autoRefreshEnabled"
              checked-children="自动"
              un-checked-children="手动"
              size="small"
              style="margin-right: 8px"
            />
          </a-tooltip>
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshEnabled && autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button type="primary" size="small" @click="handleOpenExecuteChain">
            <template #icon><SendOutlined /></template>
            执行打印链
          </a-button>
        </div>
      </div>
    </template>

    <div class="task-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">任务总数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-pending">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.PENDING }}</div>
            <div class="stat-card-label">待处理</div>
          </div>
          <ClockCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-queued">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.QUEUED }}</div>
            <div class="stat-card-label">队列中</div>
          </div>
          <EllipsisOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-printing">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.PRINTING }}</div>
            <div class="stat-card-label">打印中</div>
          </div>
          <PrinterOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-completed">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.COMPLETED }}</div>
            <div class="stat-card-label">已完成</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-failed">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.FAILED }}</div>
            <div class="stat-card-label">失败</div>
          </div>
          <CloseCircleOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'taskId'"
        :filter-fields="filterFields"
        :show-add="false"
        :show-delete="false"
        :show-batch-delete="false"
        :selectable="false"
        @refresh="debounceClick('refresh', fetchData)"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
      >
        <template #toolbar-actions>
          <a-select
            v-model:value="filterForm.statusList"
            mode="multiple"
            placeholder="任务状态"
            style="min-width: 180px"
            size="small"
            allow-clear
            @change="handleStatusFilterChange"
          >
            <a-select-option value="PENDING">待处理</a-select-option>
            <a-select-option value="QUEUED">队列中</a-select-option>
            <a-select-option value="PRINTING">打印中</a-select-option>
            <a-select-option value="COMPLETED">已完成</a-select-option>
            <a-select-option value="FAILED">失败</a-select-option>
            <a-select-option value="CANCELLED">已取消</a-select-option>
          </a-select>
        </template>

        <template #empty>
          <a-empty v-if="!hasError" description="暂无打印任务" />
          <a-result v-else status="error" title="数据加载失败">
            <template #extra>
              <a-button type="primary" @click="debounceClick('refresh', fetchData)()">
                <template #icon><ReloadOutlined /></template>
                重新加载
              </a-button>
            </template>
          </a-result>
        </template>

        <template #statusCell="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'default'">
            {{ statusLabelMap[record.status] || record.status }}
          </a-tag>
        </template>

        <template #priorityCell="{ record }">
          <a-tag :color="record.priority >= 5 ? 'red' : record.priority >= 3 ? 'orange' : 'default'">
            {{ record.priority }}
          </a-tag>
        </template>

        <template #documentTypeCell="{ record }">
          <a-tag>{{ record.documentType || '-' }}</a-tag>
        </template>

        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="handleViewDetail(record)">
              详情
            </a-button>
            <a-button
              v-if="record.status === 'PENDING' || record.status === 'QUEUED'"
              type="link"
              size="small"
              danger
              @click="handleCancelTask(record)"
            >
              取消
            </a-button>
            <a-button
              v-if="record.screenshotId != null && record.screenshotStatus === 'PENDING'"
              type="link"
              size="small"
              @click="handleConfirmScreenshot(record)"
            >
              确认截图
            </a-button>
            <a-button
              v-if="record.screenshotStatus === 'FAILED'"
              type="link"
              size="small"
              @click="handleRetryScreenshot(record)"
            >
              重新截图
            </a-button>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 任务详情弹窗 -->
      <a-modal
        :visible="detailVisible"
        :title="`任务详情 - ${detailData?.taskCode || ''}`"
        :footer="null"
        :width="700"
        @cancel="handleDetailClose"
      >
        <a-spin :spinning="detailLoading">
          <a-descriptions bordered column="2" size="small" v-if="detailData">
            <a-descriptions-item label="任务编号" :span="2">
              <a-typography-text copyable>{{ detailData.taskCode }}</a-typography-text>
            </a-descriptions-item>
            <a-descriptions-item label="状态" :span="1">
              <a-tag :color="statusColorMap[detailData.status] || 'default'">
                {{ statusLabelMap[detailData.status] || detailData.status }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="优先级" :span="1">
              <a-tag :color="detailData.priority >= 5 ? 'red' : detailData.priority >= 3 ? 'orange' : 'default'">
                {{ detailData.priority }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="单据类型" :span="1">{{ detailData.documentType || '-' }}</a-descriptions-item>
            <a-descriptions-item label="单据编号" :span="1">{{ detailData.documentNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="页面编码" :span="2">{{ detailData.pageCode || '-' }}</a-descriptions-item>
            <a-descriptions-item label="打印链" :span="1">{{ detailData.chainName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="步骤顺序" :span="1">{{ detailData.stepOrder ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="打印模板" :span="1">{{ detailData.templateName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="客户端" :span="1">{{ detailData.clientName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="打印机" :span="2">{{ detailData.printerName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="提交时间" :span="1">{{ detailData.submitTime || '-' }}</a-descriptions-item>
            <a-descriptions-item label="开始时间" :span="1">{{ detailData.startTime || '-' }}</a-descriptions-item>
            <a-descriptions-item label="完成时间" :span="2">{{ detailData.completeTime || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间" :span="2">{{ detailData.createdAt || '-' }}</a-descriptions-item>
            <a-descriptions-item v-if="detailData.errorMessage" label="错误信息" :span="2">
              <a-alert
                type="error"
                :message="detailData.errorMessage"
                banner
                style="margin: 0"
              />
            </a-descriptions-item>
          </a-descriptions>
          </a-spin>
        </a-modal>

      <!-- 执行打印链弹窗 -->
      <a-modal
        :visible="executeVisible"
        title="执行打印链"
        :confirm-loading="executeLoading"
        ok-text="执行"
        @ok="handleExecuteChain"
        @cancel="handleExecuteClose"
      >
        <a-form
          ref="executeFormRef"
          :model="executeForm"
          :rules="executeFormRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="打印链" name="chainId">
            <a-select
              v-model:value="executeForm.chainId"
              placeholder="请选择打印链"
              :loading="chainLoading"
              show-search
              option-filter-prop="label"
              allow-clear
            >
              <a-select-option
                v-for="chain in chainOptions"
                :key="chain.chainId"
                :value="chain.chainId"
                :label="chain.chainName"
              >
                {{ chain.chainName }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="数据 JSON" name="dataJson">
            <a-textarea
              v-model:value="executeForm.dataJson"
              placeholder='请输入打印数据 JSON，例如：{"orderNo": "ORD20250101001"}'
              :rows="6"
            />
          </a-form-item>
          <a-form-item label="页面编码">
            <a-input v-model:value="executeForm.pageCode" placeholder="可选" />
          </a-form-item>
          <a-form-item label="单据类型">
            <a-input v-model:value="executeForm.documentType" placeholder="可选" />
          </a-form-item>
          <a-form-item label="单据编号">
            <a-input v-model:value="executeForm.documentNo" placeholder="可选" />
          </a-form-item>
          <a-form-item label="单据 ID">
            <a-input-number v-model:value="executeForm.documentId" placeholder="可选" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  SyncOutlined,
  ReloadOutlined,
  SendOutlined,
  FileTextOutlined,
  ClockCircleOutlined,
  EllipsisOutlined,
  PrinterOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { printingApi, type PrintTaskVO, type PrintTaskQuery, type PrintExecuteRequest, type PrintChainVO } from '@/api/printing'
import { PageContainer } from '@/components'

// ── 状态映射 ────────────────────────────────────────────
const statusColorMap: Record<string, string> = {
  PENDING: 'default',
  QUEUED: 'blue',
  PRINTING: 'processing',
  COMPLETED: 'green',
  FAILED: 'red',
  CANCELLED: 'gray'
}

const statusLabelMap: Record<string, string> = {
  PENDING: '待处理',
  QUEUED: '队列中',
  PRINTING: '打印中',
  COMPLETED: '已完成',
  FAILED: '失败',
  CANCELLED: '已取消'
}

// ── 表格数据 ────────────────────────────────────────────
const tableData = ref<PrintTaskVO[]>([])
const loading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const autoRefreshEnabled = ref(true)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

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
const statusCounts = computed(() => {
  const counts: Record<string, number> = {
    PENDING: 0,
    QUEUED: 0,
    PRINTING: 0,
    COMPLETED: 0,
    FAILED: 0,
    CANCELLED: 0
  }
  tableData.value.forEach(item => {
    if (counts[item.status] !== undefined) {
      counts[item.status]++
    }
  })
  return counts
})

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

// 筛选表单
const filterForm = reactive({
  statusList: [] as string[],
  pageCode: undefined as string | undefined,
  documentType: undefined as string | undefined,
  documentNo: undefined as string | undefined,
  submitTimeRange: undefined as [string, string] | undefined
})

// 表格列
const vxeColumns = computed(() => [
  { field: 'taskCode', title: '任务编号', width: 160, showOverflow: 'tooltip' },
  { field: 'documentNo', title: '单据编号', width: 150, showOverflow: 'tooltip' },
  { field: 'documentType', title: '单据类型', width: 100, slotName: 'documentTypeCell' },
  { field: 'chainName', title: '打印链', width: 130, showOverflow: 'tooltip' },
  { field: 'stepOrder', title: '步骤', width: 60, align: 'center' },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'priority', title: '优先级', width: 80, align: 'center', slotName: 'priorityCell' },
  { field: 'clientName', title: '客户端', width: 120, showOverflow: 'tooltip' },
  { field: 'submitTime', title: '提交时间', width: 160 },
  { type: 'action', title: '操作', width: 220, fixed: 'right' }
])

// 筛选字段（用于 VxeTableList 内置面板）
const filterFields = computed<FilterField[]>(() => [
  { key: 'pageCode', label: '页面编码', type: 'input', placeholder: '请输入页面编码' },
  { key: 'documentType', label: '单据类型', type: 'input', placeholder: '请输入单据类型' },
  { key: 'documentNo', label: '单据编号', type: 'input', placeholder: '请输入单据编号' },
  { key: 'submitTimeRange', label: '提交时间', type: 'dateRange', placeholder: '选择时间范围' }
])

// ── 任务详情 ────────────────────────────────────────────
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<PrintTaskVO | null>(null)

// ── 执行打印链 ──────────────────────────────────────────
const executeVisible = ref(false)
const executeLoading = ref(false)
const chainLoading = ref(false)
const chainOptions = ref<PrintChainVO[]>([])
const executeFormRef = ref<FormInstance>()

const executeForm = reactive<PrintExecuteRequest & { documentId?: number }>({
  chainId: 0,
  dataJson: '',
  pageCode: undefined,
  documentType: undefined,
  documentNo: undefined,
  documentId: undefined
})

const executeFormRules = {
  chainId: { required: true, message: '请选择打印链', trigger: 'change', type: 'number' as const },
  dataJson: { required: true, message: '请输入打印数据 JSON', trigger: 'blur' }
}

// ── 数据加载 ────────────────────────────────────────────
const buildQueryParams = (): PrintTaskQuery => {
  const params: PrintTaskQuery = {
    page: pagination.current,
    size: pagination.pageSize
  }
  if (filterForm.statusList.length > 0) {
    // If the API supports comma-separated or single status, adjust accordingly.
    // For simplicity, only the first selected status is sent if API expects a single value.
    // If API supports multi, this could be adjusted. We'll send as comma-separated.
    params.status = filterForm.statusList.join(',')
  }
  if (filterForm.pageCode) params.pageCode = filterForm.pageCode
  if (filterForm.documentType) params.documentType = filterForm.documentType
  if (filterForm.documentNo) params.documentNo = filterForm.documentNo
  if (filterForm.submitTimeRange) {
    params.startTime = filterForm.submitTimeRange[0]
    params.endTime = filterForm.submitTimeRange[1]
  }
  return params
}

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await printingApi.getTasks(buildQueryParams())
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[打印任务] 加载任务数据失败')
    message.error('加载任务失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// ── 筛选变化 ────────────────────────────────────────────
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    filterForm.pageCode = undefined
    filterForm.documentType = undefined
    filterForm.documentNo = undefined
    filterForm.submitTimeRange = undefined
  } else {
    if (filters.pageCode !== undefined) filterForm.pageCode = filters.pageCode || undefined
    if (filters.documentType !== undefined) filterForm.documentType = filters.documentType || undefined
    if (filters.documentNo !== undefined) filterForm.documentNo = filters.documentNo || undefined
    if (filters.submitTimeRange !== undefined) {
      filterForm.submitTimeRange = filters.submitTimeRange || undefined
    }
  }
  pagination.current = 1
  fetchData()
}

const handleStatusFilterChange = () => {
  pagination.current = 1
  fetchData()
}

// ── 分页变化 ────────────────────────────────────────────
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ── 任务详情 ────────────────────────────────────────────
const handleViewDetail = async (record: PrintTaskVO) => {
  detailLoading.value = true
  detailData.value = null
  detailVisible.value = true
  try {
    const res = await printingApi.getTask(record.taskId)
    if (res.data) {
      detailData.value = res.data
    }
  } catch (error) {
    console.warn('[打印任务] 获取任务详情失败', error)
    message.error('获取任务详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const handleDetailClose = () => {
  detailVisible.value = false
  detailData.value = null
}

// ── 取消任务 ────────────────────────────────────────────
const handleCancelTask = (record: PrintTaskVO) => {
  Modal.confirm({
    title: '确认取消',
    content: `确定要取消任务 "${record.taskCode}" 吗？`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '返回',
    centered: true,
    async onOk() {
      try {
        await printingApi.cancelTask(record.taskId)
        message.success('任务已取消')
        fetchData()
      } catch (err) {
        console.warn('[打印任务] 取消任务失败', err)
        message.error('取消任务失败')
      }
    }
  })
}

// ── 确认截图 ────────────────────────────────────────────
const handleConfirmScreenshot = (record: PrintTaskVO) => {
  Modal.confirm({
    title: '确认截图',
    content: `确认任务 "${record.taskCode}" 的截图结果是否正常？`,
    okText: '确认正常',
    cancelText: '返回',
    centered: true,
    async onOk() {
      try {
        await printingApi.confirmScreenshot(record.taskId)
        message.success('截图已确认')
        fetchData()
      } catch (err) {
        console.warn('[打印任务] 确认截图失败', err)
        message.error('确认截图失败')
      }
    }
  })
}

// ── 重新截图（超时后重试）────────────────────────────────
const handleRetryScreenshot = (record: PrintTaskVO) => {
  Modal.confirm({
    title: '重新截图',
    content: `截图任务已超时，是否重新调起截图？打印已正常执行，无需重新打印。`,
    okText: '重新截图',
    cancelText: '返回',
    centered: true,
    async onOk() {
      try {
        await printingApi.retryScreenshot(record.screenshotId)
        message.success('截图任务已重置，请重新截图')
        fetchData()
      } catch (err) {
        console.warn('[打印任务] 重试截图失败', err)
        message.error('重试截图失败')
      }
    }
  })
}

// ── 执行打印链 ──────────────────────────────────────────
const fetchChains = async () => {
  chainLoading.value = true
  try {
    const res = await printingApi.getChains({ page: 1, size: 999 })
    if (res.data) {
      chainOptions.value = res.data.records
    }
  } catch (err) {
    console.warn('[打印任务] 加载打印链列表失败', err)
    message.error('加载打印链列表失败')
  } finally {
    chainLoading.value = false
  }
}

const handleOpenExecuteChain = () => {
  executeVisible.value = true
  Object.assign(executeForm, {
    chainId: undefined as any,
    dataJson: '',
    pageCode: undefined,
    documentType: undefined,
    documentNo: undefined,
    documentId: undefined
  })
  nextTick(() => {
    executeFormRef.value?.resetFields()
  })
  fetchChains()
}

const handleExecuteChain = async () => {
  try {
    await executeFormRef.value?.validate()
  } catch {
    return
  }
  executeLoading.value = true
  try {
    const payload: PrintExecuteRequest = {
      chainId: executeForm.chainId,
      dataJson: executeForm.dataJson
    }
    if (executeForm.pageCode) payload.pageCode = executeForm.pageCode
    if (executeForm.documentType) payload.documentType = executeForm.documentType
    if (executeForm.documentId) payload.documentId = executeForm.documentId
    if (executeForm.documentNo) payload.documentNo = executeForm.documentNo

    await printingApi.executeChain(payload)
    message.success('打印链执行成功')
    executeVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[打印任务] 执行打印链失败', err)
    message.error('执行打印链失败')
  } finally {
    executeLoading.value = false
  }
}

const handleExecuteClose = () => {
  executeVisible.value = false
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', fetchData)()
  }
}

// ── 生命周期 ────────────────────────────────────────────
onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  startAutoRefresh()

  countdownTimer = setInterval(() => {
    if (autoRefreshEnabled.value && autoRefreshCountdown.value > 0) {
      autoRefreshCountdown.value--
    }
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearTimeout(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

function startAutoRefresh() {
  refreshTimer = setTimeout(async () => {
    if (!autoRefreshEnabled.value) {
      startAutoRefresh()
      return
    }
    await fetchData()
    autoRefreshCountdown.value = 30
    startAutoRefresh()
  }, 30000)
}

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.task-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.task-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.task-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.task-page-header-right {
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

.task-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.task-management > :deep(.vxe-table-list-container) {
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
  min-width: 0;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-pending { background: linear-gradient(135deg, #f0f5ff 0%, #d6e4ff 100%); }
.stat-queued { background: linear-gradient(135deg, #e6f7ff 0%, #91d5ff 100%); }
.stat-printing { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-failed { background: linear-gradient(135deg, #fff2f0 0%, #ffccc7 100%); }

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

/* 响应式 */
@media (max-width: 1024px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 30%; min-width: 140px; }
}
@media (max-width: 768px) {
  .stat-card { flex: 1 1 45%; }
}
</style>
