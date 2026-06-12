<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>DMS / 渠道管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>渠道管理</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增渠道</a-button>
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
            <a-empty description="暂无渠道数据">
              <a-space>
                <a-button size="small" type="primary" @click="handleAdd">新增渠道</a-button>
                <a-button size="small" @click="wms.fetchData">刷新</a-button>
              </a-space>
            </a-empty>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'channelType'">
              <a-tag :color="channelTypeMap[record.channelType]?.color || 'default'">
                {{ channelTypeMap[record.channelType]?.text || record.channelType }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'createTime'">
              {{ formatDateTime(record.createTime) }}
            </template>
            <template v-if="column.dataIndex === 'status'">
              <a-switch
                :checked="record.status === 1"
                :loading="(record as any)._statusLoading"
                size="small"
                @change="(checked: boolean) => handleStatusChange(record as any, checked)"
              />
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-space :size="4">
                <a-tooltip title="编辑">
                  <a-button v-permission="'dms:channel:edit'" type="link" size="small" @click="handleEdit(record as any)"><EditOutlined /></a-button>
                </a-tooltip>
                <a-tooltip title="删除">
                  <a-button v-permission="'dms:channel:delete'" type="link" size="small" danger @click="wms.handleDelete(record as any, '确定要删除该渠道吗？')"><DeleteOutlined /></a-button>
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
          <a-form-item label="渠道编码" name="channelCode">
            <a-input v-model:value="formState.channelCode" size="small" placeholder="请输入渠道编码" :disabled="isEdit" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="渠道名称" name="channelName">
            <a-input v-model:value="formState.channelName" size="small" placeholder="请输入渠道名称" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="渠道类型" name="channelType">
            <a-select v-model:value="formState.channelType" size="small" placeholder="请选择渠道类型">
              <a-select-option :value="0">线上</a-select-option>
              <a-select-option :value="1">线下</a-select-option>
              <a-select-option :value="2">API</a-select-option>
              <a-select-option :value="3">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="优先级" name="priority">
            <a-input-number v-model:value="formState.priority" :min="0" :max="999" size="small" style="width:100%" placeholder="优先级" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea v-model:value="formState.remark" :rows="2" size="small" placeholder="请输入备注" />
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
import { channelApi, type DmsChannel } from '@/api/dms/channel'
import { useWmsTable } from '@/composables/useWmsTable'
import {
  PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, SyncOutlined,
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[DMS渠道]', err) }

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    return d.toLocaleString('zh-CN')
  } catch { return dateStr }
}

// 行选择状态
const selectedRowKeys = ref<(string | number)[]>([])
const rowSelection = {
  selectedRowKeys,
  onChange: (keys: (string | number)[]) => { selectedRowKeys.value = keys },
} as any
function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的渠道')
    return
  }
  wms.handleDelete({ id: selectedRowKeys.value as any }, `确定要删除选中的 ${selectedRowKeys.value.length} 个渠道吗？`)
}

const channelTypeMap: Record<number, { text: string; color: string }> = {
  0: { text: '线上', color: 'blue' },
  1: { text: '线下', color: 'green' },
  2: { text: 'API', color: 'purple' },
  3: { text: '其他', color: 'default' },
}

const columns = [
  { title: '渠道编码', dataIndex: 'channelCode', width: 120 },
  { title: '渠道名称', dataIndex: 'channelName', width: 150 },
  { title: '渠道类型', dataIndex: 'channelType', width: 90 },
  { title: '状态', dataIndex: 'status', width: 70 },
  { title: '优先级', dataIndex: 'priority', width: 70 },
  { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', width: 120, fixed: 'right' },
] as any

const searchFields: SearchField[] = [
  { name: 'channelCode', label: '渠道编码', type: 'input', placeholder: '请输入渠道编码' },
  { name: 'channelName', label: '渠道名称', type: 'input', placeholder: '请输入渠道名称' },
]

const wms = useWmsTable({
  fetchFn: channelApi.page as any,
  deleteFn: channelApi.updateStatus as any,
  defaultPageSize: 20,
  refreshInterval: 30,
  shortcuts: { f5: 'refresh', ctrlN: 'add' },
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown } = wms

// ── 新增/编辑 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const modalTitle = computed(() => (isEdit.value ? '编辑渠道' : '新增渠道'))
const formRef = ref<FormInstance>()
const formState = reactive<Record<string, any>>({
  id: 0, channelCode: '', channelName: '', channelType: 0, priority: 0, remark: '',
})
const formRules: Record<string, any[]> = {
  channelCode: [{ required: true, message: '请输入渠道编码', trigger: 'blur' }],
  channelName: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
}

function handleAdd() {
  isEdit.value = false
  Object.assign(formState, { id: 0, channelCode: '', channelName: '', channelType: 0, priority: 0, remark: '' })
  modalVisible.value = true
}

function handleEdit(record: DmsChannel) {
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
      await channelApi.update(payload.id, payload)
      message.success('更新成功')
    } else {
      await channelApi.create(payload)
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

// ── 状态切换 ──
async function handleStatusChange(record: DmsChannel, checked: boolean) {
  const newStatus = checked ? 1 : 0
  ;(record as any)._statusLoading = true
  try {
    await channelApi.updateStatus(record.id, newStatus)
    message.success(checked ? '已启用' : '已禁用')
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || '操作失败')
  } finally {
    ;(record as any)._statusLoading = false
  }
}

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
