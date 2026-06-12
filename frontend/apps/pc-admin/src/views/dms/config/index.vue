<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>DMS / 系统配置</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>系统配置</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>

    <template #filter>
      <SearchBar :fields="searchFields" :loading="loading" @search="handleSearch" @reset="handleReset" />
    </template>

    <template #default>
      <div class="page-body">
        <SkeletonTable v-if="loading && dataList.length === 0" :columns="columns.length" :rows="8" />
        <a-table
          v-else
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
            <a-empty description="暂无系统配置">
              <a-button size="small" @click="wms.fetchData">刷新</a-button>
            </a-empty>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'configValue'">
              <span class="config-value-text">{{ record.configValue }}</span>
            </template>
            <template v-if="column.dataIndex === 'createTime'">
              {{ formatDateTime(record.createTime) }}
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-space :size="4">
                <a-tooltip title="编辑">
                  <a-button v-permission="'dms:config:edit'" type="link" size="small" @click="handleEdit(record as any)"><EditOutlined /></a-button>
                </a-tooltip>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </template>
  </PageContainer></ErrorBoundary>

  <!-- 编辑配置弹窗 -->
  <a-modal
    v-model:open="modalVisible"
    title="编辑配置"
    width="600px"
    :confirm-loading="modalLoading"
    @ok="handleModalOk"
    @cancel="handleModalCancel"
  >
    <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
      <a-form-item label="配置键" name="configKey">
        <a-input v-model:value="formState.configKey" size="small" disabled />
      </a-form-item>
      <a-form-item label="配置值" name="configValue">
        <a-textarea v-model:value="formState.configValue" :rows="4" size="small" placeholder="请输入配置值" />
      </a-form-item>
      <a-form-item label="描述" name="configDesc">
        <a-input v-model:value="formState.configDesc" size="small" disabled />
      </a-form-item>
      <a-form-item label="作用域" name="scope">
        <a-input v-model:value="formState.scope" size="small" disabled />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import SkeletonTable from '@/components/Skeleton/SkeletonTable.vue'
import { configApi, type DmsConfig } from '@/api/dms/config'
import { useWmsTable } from '@/composables/useWmsTable'
import {
  ReloadOutlined, EditOutlined, SyncOutlined,
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[DMS配置]', err) }

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    return d.toLocaleString('zh-CN')
  } catch { return dateStr }
}

const columns = [
  { title: '配置键', dataIndex: 'configKey', width: 180 },
  { title: '配置值', dataIndex: 'configValue', width: 300, ellipsis: true },
  { title: '描述', dataIndex: 'configDesc', width: 200, ellipsis: true },
  { title: '作用域', dataIndex: 'scope', width: 120 },
  { title: '操作', dataIndex: 'action', width: 80, fixed: 'right' },
] as any

const searchFields: SearchField[] = [
  { name: 'configKey', label: '配置键', type: 'input', placeholder: '请输入配置键' },
]

const wms = useWmsTable({
  fetchFn: configApi.list as any,
  defaultPageSize: 20,
  refreshInterval: 30,
  shortcuts: { f5: 'refresh' },
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown } = wms

// ── 编辑 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const currentEditKey = ref('')
const formRef = ref<FormInstance>()
const formState = reactive<Record<string, any>>({
  configKey: '',
  configValue: '',
  configDesc: '',
  scope: '',
})
const formRules: Record<string, any[]> = {
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
}

function handleEdit(record: DmsConfig) {
  currentEditKey.value = record.configKey
  Object.assign(formState, { ...record })
  modalVisible.value = true
}

async function handleModalOk() {
  try { await formRef.value?.validate() } catch { return }
  modalLoading.value = true
  try {
    await configApi.update(currentEditKey.value, { configValue: formState.configValue })
    message.success('更新成功')
    modalVisible.value = false
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || '更新失败')
  } finally {
    modalLoading.value = false
  }
}

function handleModalCancel() { modalVisible.value = false }

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
.config-value-text { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 12px; word-break: break-all; }
:deep(.ant-input-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }

@media print {
  .page-header__right .shortcut-hints,
  .auto-refresh-badge,
  .update-time { display: none !important; }
}
</style>
