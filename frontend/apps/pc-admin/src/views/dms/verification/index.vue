<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>DMS / 核验管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>核验管理</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="debounceClick('refresh', handleRefresh)">
            <ReloadOutlined /> 刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          </span>
        </div>
      </div>
    </template>

    <template #default>
      <div class="page-body">
        <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
          <!-- Tab 1: 绑定记录 -->
          <a-tab-pane key="binding" tab="绑定记录">
            <SearchBar :fields="bindingSearchFields" :loading="bindingLoading" @search="handleBindingSearch" @reset="handleBindingReset" />
            <div style="margin-top:12px;">
              <SkeletonTable v-if="bindingLoading && bindingDataList.length === 0" :columns="bindingColumns.length" :rows="8" />
              <a-table
                v-else
                :dataSource="bindingDataList"
                :columns="bindingColumns"
                :loading="bindingLoading"
                :pagination="bindingPagination"
                rowKey="id"
                size="small"
                bordered
                @change="handleBindingTableChange"
              >
                <template #emptyText>
                  <a-empty description="暂无绑定记录">
                    <a-button size="small" @click="fetchBindingData">刷新</a-button>
                  </a-empty>
                </template>
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'bindTime' || column.dataIndex === 'handoverTime'">
                    {{ formatDateTime(record[column.dataIndex]) }}
                  </template>
                  <template v-if="column.dataIndex === 'status'">
                    <a-tag :color="record.status === 1 ? 'blue' : 'default'">
                      {{ record.status === 1 ? '绑定中' : '已交车' }}
                    </a-tag>
                  </template>
                  <template v-if="column.dataIndex === 'action'">
                    <a-space :size="4">
                      <a-tooltip title="查看详情">
                        <a-button v-permission="'dms:verification:view'" type="link" size="small" @click="handleViewBinding(record)"><EyeOutlined /></a-button>
                      </a-tooltip>
                      <a-tooltip v-if="record.status === 1" title="交车">
                        <a-button v-permission="'dms:verification:handover'" type="link" size="small" @click="handleHandover(record)"><SwapRightOutlined /></a-button>
                      </a-tooltip>
                    </a-space>
                  </template>
                </template>
              </a-table>
            </div>
          </a-tab-pane>

          <!-- Tab 2: 预警记录 -->
          <a-tab-pane key="alert" tab="预警记录">
            <SearchBar :fields="alertSearchFields" :loading="alertLoading" @search="handleAlertSearch" @reset="handleAlertReset" />
            <div style="margin-top:12px;">
              <SkeletonTable v-if="alertLoading && alertDataList.length === 0" :columns="alertColumns.length" :rows="8" />
              <a-table
                v-else
                :dataSource="alertDataList"
                :columns="alertColumns"
                :loading="alertLoading"
                :pagination="alertPagination"
                rowKey="id"
                size="small"
                bordered
                @change="handleAlertTableChange"
              >
                <template #emptyText>
                  <a-empty description="暂无预警记录">
                    <a-button size="small" @click="fetchAlertData">刷新</a-button>
                  </a-empty>
                </template>
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'createTime'">
                    {{ formatDateTime(record.createTime) }}
                  </template>
                  <template v-if="column.dataIndex === 'alertType'">
                    <a-tag :color="alertTypeMap[record.alertType]?.color || 'default'">
                      {{ alertTypeMap[record.alertType]?.text || record.alertType }}
                    </a-tag>
                  </template>
                  <template v-if="column.dataIndex === 'alertLevel'">
                    <a-tag :color="alertLevelMap[record.alertLevel]?.color || 'default'">
                      {{ alertLevelMap[record.alertLevel]?.text || record.alertLevel }}
                    </a-tag>
                  </template>
                  <template v-if="column.dataIndex === 'handleStatus'">
                    <a-tag :color="handleStatusMap[record.handleStatus]?.color || 'default'">
                      {{ handleStatusMap[record.handleStatus]?.text || record.handleStatus }}
                    </a-tag>
                  </template>
                  <template v-if="column.dataIndex === 'action'">
                    <a-button v-permission="'dms:verification:handle-alert'" type="link" size="small" :disabled="record.handleStatus === 2" @click="handleAlertAction(record)">
                      处理
                    </a-button>
                  </template>
                </template>
              </a-table>
            </div>
          </a-tab-pane>

          <!-- Tab 3: 巡检记录 -->
          <a-tab-pane key="inspection" tab="巡检记录">
            <SearchBar :fields="inspectionSearchFields" :loading="inspectionLoading" @search="handleInspectionSearch" @reset="handleInspectionReset" />
            <div style="margin-top:12px;">
              <SkeletonTable v-if="inspectionLoading && inspectionDataList.length === 0" :columns="inspectionColumns.length" :rows="8" />
              <a-table
                v-else
                :dataSource="inspectionDataList"
                :columns="inspectionColumns"
                :loading="inspectionLoading"
                :pagination="inspectionPagination"
                rowKey="id"
                size="small"
                bordered
                @change="handleInspectionTableChange"
              >
                <template #emptyText>
                  <a-empty description="暂无巡检记录">
                    <a-button size="small" @click="fetchInspectionData">刷新</a-button>
                  </a-empty>
                </template>
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'inspectionTime'">
                    {{ formatDateTime(record.inspectionTime) }}
                  </template>
                  <template v-if="column.dataIndex === 'inspectionType'">
                    <a-tag :color="inspectionTypeMap[record.inspectionType]?.color || 'default'">
                      {{ inspectionTypeMap[record.inspectionType]?.text || record.inspectionType }}
                    </a-tag>
                  </template>
                  <template v-if="column.dataIndex === 'result'">
                    <a-tag :color="record.result === 1 ? 'green' : 'red'">
                      {{ record.result === 1 ? '通过' : '不通过' }}
                    </a-tag>
                  </template>
                  <template v-if="column.dataIndex === 'action'">
                    <a-button v-permission="'dms:verification:review'" type="link" size="small" @click="handleInspectionReview(record)">审核</a-button>
                  </template>
                </template>
              </a-table>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>
    </template>
  </PageContainer></ErrorBoundary>

  <!-- 交车弹窗 -->
  <a-modal v-model:open="handoverVisible" title="交车操作" width="480px" :confirm-loading="handoverLoading" @ok="handleHandoverOk" @cancel="handoverVisible = false">
    <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="骑手">
        <span>{{ handoverRecord?.riderName }}</span>
      </a-form-item>
      <a-form-item label="车牌号">
        <span>{{ handoverRecord?.plateNo || handoverRecord?.vehiclePlate }}</span>
      </a-form-item>
      <a-form-item label="交车里程">
        <a-input-number v-model:value="handoverForm.mileage" :min="0" size="small" style="width:100%" placeholder="请输入交车里程" />
      </a-form-item>
      <a-form-item label="交车地点">
        <a-input v-model:value="handoverForm.location" size="small" placeholder="请输入交车地点" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 预警处理弹窗 -->
  <a-modal v-model:open="alertHandleVisible" title="处理预警" width="480px" :confirm-loading="alertHandleLoading" @ok="handleAlertHandleOk" @cancel="alertHandleVisible = false">
    <a-form ref="alertHandleFormRef" :model="alertHandleForm" :rules="alertHandleRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="预警类型">
        <a-tag :color="alertTypeMap[alertHandleRecord?.alertType]?.color">{{ alertTypeMap[alertHandleRecord?.alertType]?.text }}</a-tag>
      </a-form-item>
      <a-form-item label="预警内容">
        <span>{{ alertHandleRecord?.alertContent || alertHandleRecord?.alertMsg }}</span>
      </a-form-item>
      <a-form-item label="处理结果" name="handleStatus">
        <a-select v-model:value="alertHandleForm.handleStatus" size="small" placeholder="请选择处理结果">
          <a-select-option :value="1">已确认</a-select-option>
          <a-select-option :value="2">已忽略</a-select-option>
          <a-select-option :value="3">已处理</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="alertHandleForm.remark" :rows="2" size="small" placeholder="请输入处理备注" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 巡检审核弹窗 -->
  <a-modal v-model:open="inspectionReviewVisible" title="审核巡检" width="480px" :confirm-loading="inspectionReviewLoading" @ok="handleInspectionReviewOk" @cancel="inspectionReviewVisible = false">
    <a-form ref="inspectionReviewFormRef" :model="inspectionReviewForm" :rules="inspectionReviewRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="巡检类型">
        <a-tag>{{ inspectionTypeMap[inspectionReviewRecord?.inspectionType]?.text || '-' }}</a-tag>
      </a-form-item>
      <a-form-item label="审核结果" name="result">
        <a-select v-model:value="inspectionReviewForm.result" size="small" placeholder="请选择审核结果">
          <a-select-option :value="1">通过</a-select-option>
          <a-select-option :value="0">不通过</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="审核人" name="reviewer">
        <a-input v-model:value="inspectionReviewForm.reviewer" size="small" placeholder="请输入审核人" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="inspectionReviewForm.remark" :rows="2" size="small" placeholder="请输入审核备注" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import SkeletonTable from '@/components/Skeleton/SkeletonTable.vue'
import { verificationApi } from '@/api/dms/verification'
import {
  ReloadOutlined, EyeOutlined, SwapRightOutlined, SyncOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'

function handleError(err: any) { console.warn('[DMS核验]', err) }

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    return d.toLocaleString('zh-CN')
  } catch { return dateStr }
}

const router = useRouter()

// ── 映射表 ──────────────────────────────────────────
const alertTypeMap: Record<number, { text: string; color: string }> = {
  1: { text: '人车分离', color: 'red' },
  2: { text: '异常滞留', color: 'orange' },
  3: { text: '绑定超时', color: 'gold' },
}

const alertLevelMap: Record<number, { text: string; color: string }> = {
  1: { text: '低', color: 'green' },
  2: { text: '中', color: 'orange' },
  3: { text: '高', color: 'red' },
}

const handleStatusMap: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'orange' },
  1: { text: '已确认', color: 'blue' },
  2: { text: '已忽略', color: 'default' },
  3: { text: '已处理', color: 'green' },
}

const inspectionTypeMap: Record<number, { text: string; color: string }> = {
  1: { text: '出车前检查', color: 'blue' },
  2: { text: '收车后检查', color: 'purple' },
  3: { text: '随机抽检', color: 'cyan' },
  4: { text: '定期检查', color: 'green' },
}

// ── Tabs ──────────────────────────────────────────
const activeTab = ref('binding')

function handleRefresh() {
  switch (activeTab.value) {
    case 'binding': fetchBindingData(); break
    case 'alert': fetchAlertData(); break
    case 'inspection': fetchInspectionData(); break
  }
}

function handleTabChange(key: string) {
  switch (key) {
    case 'binding':
      if (bindingDataList.value.length === 0) fetchBindingData()
      break
    case 'alert':
      if (alertDataList.value.length === 0) fetchAlertData()
      break
    case 'inspection':
      if (inspectionDataList.value.length === 0) fetchInspectionData()
      break
  }
}

// ── 绑定记录 ──────────────────────────────────────────
const bindingLoading = ref(false)
const bindingDataList = ref<any[]>([])
const bindingPagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showQuickJumper: true, pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`,
})
const bindingSearchParams = reactive<Record<string, any>>({})
const lastUpdateTime = ref('')

const bindingColumns = [
  { title: '骑手姓名', dataIndex: 'riderName', width: 100 },
  { title: '车牌号', dataIndex: 'plateNo', width: 110 },
  { title: '绑定时间', dataIndex: 'bindTime', width: 170 },
  { title: '交车时间', dataIndex: 'handoverTime', width: 170 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '操作', dataIndex: 'action', width: 120, fixed: 'right' },
] as any

const bindingSearchFields: SearchField[] = [
  { name: 'riderName', label: '骑手姓名', type: 'input', placeholder: '请输入骑手姓名' },
  { name: 'plateNo', label: '车牌号', type: 'input', placeholder: '请输入车牌号' },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '绑定中', value: 1 }, { label: '已交车', value: 2 },
  ]},
]

async function fetchBindingData() {
  bindingLoading.value = true
  try {
    const res: any = await verificationApi.bindingPage({
      current: bindingPagination.current,
      pageSize: bindingPagination.pageSize,
      ...bindingSearchParams,
    })
    const data = res?.data ?? res
    if (data?.records) {
      bindingDataList.value = data.records
      bindingPagination.total = data.total || 0
    } else if (Array.isArray(data)) {
      bindingDataList.value = data
      bindingPagination.total = data.length
    } else {
      bindingDataList.value = []
      bindingPagination.total = 0
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    bindingDataList.value = []
    message.error('获取绑定数据失败')
  } finally {
    bindingLoading.value = false
  }
}

function handleBindingSearch(formData: Record<string, any>) {
  Object.assign(bindingSearchParams, formData)
  bindingPagination.current = 1
  fetchBindingData()
}

function handleBindingReset() {
  Object.keys(bindingSearchParams).forEach(k => { bindingSearchParams[k] = undefined })
  bindingPagination.current = 1
  fetchBindingData()
}

function handleBindingTableChange(pag: any) {
  bindingPagination.current = pag.current
  bindingPagination.pageSize = pag.pageSize
  fetchBindingData()
}

// ── 交车 ──────────────────────────────────────────
const handoverVisible = ref(false)
const handoverLoading = ref(false)
const handoverRecord = ref<any>(null)
const handoverForm = reactive({ mileage: undefined, location: '' })

function handleHandover(record: any) {
  handoverRecord.value = record
  handoverForm.mileage = undefined
  handoverForm.location = ''
  handoverVisible.value = true
}

async function handleHandoverOk() {
  if (!handoverRecord.value) return
  handoverLoading.value = true
  try {
    await verificationApi.bindingHandover(handoverRecord.value.id, {
      handoverMileage: handoverForm.mileage,
      handoverLocation: handoverForm.location,
    })
    message.success('交车成功')
    handoverVisible.value = false
    fetchBindingData()
  } catch (err: any) {
    message.error(err?.message || '交车失败')
  } finally {
    handoverLoading.value = false
  }
}

function handleViewBinding(record: any) {
  router.push({ path: `/dms/verification/binding/${record.id}` })
}

// ── 预警记录 ──────────────────────────────────────────
const alertLoading = ref(false)
const alertDataList = ref<any[]>([])
const alertPagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showQuickJumper: true, pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`,
})
const alertSearchParams = reactive<Record<string, any>>({})

const alertColumns = [
  { title: '预警类型', dataIndex: 'alertType', width: 100 },
  { title: '预警级别', dataIndex: 'alertLevel', width: 80 },
  { title: '预警内容', dataIndex: 'alertContent', ellipsis: true },
  { title: '处理状态', dataIndex: 'handleStatus', width: 90 },
  { title: '发生时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', dataIndex: 'action', width: 80, fixed: 'right' },
] as any

const alertSearchFields: SearchField[] = [
  { name: 'alertType', label: '预警类型', type: 'select', placeholder: '请选择类型', options: [
    { label: '人车分离', value: 1 }, { label: '异常滞留', value: 2 }, { label: '绑定超时', value: 3 },
  ]},
  { name: 'handleStatus', label: '处理状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '待处理', value: 0 }, { label: '已确认', value: 1 },
    { label: '已忽略', value: 2 }, { label: '已处理', value: 3 },
  ]},
]

async function fetchAlertData() {
  alertLoading.value = true
  try {
    const res: any = await verificationApi.alertPage({
      current: alertPagination.current,
      pageSize: alertPagination.pageSize,
      ...alertSearchParams,
    })
    const data = res?.data ?? res
    if (data?.records) {
      alertDataList.value = data.records
      alertPagination.total = data.total || 0
    } else if (Array.isArray(data)) {
      alertDataList.value = data
      alertPagination.total = data.length
    } else {
      alertDataList.value = []
      alertPagination.total = 0
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    alertDataList.value = []
    message.error('获取告警数据失败')
  } finally {
    alertLoading.value = false
  }
}

function handleAlertSearch(formData: Record<string, any>) {
  Object.assign(alertSearchParams, formData)
  alertPagination.current = 1
  fetchAlertData()
}

function handleAlertReset() {
  Object.keys(alertSearchParams).forEach(k => { alertSearchParams[k] = undefined })
  alertPagination.current = 1
  fetchAlertData()
}

function handleAlertTableChange(pag: any) {
  alertPagination.current = pag.current
  alertPagination.pageSize = pag.pageSize
  fetchAlertData()
}

// ── 预警处理 ──────────────────────────────────────────
const alertHandleVisible = ref(false)
const alertHandleLoading = ref(false)
const alertHandleRecord = ref<any>(null)
const alertHandleFormRef = ref<FormInstance>()
const alertHandleForm = reactive({ handleStatus: 3, remark: '' })
const alertHandleRules: Record<string, any[]> = {
  handleStatus: [{ required: true, message: '请选择处理结果', trigger: 'change' }],
}

function handleAlertAction(record: any) {
  alertHandleRecord.value = record
  alertHandleForm.handleStatus = 3
  alertHandleForm.remark = ''
  alertHandleVisible.value = true
}

async function handleAlertHandleOk() {
  try { await alertHandleFormRef.value?.validate() } catch { return }
  if (!alertHandleRecord.value) return
  alertHandleLoading.value = true
  try {
    await verificationApi.handleAlert(alertHandleRecord.value.id, {
      handleStatus: alertHandleForm.handleStatus,
      remark: alertHandleForm.remark,
    })
    message.success('处理成功')
    alertHandleVisible.value = false
    fetchAlertData()
  } catch (err: any) {
    message.error(err?.message || '处理失败')
  } finally {
    alertHandleLoading.value = false
  }
}

// ── 巡检记录 ──────────────────────────────────────────
const inspectionLoading = ref(false)
const inspectionDataList = ref<any[]>([])
const inspectionPagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showQuickJumper: true, pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`,
})
const inspectionSearchParams = reactive<Record<string, any>>({})

const inspectionColumns = [
  { title: '车牌号', dataIndex: 'plateNo', width: 110 },
  { title: '骑手姓名', dataIndex: 'riderName', width: 100 },
  { title: '巡检类型', dataIndex: 'inspectionType', width: 110 },
  { title: '结果', dataIndex: 'result', width: 70 },
  { title: '巡检时间', dataIndex: 'inspectionTime', width: 170 },
  { title: '操作', dataIndex: 'action', width: 80, fixed: 'right' },
] as any

const inspectionSearchFields: SearchField[] = [
  { name: 'plateNo', label: '车牌号', type: 'input', placeholder: '请输入车牌号' },
  { name: 'riderName', label: '骑手姓名', type: 'input', placeholder: '请输入骑手姓名' },
  { name: 'inspectionType', label: '巡检类型', type: 'select', placeholder: '请选择类型', options: [
    { label: '出车前检查', value: 1 }, { label: '收车后检查', value: 2 },
    { label: '随机抽检', value: 3 }, { label: '定期检查', value: 4 },
  ]},
]

async function fetchInspectionData() {
  inspectionLoading.value = true
  try {
    const res: any = await verificationApi.inspectionPage({
      current: inspectionPagination.current,
      pageSize: inspectionPagination.pageSize,
      ...inspectionSearchParams,
    })
    const data = res?.data ?? res
    if (data?.records) {
      inspectionDataList.value = data.records
      inspectionPagination.total = data.total || 0
    } else if (Array.isArray(data)) {
      inspectionDataList.value = data
      inspectionPagination.total = data.length
    } else {
      inspectionDataList.value = []
      inspectionPagination.total = 0
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    inspectionDataList.value = []
    message.error('获取验车数据失败')
  } finally {
    inspectionLoading.value = false
  }
}

function handleInspectionSearch(formData: Record<string, any>) {
  Object.assign(inspectionSearchParams, formData)
  inspectionPagination.current = 1
  fetchInspectionData()
}

function handleInspectionReset() {
  Object.keys(inspectionSearchParams).forEach(k => { inspectionSearchParams[k] = undefined })
  inspectionPagination.current = 1
  fetchInspectionData()
}

function handleInspectionTableChange(pag: any) {
  inspectionPagination.current = pag.current
  inspectionPagination.pageSize = pag.pageSize
  fetchInspectionData()
}

// ── 巡检审核 ──────────────────────────────────────────
const inspectionReviewVisible = ref(false)
const inspectionReviewLoading = ref(false)
const inspectionReviewRecord = ref<any>(null)
const inspectionReviewFormRef = ref<FormInstance>()
const inspectionReviewForm = reactive({ result: 1, reviewer: '', remark: '' })
const inspectionReviewRules: Record<string, any[]> = {
  result: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
}

function handleInspectionReview(record: any) {
  inspectionReviewRecord.value = record
  inspectionReviewForm.result = 1
  inspectionReviewForm.reviewer = ''
  inspectionReviewForm.remark = ''
  inspectionReviewVisible.value = true
}

async function handleInspectionReviewOk() {
  try { await inspectionReviewFormRef.value?.validate() } catch { return }
  if (!inspectionReviewRecord.value) return
  inspectionReviewLoading.value = true
  try {
    await verificationApi.reviewInspection(inspectionReviewRecord.value.id, {
      reviewResult: inspectionReviewForm.result,
      reviewer: inspectionReviewForm.reviewer,
      remark: inspectionReviewForm.remark,
    })
    message.success('审核完成')
    inspectionReviewVisible.value = false
    fetchInspectionData()
  } catch (err: any) {
    message.error(err?.message || '审核失败')
  } finally {
    inspectionReviewLoading.value = false
  }
}

// ── 防抖 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const autoRefreshCountdown = ref(0)
const loading = computed(() => {
  if (activeTab.value === 'binding') return bindingLoading.value
  if (activeTab.value === 'alert') return alertLoading.value
  if (activeTab.value === 'inspection') return inspectionLoading.value
  return false
})
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', handleRefresh)
  }
}

// ── 初始化 ──────────────────────────────────────────
import { onMounted, onUnmounted } from 'vue'
onMounted(() => {
  fetchBindingData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => { handleRefresh(); autoRefreshCountdown.value = 30 }, 30000)
  countdownTimer = setInterval(() => { if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value-- }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.page-body { padding: 0; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none; }
.shortcut-hints { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; user-select: none; }
.shortcut-hint { display: inline-flex; align-items: center; gap: 2px; padding: 1px 4px; border-radius: 3px; background: #f5f7fa; }
.shortcut-hint kbd { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 3px; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 11px; color: #606266; background: #fff; border: 1px solid #d0d5dd; border-radius: 3px; box-shadow: 0 1px 0 #d0d5dd; line-height: 18px; }
@media print {
  .page-header__right .shortcut-hints,
  .auto-refresh-badge,
  .update-time { display: none !important; }
}
</style>
