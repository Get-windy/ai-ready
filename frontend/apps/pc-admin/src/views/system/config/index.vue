<template>
  <div class="config-management">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :table-key="'system-config-list'"
      :filter-fields="filterFields"
      :show-search="false"
      add-text="新增配置"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDeleteConfirm"
      @batch-delete="handleBatchDelete"
      @refresh="fetchData"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <a-button @click="handleRefreshCache">
          <template #icon><SyncOutlined /></template>
          刷新缓存
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'groupName'">
          <a-tag color="blue">{{ record.groupName }}</a-tag>
        </template>

        <template v-else-if="column.key === 'configValue'">
          <span
            class="config-value"
            :class="{ sensitive: isSensitiveKey(record.configKey) }"
          >
            {{ isSensitiveKey(record.configKey) ? '******' : record.configValue }}
          </span>
          <a-tooltip title="复制" v-if="!isSensitiveKey(record.configKey)">
            <a-button
              type="link"
              size="small"
              :style="{ padding: '0 4px' }"
              @click="handleCopy(record.configValue)"
            >
              <CopyOutlined />
            </a-button>
          </a-tooltip>
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">
              编辑
            </a-button>
            <a-button type="link" size="small" danger @click="handleDeleteConfirm(record)">
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </TableList>

    <!-- 配置表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      width="550px"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="配置键" name="configKey">
          <a-input
            v-model:value="formState.configKey"
            placeholder="请输入配置键，如 sys.upload.path"
            :disabled="isEdit"
          />
        </a-form-item>
        <a-form-item label="配置值" name="configValue">
          <a-textarea
            v-model:value="formState.configValue"
            placeholder="请输入配置值"
            :rows="4"
          />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea
            v-model:value="formState.description"
            placeholder="请输入配置描述"
            :rows="2"
          />
        </a-form-item>
        <a-form-item label="分组" name="groupName">
          <a-input
            v-model:value="formState.groupName"
            placeholder="请输入分组名称，如 SYS/UPLOAD/EMAIL"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  SyncOutlined,
  CopyOutlined
} from '@ant-design/icons-vue'
import TableList, { type FilterField } from '@/components/TableList/TableList.vue'
import { configApi, type ConfigInfo } from '@/api/config'

// 搜索表单
const searchForm = reactive({
  configKey: '',
  groupName: undefined as string | undefined
})

const groupOptions = ref<string[]>([])

// 表格数据
const tableData = ref<ConfigInfo[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 表格列
const columns: any[] = [
  { title: '配置键', dataIndex: 'configKey', width: 200, ellipsis: true },
  { title: '配置值', key: 'configValue', width: 300, ellipsis: true },
  { title: '描述', dataIndex: 'description', width: 200, ellipsis: true },
  { title: '分组', key: 'groupName', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

// 筛选字段
const filterFields = computed<FilterField[]>(() => [
  { key: 'configKey', label: '配置键', type: 'input', placeholder: '请输入配置键' },
  { key: 'groupName', label: '分组', type: 'select', options: groupOptions.value.map(g => ({ label: g, value: g })) },
])

// 弹窗
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const modalTitle = computed(() => isEdit.value ? '编辑配置' : '新增配置')

const formState = reactive({
  id: 0,
  configKey: '',
  configValue: '',
  description: '',
  groupName: ''
})

const formRules = {
  configKey: { required: true, message: '请输入配置键', trigger: 'blur' },
  configValue: { required: true, message: '请输入配置值', trigger: 'blur' },
  groupName: { required: true, message: '请输入分组', trigger: 'blur' }
}

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const res = await configApi.getPage({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    message.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

const fetchGroups = async () => {
  try {
    const res = await configApi.getGroups()
    if (res.data) {
      groupOptions.value = res.data
    }
  } catch {
    // 忽略
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, { configKey: '', groupName: undefined })
  handleSearch()
}

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { configKey: '', groupName: undefined })
  } else {
    Object.assign(searchForm, filters)
  }
  pagination.current = 1
  fetchData()
}

// 分页变化
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, { id: 0, configKey: '', configValue: '', description: '', groupName: '' })
  modalVisible.value = true
}

// 编辑
const handleEdit = (record: ConfigInfo) => {
  isEdit.value = true
  Object.assign(formState, {
    id: record.id,
    configKey: record.configKey,
    configValue: record.configValue,
    description: record.description,
    groupName: record.groupName
  })
  modalVisible.value = true
}

// 删除
const handleDeleteConfirm = (record: ConfigInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除配置 "${record.configKey}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await configApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch {
        message.error('删除失败')
      }
    }
  })
}

// 批量删除
const handleBatchDelete = (deleteKeys?: number[]) => {
  const ids = deleteKeys || selectedRowKeys.value
  if (!ids.length) return
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${ids.length} 个配置吗？`,
    async onOk() {
      try {
        await configApi.batchDelete(ids)
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchData()
      } catch {
        message.error('批量删除失败')
      }
    }
  })
}

// 刷新缓存
const handleRefreshCache = async () => {
  try {
    await configApi.refreshCache()
    message.success('缓存刷新成功')
  } catch {
    message.error('缓存刷新失败')
  }
}

// 复制
const handleCopy = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

// 敏感键
const isSensitiveKey = (key: string) => {
  const sensitiveKeywords = ['password', 'secret', 'token', 'key', 'private']
  return sensitiveKeywords.some(k => key.toLowerCase().includes(k))
}

// 提交表单
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (isEdit.value) {
      await configApi.update(formState as any)
      message.success('更新成功')
    } else {
      await configApi.create(formState as any)
      message.success('创建成功')
    }

    modalVisible.value = false
    fetchData()
  } catch (error) {
    message.error('操作失败')
  } finally {
    modalLoading.value = false
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

onMounted(() => {
  fetchData()
  fetchGroups()
})
</script>

<style scoped>
.config-management {
  padding: 0;
}

.config-value.sensitive {
  color: #999;
  font-style: italic;
}
</style>
