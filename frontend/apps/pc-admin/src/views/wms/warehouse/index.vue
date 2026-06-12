<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>WMS / 仓库管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>仓库管理</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button v-permission="'wms:warehouse:create'" type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增仓库</a-button>
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
                <a-button v-permission="'wms:warehouse:create'" type="primary" size="small" @click="handleAdd" class="empty-state-action">
                  <PlusOutlined /> 新增仓库
                </a-button>
              </template>
            </div>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'warehouseType'">
              <a-tag :color="warehouseTypeMap[record.warehouseType]?.color || 'default'">
                {{ warehouseTypeMap[record.warehouseType]?.text || record.warehouseType }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'isWmsEnabled'">
              <a-tag :color="record.isWmsEnabled === 1 ? 'green' : 'default'">
                {{ record.isWmsEnabled === 1 ? '已启用' : '未启用' }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'capacityRate'">
              <span>{{ computeRate(record.usedCapacity, record.totalCapacity) }}</span>
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-space :size="4">
                <a-tooltip title="查看">
                  <a-button v-permission="'wms:warehouse:view'" type="link" size="small" @click="handleView(record as any)"><EyeOutlined /></a-button>
                </a-tooltip>
                <a-tooltip title="编辑">
                  <a-button v-permission="'wms:warehouse:edit'" type="link" size="small" @click="handleEdit(record as any)"><EditOutlined /></a-button>
                </a-tooltip>
                <a-tooltip title="删除">
                  <a-button v-permission="'wms:warehouse:delete'" type="link" size="small" danger @click="wms.handleDelete(record as any, '确定要删除该仓库吗？')"><DeleteOutlined /></a-button>
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
          <a-form-item label="仓库编码" name="warehouseCode">
            <a-input v-model:value="formState.warehouseCode" size="small" placeholder="请输入仓库编码" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="仓库名称" name="warehouseName">
            <a-input v-model:value="formState.warehouseName" size="small" placeholder="请输入仓库名称" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="仓库类型" name="warehouseType">
            <a-select v-model:value="formState.warehouseType" size="small" placeholder="请选择仓库类型">
              <a-select-option :value="0">普通仓库</a-select-option>
              <a-select-option :value="1">冷藏仓库</a-select-option>
              <a-select-option :value="2">冷冻仓库</a-select-option>
              <a-select-option :value="3">危险品仓库</a-select-option>
              <a-select-option :value="4">虚拟仓库</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="WMS启用的" name="isWmsEnabled">
            <a-switch v-model:checked="formState.isWmsEnabled" :checkedValue="1" :unCheckedValue="0" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="总容量" name="totalCapacity">
            <a-input-number v-model:value="formState.totalCapacity" :min="0" size="small" style="width:100%" placeholder="总容量" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="已用容量" name="usedCapacity">
            <a-input-number v-model:value="formState.usedCapacity" :min="0" size="small" style="width:100%" placeholder="已用容量" />
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
import { warehouseApi, type WmsWarehouse } from '@/api/wms/warehouse'
import { useWmsTable } from '@/composables/useWmsTable'
import {
  PlusOutlined, ReloadOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SyncOutlined,
  WarningOutlined, InboxOutlined,
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[WMS仓库]', err) }
const hasError = ref(false)

const warehouseTypeMap: Record<number, { text: string; color: string }> = {
  0: { text: '普通仓库', color: 'blue' },
  1: { text: '冷藏仓库', color: 'cyan' },
  2: { text: '冷冻仓库', color: 'purple' },
  3: { text: '危险品仓库', color: 'red' },
  4: { text: '虚拟仓库', color: 'orange' },
}

const columns = [
  { title: '仓库编码', dataIndex: 'warehouseCode', width: 130 },
  { title: '仓库名称', dataIndex: 'warehouseName', width: 160 },
  { title: '仓库类型', dataIndex: 'warehouseType', width: 110 },
  { title: '库区数', dataIndex: 'zoneCount', width: 80 },
  { title: '货位数', dataIndex: 'locationCount', width: 80 },
  { title: '容量使用率', dataIndex: 'capacityRate', width: 100 },
  { title: 'WMS启用', dataIndex: 'isWmsEnabled', width: 90 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', dataIndex: 'action', width: 140, fixed: 'right' },
] as any

const searchFields: SearchField[] = [
  { name: 'warehouseCode', label: '仓库编码', type: 'input', placeholder: '请输入仓库编码' },
  { name: 'warehouseName', label: '仓库名称', type: 'input', placeholder: '请输入仓库名称' },
  { name: 'warehouseType', label: '仓库类型', type: 'select', placeholder: '请选择仓库类型', options: [
    { label: '普通仓库', value: 0 }, { label: '冷藏仓库', value: 1 },
    { label: '冷冻仓库', value: 2 }, { label: '危险品仓库', value: 3 }, { label: '虚拟仓库', value: 4 },
  ]},
]

function computeRate(used: number, total: number): string {
  if (!total) return '0%'
  return ((used / total) * 100).toFixed(1) + '%'
}

const wms = useWmsTable({
  fetchFn: warehouseApi.page as any,
  deleteFn: warehouseApi.remove,
  defaultPageSize: 20,
  refreshInterval: 30,
  shortcuts: { f5: 'refresh', ctrlN: 'add' },
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown, debounce } = wms

// ── 新增/编辑 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const modalTitle = computed(() => (isEdit.value ? '编辑仓库' : '新增仓库'))
const formRef = ref<FormInstance>()
const formState = reactive<Record<string, any>>({
  id: 0, warehouseCode: '', warehouseName: '', warehouseType: 0, isWmsEnabled: 0,
  totalCapacity: 0, usedCapacity: 0, remark: '',
})
const formRules: Record<string, any[]> = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
}

// ── 表单脏状态跟踪 ──
const formDirty = ref(false)
watch(() => JSON.stringify(formState), () => { if (modalVisible.value && !formDirty.value) formDirty.value = true })

function handleAdd() {
  isEdit.value = false
  formDirty.value = false
  Object.assign(formState, { id: 0, warehouseCode: '', warehouseName: '', warehouseType: 0, isWmsEnabled: 0, totalCapacity: 0, usedCapacity: 0, remark: '' })
  modalVisible.value = true
}

function handleEdit(record: WmsWarehouse) {
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
      await warehouseApi.update(payload)
      message.success('更新成功')
    } else {
      await warehouseApi.save(payload)
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

function handleModalCancel() {
  formDirty.value = false; modalVisible.value = false
}

// ── 查看 ──
function handleView(record: WmsWarehouse) {
  message.info(`查看仓库: ${record.warehouseName}`)
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
