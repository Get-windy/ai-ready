<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>WMS / 移库管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>移库管理</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button v-permission="'wms:move:create'" type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增移库单</a-button>
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
        <a-skeleton active :loading="loading && dataList.length === 0">
        <a-table
          :dataSource="dataList"
          :columns="columns"
          :loading="loading"
          :pagination="pagination"
          rowKey="id"
          size="small"
          bordered
          @change="handleTableChange"
        >
          <template #emptyText>
            <div class="empty-state-wrapper">
              <template v-if="hasError">
                <WarningOutlined class="empty-state-icon" style="color: #faad14" />
                <p class="empty-state-text">加载失败</p>
                <a-button type="primary" size="small" @click="wms.debounce('refresh', wms.fetchData)" class="empty-state-action">
                  <ReloadOutlined /> 重试
                </a-button>
              </template>
              <template v-else>
                <InboxOutlined class="empty-state-icon" />
                <p class="empty-state-text">暂无数据</p>
                <a-button v-permission="'wms:move:create'" type="primary" size="small" @click="handleAdd" class="empty-state-action">
                  <PlusOutlined /> 新增移库单
                </a-button>
              </template>
            </div>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="statusMap[record.status]?.color || 'default'">{{ statusMap[record.status]?.text || record.status }}</a-tag>
            </template>
            <template v-if="column.dataIndex === 'progress'">
              <span>{{ record.movedQty || 0 }} / {{ record.totalQty || 0 }}</span>
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-space :size="4">
                <a-tooltip title="查看"><a-button v-permission="'wms:move:view'" type="link" size="small" @click="handleView(record as any)"><EyeOutlined /></a-button></a-tooltip>
                <a-tooltip title="开始移库" v-if="record.status === 0">
                  <a-button v-permission="'wms:move:start'" type="link" size="small" @click="handleStart(record as any)"><PlayCircleOutlined /></a-button>
                </a-tooltip>
                <a-tooltip title="执行移库" v-if="record.status === 1">
                  <a-button v-permission="'wms:move:execute'" type="link" size="small" @click="handleExecute(record as any)"><CheckCircleOutlined /></a-button>
                </a-tooltip>
                <a-tooltip title="删除" v-if="record.status === 0">
                  <a-button v-permission="'wms:move:delete'" type="link" size="small" danger @click="wms.handleDelete(record as any, '确定要删除该移库单吗？')"><DeleteOutlined /></a-button>
                </a-tooltip>
              </a-space>
            </template>
          </template>
        </a-table>
        </a-skeleton>
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
          <a-form-item label="仓库" name="warehouseName">
            <a-input v-model:value="formState.warehouseName" size="small" placeholder="请输入仓库名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="移库原因" name="moveReason">
            <a-input v-model:value="formState.moveReason" size="small" placeholder="请输入移库原因" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="源货位" name="sourceLocationCode">
            <a-input v-model:value="formState.sourceLocationCode" size="small" placeholder="请输入源货位编号" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="目标货位" name="targetLocationCode">
            <a-input v-model:value="formState.targetLocationCode" size="small" placeholder="请输入目标货位编号" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="总数" name="totalQty">
            <a-input-number v-model:value="formState.totalQty" :min="0" size="small" style="width:100%" placeholder="总数" />
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
import { ref, reactive, computed, watch, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { moveApi, type WmsMoveTask } from '@/api/wms/move'
import { useWmsTable } from '@/composables/useWmsTable'
import {
  PlusOutlined, ReloadOutlined, EyeOutlined, DeleteOutlined, SyncOutlined,
  PlayCircleOutlined, CheckCircleOutlined,
  WarningOutlined, InboxOutlined,
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[WMS移库]', err) }
const hasError = ref(false)

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'orange' },
  1: { text: '移库中', color: 'blue' },
  2: { text: '已完成', color: 'green' },
  3: { text: '已取消', color: 'red' },
  4: { text: '异常', color: 'red' },
}

const columns = [
  { title: '任务编号', dataIndex: 'taskNo', width: 150 },
  { title: '仓库', dataIndex: 'warehouseName', width: 120 },
  { title: '源货位', dataIndex: 'sourceLocationCode', width: 120 },
  { title: '目标货位', dataIndex: 'targetLocationCode', width: 120 },
  { title: '进度', dataIndex: 'progress', width: 100 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '移库原因', dataIndex: 'moveReason', width: 120, ellipsis: true },
  { title: '操作人', dataIndex: 'operatorName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', width: 180, fixed: 'right' },
] as any

const searchFields: SearchField[] = [
  { name: 'taskNo', label: '任务编号', type: 'input', placeholder: '请输入任务编号' },
  { name: 'sourceLocationCode', label: '源货位', type: 'input', placeholder: '请输入源货位编号' },
  { name: 'targetLocationCode', label: '目标货位', type: 'input', placeholder: '请输入目标货位编号' },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '待处理', value: 0 }, { label: '移库中', value: 1 }, { label: '已完成', value: 2 }, { label: '已取消', value: 3 }, { label: '异常', value: 4 },
  ]},
]

const wms = useWmsTable({
  fetchFn: moveApi.page as any,
  deleteFn: moveApi.remove,
  defaultPageSize: 20,
  refreshInterval: 30,
  shortcuts: { f5: 'refresh', ctrlN: 'add' },
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown } = wms

// ── 新增/编辑 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const modalTitle = computed(() => (isEdit.value ? '编辑移库单' : '新增移库单'))
const formRef = ref<FormInstance>()
const formState = reactive<Record<string, any>>({
  id: 0, warehouseName: '', moveReason: '', sourceLocationCode: '', targetLocationCode: '', totalQty: 0, remark: '',
})
const formRules: Record<string, any[]> = {
  sourceLocationCode: [{ required: true, message: '请输入源货位编号', trigger: 'blur' }],
  targetLocationCode: [{ required: true, message: '请输入目标货位编号', trigger: 'blur' }],
}

// ── 表单脏状态跟踪 ──
const formDirty = ref(false)
watch(() => JSON.stringify(formState), () => { if (modalVisible.value && !formDirty.value) formDirty.value = true })

function handleAdd() {
  isEdit.value = false
  formDirty.value = false
  Object.assign(formState, {
    id: 0, warehouseName: '', moveReason: '', sourceLocationCode: '', targetLocationCode: '', totalQty: 0, remark: '',
  })
  modalVisible.value = true
}

function handleEdit(record: WmsMoveTask) {
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
      await moveApi.update(payload)
      message.success('更新成功')
    } else {
      await moveApi.save(payload)
      message.success('创建成功')
    }
    formDirty.value = false
    modalVisible.value = false
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    modalLoading.value = false
  }
}

function handleModalCancel() { formDirty.value = false; modalVisible.value = false }

function handleView(record: WmsMoveTask) {
  message.info(`查看移库单: ${record.taskNo}`)
}

// ── 工作流操作 ──
async function handleStart(record: WmsMoveTask) {
  try {
    await moveApi.startMove(record.id)
    message.success('移库已开始')
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || '操作失败')
  }
}

async function handleExecute(record: WmsMoveTask) {
  Modal.confirm({
    title: '执行移库',
    content: `确定要执行移库单"${record.taskNo}"吗？`,
    onOk: async () => {
      try {
        await moveApi.executeMove(record.id)
        message.success('移库执行成功')
        wms.fetchData()
      } catch (err: any) {
        message.error(err?.message || '操作失败')
      }
    },
  })
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

// ── 生命周期（Composable 已处理定时器/快捷键） ──
onUnmounted(() => { /* useWmsTable handles cleanup */ })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none; }
.page-body { padding: 0; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-picker-small), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }

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

/* ── 空状态 ──────────────────────── */
.empty-state-wrapper { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.empty-state-icon { font-size: 48px; color: #d9d9d9; }
.empty-state-text { color: #999; margin-top: 12px; }
.empty-state-action { margin-top: 12px; }

/* ── vxe-table 表头 2px 边框 ─────── */
:deep(.vxe-table .vxe-header--row) { border-top: 2px solid #e8e8e8; }
</style>
