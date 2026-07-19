<template>
  <ErrorBoundary>
    <PageContainer title="系统参数">
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="配置类型">
            <a-select
              v-model:value="queryForm.configType"
              placeholder="全部类型"
              allow-clear
              style="width: 140px"
              @change="handleSearch"
            >
              <a-select-option
                v-for="t in configTypes"
                :key="t.code"
                :value="t.code"
              >
                {{ t.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="配置分组">
            <a-select
              v-model:value="queryForm.configGroup"
              placeholder="全部分组"
              allow-clear
              style="width: 140px"
              @change="handleSearch"
            >
              <a-select-option
                v-for="g in configGroups"
                :key="g.code"
                :value="g.code"
              >
                {{ g.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleSearch"
              >
                <template #icon>
                  <SearchOutlined />
                </template>
                查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ClearOutlined />
                </template>
                重置
              </a-button>
              <a-button @click="handleRefresh">
                <template #icon>
                  <ReloadOutlined />
                </template>
                刷新缓存
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>
      <div class="table-toolbar">
        <a-button
          type="primary"
          @click="handleAdd"
        >
          <template #icon>
            <PlusOutlined />
          </template>
          新增配置
        </a-button>
        <a-button
          danger
          :disabled="!selectedKeys.length"
          @click="handleBatchDelete"
        >
          <template #icon>
            <DeleteOutlined />
          </template>
          批量删除
        </a-button>
      </div>
      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          :row-selection="{ selectedRowKeys: selectedKeys, onChange: (keys: any[]) => selectedKeys = keys }"
          size="small"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'configValue'">
              <span style="max-width: 200px; display: inline-block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ record.configValue }}</span>
            </template>
            <template v-if="column.key === 'enabled'">
              <a-switch
                :checked="record.enabled"
                disabled
                size="small"
              />
            </template>
            <template v-if="column.key === 'systemConfig'">
              <a-tag :color="record.systemConfig ? 'blue' : 'default'">
                {{ record.systemConfig ? '系统' : '自定义' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                type="link"
                size="small"
                danger
                :disabled="record.systemConfig"
                @click="handleDelete(record)"
              >
                删除
              </a-button>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>

    <a-modal
      v-model:open="editVisible"
      :title="editingId ? '编辑配置' : '新增配置'"
      width="600px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form
        :model="editForm"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="配置键"
              required
            >
              <a-input
                v-model:value="editForm.configKey"
                placeholder="请输入配置键"
                :disabled="!!editingId"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="配置名称"
              required
            >
              <a-input
                v-model:value="editForm.configName"
                placeholder="请输入配置名称"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item
          label="配置值"
          required
        >
          <a-textarea
            v-model:value="editForm.configValue"
            :rows="3"
            placeholder="请输入配置值"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="配置类型">
              <a-select v-model:value="editForm.configType">
                <a-select-option
                  v-for="t in configTypes"
                  :key="t.code"
                  :value="t.code"
                >
                  {{ t.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="配置分组">
              <a-select v-model:value="editForm.configGroup">
                <a-select-option
                  v-for="g in configGroups"
                  :key="g.code"
                  :value="g.code"
                >
                  {{ g.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="值类型">
              <a-select v-model:value="editForm.valueType">
                <a-select-option value="string">
                  字符串
                </a-select-option>
                <a-select-option value="number">
                  数字
                </a-select-option>
                <a-select-option value="boolean">
                  布尔值
                </a-select-option>
                <a-select-option value="json">
                  JSON
                </a-select-option>
                <a-select-option value="list">
                  列表
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="editForm.description"
            :rows="2"
            placeholder="请输入描述"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="默认值">
              <a-input
                v-model:value="editForm.defaultValue"
                placeholder="默认值"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否启用">
              <a-switch v-model:checked="editForm.enabled" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, ClearOutlined, ReloadOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { configApi, type SystemConfig } from '@/api/config'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<SystemConfig[]>([])
const selectedKeys = ref<number[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` })
const queryForm = reactive<{ configType?: string; configGroup?: string }>({})
const configTypes = ref<{ code: string; name: string }[]>([])
const configGroups = ref<{ code: string; name: string }[]>([])

const editVisible = ref(false)
const editingId = ref<number | null>(null)
const editForm = reactive<Partial<SystemConfig>>({
  configKey: '',
  configName: '',
  configValue: '',
  configType: 'system',
  configGroup: 'basic',
  valueType: 'string',
  description: '',
  defaultValue: '',
  enabled: true
})

const columns: any[] = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '配置键', dataIndex: 'configKey', key: 'configKey', width: 180 },
  { title: '配置名称', dataIndex: 'configName', key: 'configName', width: 140 },
  { title: '配置值', dataIndex: 'configValue', key: 'configValue', ellipsis: true },
  { title: '类型', dataIndex: 'configType', key: 'configType', width: 80 },
  { title: '分组', dataIndex: 'configGroup', key: 'configGroup', width: 80 },
  { title: '启用', dataIndex: 'enabled', key: 'enabled', width: 60 },
  { title: '属性', dataIndex: 'systemConfig', key: 'systemConfig', width: 70 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 160 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await configApi.getPage({
      ...queryForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (result && result.records) {
      tableData.value = result.records
      pagination.total = result.total
    } else if (Array.isArray(result)) {
      tableData.value = result
      pagination.total = result.length
    }
  } catch (e) {
    message.error('查询失败')
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    configTypes.value = (await configApi.getConfigTypes()) || []
    configGroups.value = (await configApi.getConfigGroups()) || []
  } catch (e) {
    // 静默失败
  }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() {
  queryForm.configType = undefined
  queryForm.configGroup = undefined
  handleSearch()
}
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function handleAdd() {
  editingId.value = null
  editForm.configKey = ''
  editForm.configName = ''
  editForm.configValue = ''
  editForm.configType = 'system'
  editForm.configGroup = 'basic'
  editForm.valueType = 'string'
  editForm.description = ''
  editForm.defaultValue = ''
  editForm.enabled = true
  editVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(editForm, {
    configKey: record.configKey,
    configName: record.configName,
    configValue: record.configValue,
    configType: record.configType,
    configGroup: record.configGroup,
    valueType: record.valueType,
    description: record.description,
    defaultValue: record.defaultValue,
    enabled: record.enabled
  })
  editVisible.value = true
}

async function handleSave() {
  if (!editForm.configKey || !editForm.configValue) {
    message.warning('请填写配置键和配置值')
    return
  }
  saving.value = true
  try {
    await configApi.save(editForm)
    message.success('保存成功')
    editVisible.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除配置 "${record.configKey}" 吗？`,
    onOk: async () => {
      await configApi.delete(record.configKey)
      message.success('删除成功')
      loadData()
    }
  })
}

function handleBatchDelete() {
  if (!selectedKeys.value.length) return
  Modal.confirm({
    title: '确认批量删除',
    content: `确定要删除选中的 ${selectedKeys.value.length} 条配置吗？`,
    onOk: async () => {
      // 后端批量删除需要 configKey，但选择的是 id；写一个简单实现
      for (const id of selectedKeys.value) {
        const item = tableData.value.find(r => r.id === id)
        if (item) await configApi.delete(item.configKey)
      }
      message.success('批量删除成功')
      selectedKeys.value = []
      loadData()
    }
  })
}

async function handleRefresh() {
  try {
    await configApi.refreshCache()
    message.success('缓存已刷新')
  } catch (e) {
    message.error('刷新失败')
  }
}

onMounted(() => { loadOptions(); loadData() })
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.table-toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.table-area { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
</style>
