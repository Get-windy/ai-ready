<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>模板管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">模板管理</h2>
        </div>
        <div class="page-header-right">
          <a-button type="primary" size="small" @click="openForm()">
            <template #icon><PlusOutlined /></template>
            新增模板
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" :pagination="false" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag>{{ typeLabel(record.type) }}</a-tag>
          </template>
          <template v-if="column.key === 'enabled'">
            <a-switch :checked="record.enabled" size="small" @change="(checked: boolean) => toggleEnabled(record, checked)" />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="openForm(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除?" @confirm="handleDelete(record)">
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formVisible" :title="editingId ? '编辑模板' : '新增模板'" @ok="handleSave" :confirm-loading="saving" destroy-on-close width="720px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="模板名称" required>
          <a-input v-model:value="form.name" placeholder="请输入模板名称" />
        </a-form-item>
        <a-form-item label="模板编码" required>
          <a-input v-model:value="form.code" placeholder="请输入模板编码" />
        </a-form-item>
        <a-form-item label="模板类型" required>
          <a-select v-model:value="form.type">
            <a-select-option value="entity">实体类</a-select-option>
            <a-select-option value="controller">Controller</a-select-option>
            <a-select-option value="service">Service</a-select-option>
            <a-select-option value="mapper">Mapper</a-select-option>
            <a-select-option value="frontend">前端页面</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="版本号">
          <a-input v-model:value="form.version" placeholder="例如 1.0" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="2" />
        </a-form-item>
        <a-form-item label="模板内容">
          <a-textarea v-model:value="form.content" :rows="6" placeholder="使用 ${variable} 作为占位符" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

interface DevTemplateItem {
  id: number
  name: string
  code: string
  type: string
  content?: string
  description?: string
  version?: string
  enabled: boolean
  createTime?: string
  updateTime?: string
}

const loading = ref(false)
const list = ref<DevTemplateItem[]>([])
const formVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = ref<Partial<DevTemplateItem>>({
  name: '',
  code: '',
  type: 'entity',
  version: '1.0',
  description: '',
  content: '',
  enabled: true,
})

const typeLabels: Record<string, string> = {
  entity: '实体类', controller: 'Controller', service: 'Service',
  mapper: 'Mapper', frontend: '前端页面',
}
function typeLabel(type: string) { return typeLabels[type] || type }

const columns = [
  { title: '模板名称', dataIndex: 'name', key: 'name' },
  { title: '模板编码', dataIndex: 'code', key: 'code', width: 120 },
  { title: '类型', key: 'type', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '版本', dataIndex: 'version', key: 'version', width: 70 },
  { title: '启用', key: 'enabled', width: 60 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 140 },
]

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/codegen/template/list', { params: { page: 1, size: 50 } })
    list.value = res?.records || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openForm(record?: Record<string, any>) {
  if (record) {
    editingId.value = record.id as number
    form.value = { ...(record as DevTemplateItem) }
  } else {
    editingId.value = null
    form.value = { name: '', code: '', type: 'entity', version: '1.0', description: '', content: '', enabled: true }
  }
  formVisible.value = true
}

async function handleSave() {
  if (!form.value.name || !form.value.code) {
    message.warning('请填写模板名称和编码')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put('/codegen/template/update', form.value)
      message.success('更新成功')
    } else {
      await request.post('/codegen/template/create', form.value)
      message.success('创建成功')
    }
    formVisible.value = false
    await fetchData()
  } catch {
    message.error('操作失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: Record<string, any>) {
  try {
    await request.delete('/codegen/template/delete/' + record.id)
    message.success('删除成功')
    await fetchData()
  } catch {
    message.error('删除失败')
  }
}

async function toggleEnabled(record: Record<string, any>, checked: boolean) {
  try {
    const item = record as DevTemplateItem
    const url = checked ? '/codegen/template/activate/' + item.id : '/codegen/template/deactivate/' + item.id
    await request.post(url)
    record.enabled = checked
  } catch {
    message.error('操作失败')
  }
}

onMounted(fetchData)
</script>
