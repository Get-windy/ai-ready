<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>WMS / 货位管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>货位管理</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button v-permission="'wms:location:create'" type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增货位</a-button>
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
                <a-button v-permission="'wms:location:create'" type="primary" size="small" @click="handleAdd" class="empty-state-action">
                  <PlusOutlined /> 新增货位
                </a-button>
              </template>
            </div>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="statusMap[record.status]?.color || 'default'">{{ statusMap[record.status]?.text || record.status }}</a-tag>
            </template>
            <template v-if="column.dataIndex === 'locationType'">
              <a-tag :color="locTypeMap[record.locationType]?.color || 'default'">{{ locTypeMap[record.locationType]?.text || record.locationType }}</a-tag>
            </template>
            <template v-if="column.dataIndex === 'isPickable'">
              <a-tag :color="record.isPickable === 1 ? 'green' : 'default'">{{ record.isPickable === 1 ? '可拣货' : '否' }}</a-tag>
            </template>
            <template v-if="column.dataIndex === 'isReceivable'">
              <a-tag :color="record.isReceivable === 1 ? 'green' : 'default'">{{ record.isReceivable === 1 ? '可收货' : '否' }}</a-tag>
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-space :size="4">
                <a-tooltip title="查看"><a-button v-permission="'wms:location:view'" type="link" size="small" @click="handleView(record as any)"><EyeOutlined /></a-button></a-tooltip>
                <a-tooltip title="编辑"><a-button v-permission="'wms:location:edit'" type="link" size="small" @click="handleEdit(record as any)"><EditOutlined /></a-button></a-tooltip>
                <a-tooltip title="删除"><a-button v-permission="'wms:location:delete'" type="link" size="small" danger @click="wms.handleDelete(record as any, '确定要删除该货位吗？')"><DeleteOutlined /></a-button></a-tooltip>
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
          <a-form-item label="货位编码" name="locationCode">
            <a-input v-model:value="formState.locationCode" size="small" placeholder="请输入货位编码" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="货位名称" name="locationName">
            <a-input v-model:value="formState.locationName" size="small" placeholder="请输入货位名称" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="货位类型" name="locationType">
            <a-select v-model:value="formState.locationType" size="small" placeholder="请选择货位类型">
              <a-select-option :value="0">存储位</a-select-option>
              <a-select-option :value="1">拣货位</a-select-option>
              <a-select-option :value="2">收货暂存</a-select-option>
              <a-select-option :value="3">发货暂存</a-select-option>
              <a-select-option :value="4">不良品区</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <a-select v-model:value="formState.status" size="small" placeholder="请选择状态">
              <a-select-option :value="0">空闲</a-select-option>
              <a-select-option :value="1">占用</a-select-option>
              <a-select-option :value="2">锁定</a-select-option>
              <a-select-option :value="3">禁用</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="最大容量" name="maxCapacity">
            <a-input-number v-model:value="formState.maxCapacity" :min="0" size="small" style="width:100%" placeholder="最大容量" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="已用容量" name="usedCapacity">
            <a-input-number v-model:value="formState.usedCapacity" :min="0" size="small" style="width:100%" placeholder="已用容量" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="最大重量" name="maxWeight">
            <a-input-number v-model:value="formState.maxWeight" :min="0" size="small" style="width:100%" placeholder="最大重量(kg)" />
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
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { locationApi, type WmsLocation } from '@/api/wms/warehouse'
import { useWmsTable } from '@/composables/useWmsTable'
import {
  PlusOutlined, ReloadOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SyncOutlined,
  WarningOutlined, InboxOutlined,
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[WMS货位]', err) }
const hasError = ref(false)

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '空闲', color: 'green' },
  1: { text: '占用', color: 'orange' },
  2: { text: '锁定', color: 'red' },
  3: { text: '禁用', color: 'default' },
}

const locTypeMap: Record<number, { text: string; color: string }> = {
  0: { text: '存储位', color: 'blue' },
  1: { text: '拣货位', color: 'purple' },
  2: { text: '收货暂存', color: 'cyan' },
  3: { text: '发货暂存', color: 'orange' },
  4: { text: '不良品区', color: 'red' },
}

const columns = [
  { title: '货位编码', dataIndex: 'locationCode', width: 140 },
  { title: '货位名称', dataIndex: 'locationName', width: 150 },
  { title: '货位类型', dataIndex: 'locationType', width: 100 },
  { title: '层级', dataIndex: 'locationLevel', width: 60 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '可拣货', dataIndex: 'isPickable', width: 70 },
  { title: '可收货', dataIndex: 'isReceivable', width: 70 },
  { title: '容量使用', dataIndex: 'usedCapacity', width: 80 },
  { title: '最大容量', dataIndex: 'maxCapacity', width: 80 },
  { title: '排序', dataIndex: 'sortOrder', width: 60 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', dataIndex: 'action', width: 140, fixed: 'right' },
] as any

const searchFields: SearchField[] = [
  { name: 'locationCode', label: '货位编码', type: 'input', placeholder: '请输入货位编码' },
  { name: 'locationName', label: '货位名称', type: 'input', placeholder: '请输入货位名称' },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '空闲', value: 0 }, { label: '占用', value: 1 }, { label: '锁定', value: 2 }, { label: '禁用', value: 3 },
  ]},
]

const wms = useWmsTable({
  fetchFn: locationApi.page as any,
  deleteFn: locationApi.remove,
  defaultPageSize: 20,
  refreshInterval: 30,
  shortcuts: { f5: 'refresh', ctrlN: 'add' },
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown } = wms

// ── 新增/编辑 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const modalTitle = computed(() => (isEdit.value ? '编辑货位' : '新增货位'))
const formRef = ref<FormInstance>()
const formState = reactive<Record<string, any>>({
  id: 0, locationCode: '', locationName: '', locationType: 0, status: 0,
  maxCapacity: 0, usedCapacity: 0, maxWeight: 0, isPickable: 0, isReceivable: 0, remark: '',
})
const formRules: Record<string, any[]> = {
  locationCode: [{ required: true, message: '请输入货位编码', trigger: 'blur' }],
  locationName: [{ required: true, message: '请输入货位名称', trigger: 'blur' }],
}

// ── 表单脏状态跟踪 ──
const formDirty = ref(false)
watch(() => JSON.stringify(formState), () => { if (modalVisible.value && !formDirty.value) formDirty.value = true })

function handleAdd() {
  isEdit.value = false
  formDirty.value = false
  Object.assign(formState, {
    id: 0, locationCode: '', locationName: '', locationType: 0, status: 0,
    maxCapacity: 0, usedCapacity: 0, maxWeight: 0, isPickable: 0, isReceivable: 0, remark: '',
  })
  modalVisible.value = true
}

function handleEdit(record: WmsLocation) {
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
      await locationApi.update(payload)
      message.success('更新成功')
    } else {
      await locationApi.save(payload)
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

function handleView(record: WmsLocation) {
  message.info(`查看货位: ${record.locationName}`)
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
