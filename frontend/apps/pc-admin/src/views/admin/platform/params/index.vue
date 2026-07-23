<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">
                首页
              </router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>平台参数</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            平台参数
          </h2>
        </div>
        <div class="page-header-right">
          <a-popconfirm
            title="刷新全部参数缓存?"
            @confirm="refreshCache"
          >
            <a-button
              size="small"
              style="margin-right:8px"
            >
              <template #icon>
                <ThunderboltOutlined />
              </template>
              刷新缓存
            </a-button>
          </a-popconfirm>
          <a-button
            size="small"
            :loading="loading"
            @click="fetchData"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <!-- 过滤区（类型/分组走后端过滤，关键字前端过滤） -->
      <a-form
        layout="inline"
        style="margin-bottom:16px"
      >
        <a-form-item label="配置类型">
          <a-select
            v-model:value="query.configType"
            placeholder="全部类型"
            style="width:150px"
            allow-clear
            @change="fetchData"
          >
            <a-select-option
              v-for="t in typeOptions"
              :key="t.code"
              :value="t.code"
            >
              {{ t.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="配置分组">
          <a-select
            v-model:value="query.configGroup"
            placeholder="全部分组"
            style="width:150px"
            allow-clear
            @change="fetchData"
          >
            <a-select-option
              v-for="g in groupOptions"
              :key="g.code"
              :value="g.code"
            >
              {{ g.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关键字">
          <a-input
            v-model:value="query.keyword"
            placeholder="参数键/参数名称"
            allow-clear
            style="width:200px"
          />
        </a-form-item>
      </a-form>

      <a-table
        :data-source="pagedList"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'configType'">
            <a-tag :color="TYPE_COLORS[record.configType] || 'default'">
              {{ typeName(record.configType) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'systemConfig'">
            <a-tag :color="record.systemConfig ? 'purple' : 'default'">
              {{ record.systemConfig ? '内置' : '自定义' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="handleEdit(record as PlatformConfigItem)">编辑</a>
              <a-divider type="vertical" />
              <a @click="openLogs(record as PlatformConfigItem)">变更日志</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑参数弹窗 -->
    <a-modal
      v-model:open="editVisible"
      title="编辑参数"
      width="500px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-descriptions
        :column="1"
        size="small"
        style="margin-bottom:16px"
      >
        <a-descriptions-item label="参数键">
          {{ editingRecord?.configKey }}
        </a-descriptions-item>
        <a-descriptions-item label="参数名称">
          {{ editingRecord?.configName || '-' }}
        </a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical">
        <a-form-item label="参数值">
          <a-textarea
            v-model:value="editForm.configValue"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 变更日志弹窗 -->
    <a-modal
      v-model:open="logsVisible"
      :title="`变更日志 - ${logsRecord?.configKey || ''}`"
      width="800px"
      :footer="null"
    >
      <a-table
        :data-source="logs"
        :columns="logColumns"
        :loading="logsLoading"
        row-key="logId"
        :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'changeType'">
            <a-tag>{{ record.changeType || '-' }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { platformConfigApi, type PlatformConfigItem, type ConfigChangeLogItem } from '@/api/admin'

const TYPE_COLORS: Record<string, string> = {
  system: 'blue',
  security: 'red',
  business: 'green',
  notification: 'orange',
  integration: 'purple'
}

const loading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const allRows = ref<PlatformConfigItem[]>([])
const editingRecord = ref<PlatformConfigItem | null>(null)
const typeOptions = ref<{ code: string; name: string }[]>([])
const groupOptions = ref<{ code: string; name: string }[]>([])

const logsVisible = ref(false)
const logsLoading = ref(false)
const logs = ref<ConfigChangeLogItem[]>([])
const logsRecord = ref<PlatformConfigItem | null>(null)

const query = reactive({
  configType: undefined as string | undefined,
  configGroup: undefined as string | undefined,
  keyword: '',
})

const editForm = reactive({
  configValue: '',
})

const paginationState = reactive({ current: 1, pageSize: 20 })

// 关键字前端过滤（后端仅支持 configType/configGroup 过滤）
const filteredList = computed(() => {
  const kw = query.keyword.trim().toLowerCase()
  if (!kw) return allRows.value
  return allRows.value.filter(r =>
    (r.configKey || '').toLowerCase().includes(kw) ||
    (r.configName || '').toLowerCase().includes(kw)
  )
})

const pagination = computed(() => ({
  current: paginationState.current,
  pageSize: paginationState.pageSize,
  total: filteredList.value.length,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

const pagedList = computed(() => {
  const start = (paginationState.current - 1) * paginationState.pageSize
  return filteredList.value.slice(start, start + paginationState.pageSize)
})

const columns = [
  { title: '参数名称', dataIndex: 'configName', key: 'configName', width: 180, ellipsis: true },
  { title: '参数键', dataIndex: 'configKey', key: 'configKey', width: 220, ellipsis: true },
  { title: '参数值', dataIndex: 'configValue', key: 'configValue', ellipsis: true, width: 280 },
  { title: '类型', dataIndex: 'configType', key: 'configType', width: 100 },
  { title: '分组', dataIndex: 'configGroup', key: 'configGroup', width: 100 },
  { title: '内置', dataIndex: 'systemConfig', key: 'systemConfig', width: 80 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 140 },
]

const logColumns = [
  { title: '操作类型', dataIndex: 'changeType', key: 'changeType', width: 100 },
  { title: '旧值', dataIndex: 'oldValue', key: 'oldValue', ellipsis: true },
  { title: '新值', dataIndex: 'newValue', key: 'newValue', ellipsis: true },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 110 },
  { title: '操作时间', dataIndex: 'operateTime', key: 'operateTime', width: 170 },
]

function typeName(code: string | undefined): string {
  if (!code) return '-'
  return typeOptions.value.find(t => t.code === code)?.name || code
}

function handleTableChange(pag: any) {
  paginationState.current = pag.current
  paginationState.pageSize = pag.pageSize
}

function handleEdit(record: PlatformConfigItem) {
  editingRecord.value = record
  editForm.configValue = record.configValue ?? ''
  editVisible.value = true
}

async function handleSave() {
  if (!editingRecord.value) return
  saving.value = true
  try {
    await platformConfigApi.saveValue(editingRecord.value.configKey, editForm.configValue)
    message.success('参数已更新')
    editVisible.value = false
    editingRecord.value = null
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '参数更新失败')
  } finally {
    saving.value = false
  }
}

async function openLogs(record: PlatformConfigItem) {
  logsRecord.value = record
  logsVisible.value = true
  logsLoading.value = true
  try {
    const res = await platformConfigApi.changeLogs(record.configKey)
    logs.value = Array.isArray(res) ? res : []
  } catch {
    logs.value = []
  } finally {
    logsLoading.value = false
  }
}

async function refreshCache() {
  try {
    await platformConfigApi.refreshCache()
    message.success('缓存刷新成功')
  } catch (e: any) {
    message.error(e?.message || '缓存刷新失败')
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await platformConfigApi.list({
      configType: query.configType,
      configGroup: query.configGroup,
    })
    allRows.value = res?.records || []
    paginationState.current = 1
  } catch {
    allRows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  fetchData()
  try {
    const [types, groups] = await Promise.all([platformConfigApi.types(), platformConfigApi.groups()])
    typeOptions.value = Array.isArray(types) ? types : []
    groupOptions.value = Array.isArray(groups) ? groups : []
  } catch (e) {
    console.warn('[平台参数] 类型/分组选项获取失败', e)
  }
})
</script>
