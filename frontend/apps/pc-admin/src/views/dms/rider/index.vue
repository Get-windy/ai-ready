<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>DMS / 骑手管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>骑手管理</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增骑手</a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          </span>
        </div>
      </div>
    </template>

    <template #filter>
      <SearchBar :fields="searchFields" :loading="loading" @search="handleSearch" @reset="handleReset" />
    </template>

    <template #default>
      <div class="page-body">
        <div v-if="selectedRowKeys.length > 0" class="batch-bar no-print">
          <span>已选择 {{ selectedRowKeys.length }} 项</span>
          <a-button size="small" danger @click="handleBatchDelete">批量删除</a-button>
          <a-button size="small" @click="selectedRowKeys = []">取消选择</a-button>
        </div>
        <SkeletonTable v-if="loading && dataList.length === 0" :columns="columns.length" :rows="8" />
        <a-table
          v-else
          :dataSource="dataList"
          :columns="columns"
          :loading="loading"
          :pagination="pagination"
          :rowSelection="rowSelection"
          rowKey="id"
          size="small"
          bordered
          @change="handleTableChange"
        >
          <template #emptyText>
            <a-empty description="暂无骑手数据">
              <a-space>
                <a-button size="small" type="primary" @click="handleAdd">新增骑手</a-button>
                <a-button size="small" @click="wms.fetchData">刷新</a-button>
              </a-space>
            </a-empty>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'riderType'">
              <a-tag :color="riderTypeMap[record.riderType]?.color || 'default'">
                {{ riderTypeMap[record.riderType]?.text || record.riderType }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'createTime'">
              {{ formatDateTime(record.createTime) }}
            </template>
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="statusMap[record.status]?.color || 'default'">
                {{ statusMap[record.status]?.text || record.status }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'verifyStatus'">
              <a-tag :color="verifyStatusMap[record.verifyStatus]?.color || 'default'">
                {{ verifyStatusMap[record.verifyStatus]?.text || record.verifyStatus }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-space :size="4">
                <a-tooltip title="查看">
                  <a-button v-permission="'dms:rider:view'" type="link" size="small" @click="handleView(record as any)"><EyeOutlined /></a-button>
                </a-tooltip>
                <a-tooltip title="编辑">
                  <a-button v-permission="'dms:rider:edit'" type="link" size="small" @click="handleEdit(record as any)"><EditOutlined /></a-button>
                </a-tooltip>
                <a-tooltip title="审核" v-if="record.verifyStatus === 0">
                  <a-button v-permission="'dms:rider:approve'" type="link" size="small" @click="handleApprove(record as any)"><CheckCircleOutlined /></a-button>
                </a-tooltip>
                <a-tooltip title="删除">
                  <a-button v-permission="'dms:rider:delete'" type="link" size="small" danger @click="wms.handleDelete(record as any, '确定要删除该骑手吗？')"><DeleteOutlined /></a-button>
                </a-tooltip>
              </a-space>
            </template>
          </template>

        </a-table>
      </div>
    </template>
  </PageContainer></ErrorBoundary>

  <!-- 新增/编辑弹窗 -->
  <a-modal
    v-model:open="modalVisible"
    :title="modalTitle"
    width="640px"
    :confirm-loading="modalLoading"
    @ok="handleModalOk"
    @cancel="handleModalCancel"
  >
    <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="真实姓名" name="realName">
            <a-input v-model:value="formState.realName" size="small" placeholder="请输入真实姓名" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="手机号" name="phone">
            <a-input v-model:value="formState.phone" size="small" placeholder="请输入手机号" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="骑手类型" name="riderType">
            <a-select v-model:value="formState.riderType" size="small" placeholder="请选择骑手类型">
              <a-select-option :value="0">全职</a-select-option>
              <a-select-option :value="1">兼职</a-select-option>
              <a-select-option :value="2">众包</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <a-select v-model:value="formState.status" size="small" placeholder="请选择状态">
              <a-select-option :value="0">离线</a-select-option>
              <a-select-option :value="1">在线</a-select-option>
              <a-select-option :value="2">忙碌</a-select-option>
              <a-select-option :value="3">休息</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea v-model:value="formState.remark" :rows="2" size="small" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 审核弹窗 -->
  <a-modal
    v-model:open="approveVisible"
    title="骑手审核"
    width="480px"
    :confirm-loading="approveLoading"
    @ok="handleApproveOk"
    @cancel="handleApproveCancel"
  >
    <a-form ref="approveFormRef" :model="approveState" :rules="approveRules" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
      <a-form-item label="骑手姓名">
        <span>{{ approveState.realName }}</span>
      </a-form-item>
      <a-form-item label="手机号">
        <span>{{ approveState.phone }}</span>
      </a-form-item>
      <a-form-item label="审核结果" name="verifyStatus">
        <a-radio-group v-model:value="approveState.verifyStatus">
          <a-radio :value="1">通过</a-radio>
          <a-radio :value="2">拒绝</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="approveState.remark" :rows="3" size="small" placeholder="审核备注（可选）" />
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
import { riderApi, type DmsRider } from '@/api/dms/rider'
import { useWmsTable } from '@/composables/useWmsTable'
import {
  PlusOutlined, ReloadOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SyncOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[DMS骑手]', err) }

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    return d.toLocaleString('zh-CN')
  } catch { return dateStr }
}

// 行选择
const selectedRowKeys = ref<(string | number)[]>([])
const rowSelection = {
  selectedRowKeys,
  onChange: (keys: (string | number)[]) => { selectedRowKeys.value = keys },
} as any
function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的骑手')
    return
  }
  wms.handleDelete({ id: selectedRowKeys.value.join(',') as any }, `确定要删除选中的 ${selectedRowKeys.value.length} 个骑手吗？`)
}

const riderTypeMap: Record<number, { text: string; color: string }> = {
  0: { text: '全职', color: 'blue' },
  1: { text: '兼职', color: 'orange' },
  2: { text: '众包', color: 'purple' },
}

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '离线', color: 'default' },
  1: { text: '在线', color: 'green' },
  2: { text: '忙碌', color: 'orange' },
  3: { text: '休息', color: 'blue' },
}

const verifyStatusMap: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'orange' },
  1: { text: '已通过', color: 'green' },
  2: { text: '已拒绝', color: 'red' },
}

const columns = [
  { title: '真实姓名', dataIndex: 'realName', width: 100 },
  { title: '手机号', dataIndex: 'phone', width: 130 },
  { title: '骑手类型', dataIndex: 'riderType', width: 90 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '审核状态', dataIndex: 'verifyStatus', width: 90 },
  { title: '评分', dataIndex: 'ratingScore', width: 70 },
  { title: '总订单', dataIndex: 'totalOrders', width: 70 },
  { title: '车辆', dataIndex: 'vehiclePlate', width: 110 },
  { title: '备注', dataIndex: 'remark', width: 130, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', width: 170, fixed: 'right' },
] as any

const searchFields: SearchField[] = [
  { name: 'realName', label: '真实姓名', type: 'input', placeholder: '请输入真实姓名' },
  { name: 'phone', label: '手机号', type: 'input', placeholder: '请输入手机号' },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '离线', value: 0 }, { label: '在线', value: 1 }, { label: '忙碌', value: 2 }, { label: '休息', value: 3 },
  ]},
  { name: 'riderType', label: '骑手类型', type: 'select', placeholder: '请选择类型', options: [
    { label: '全职', value: 0 }, { label: '兼职', value: 1 }, { label: '众包', value: 2 },
  ]},
]

const wms = useWmsTable({
  fetchFn: riderApi.page as any,
  deleteFn: riderApi.updateStatus as any,
  defaultPageSize: 20,
  refreshInterval: 30,
  shortcuts: { f5: 'refresh', ctrlN: 'add' },
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown } = wms

// ── 新增/编辑 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const modalTitle = computed(() => (isEdit.value ? '编辑骑手' : '新增骑手'))
const formRef = ref<FormInstance>()
const formState = reactive<Record<string, any>>({
  id: 0, realName: '', phone: '', riderType: 0, status: 0, remark: '',
})
const formRules: Record<string, any[]> = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度 2-20 个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  riderType: [{ required: true, message: '请选择骑手类型', trigger: 'change' }],
}

function handleAdd() {
  isEdit.value = false
  Object.assign(formState, { id: 0, realName: '', phone: '', riderType: 0, status: 0, remark: '' })
  modalVisible.value = true
}

function handleEdit(record: DmsRider) {
  isEdit.value = true
  Object.assign(formState, { ...record })
  modalVisible.value = true
}

async function handleModalOk() {
  try { await formRef.value?.validate() } catch { return }
  modalLoading.value = true
  try {
    const payload = { ...formState }
    if (isEdit.value) {
      await riderApi.update(payload.id, payload)
      message.success('更新成功')
    } else {
      await riderApi.create(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    modalLoading.value = false
  }
}

function handleModalCancel() { modalVisible.value = false }

// ── 查看 ──
function handleView(record: DmsRider) {
  message.info(`骑手信息: ${record.realName} (${record.phone})`)
}

// ── 审核 ──
const approveVisible = ref(false)
const approveLoading = ref(false)
const approveFormRef = ref<FormInstance>()
const approveState = reactive<Record<string, any>>({
  id: 0,
  realName: '',
  phone: '',
  verifyStatus: 1,
  remark: '',
})
const approveRules: Record<string, any[]> = {
  verifyStatus: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
}

function handleApprove(record: DmsRider) {
  Object.assign(approveState, {
    id: record.id,
    realName: record.realName,
    phone: record.phone,
    verifyStatus: 1,
    remark: '',
  })
  approveVisible.value = true
}

async function handleApproveOk() {
  try { await approveFormRef.value?.validate() } catch { return }
  approveLoading.value = true
  try {
    await riderApi.approve(approveState.id, {
      verifyStatus: approveState.verifyStatus,
      remark: approveState.remark || undefined,
    })
    message.success('审核完成')
    approveVisible.value = false
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || '审核失败')
  } finally {
    approveLoading.value = false
  }
}

function handleApproveCancel() { approveVisible.value = false }

function handleSearch(formData: Record<string, any>) {
  Object.assign(searchParams, formData)
  wms.handleSearch()
}

function handleReset() {
  Object.keys(searchParams).forEach(k => { searchParams[k] = undefined })
  wms.handleReset()
}

function handleTableChange(pag: any) {
  wms.handlePageChange(pag.current, pag.pageSize)
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none; }
.page-body { padding: 0; }
.batch-bar { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 4px; margin-bottom: 8px; font-size: 13px; }
.batch-bar span { flex: 1; color: #1890ff; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }
@media print {
  .page-header__right .shortcut-hints,
  .auto-refresh-badge,
  .update-time { display: none !important; }
}
</style>
