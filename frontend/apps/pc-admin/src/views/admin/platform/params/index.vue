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
      </div>
    </template>

    <a-card :bordered="false">
      <a-table
        :data-source="list"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a @click="handleEdit(record)">编辑</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="editVisible"
      title="编辑参数"
      width="500px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form
        :model="editForm"
        layout="vertical"
      >
        <a-form-item label="参数值">
          <a-textarea
            v-model:value="editForm.paramValue"
            :rows="4"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea
            v-model:value="editForm.remark"
            :rows="2"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const list = ref<any[]>([])
const editingRecord = ref<any>(null)

const editForm = reactive({
  paramValue: '',
  remark: '',
})

const columns = [
  { title: '参数名称', dataIndex: 'paramName', key: 'paramName' },
  { title: '参数键', dataIndex: 'paramKey', key: 'paramKey', width: 200 },
  { title: '参数值', dataIndex: 'paramValue', key: 'paramValue', ellipsis: true, width: 300 },
  { title: '系统内置', dataIndex: 'builtin', key: 'builtin', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 60 },
]

function handleEdit(record: any) {
  editingRecord.value = record
  editForm.paramValue = record.paramValue
  editForm.remark = record.remark || ''
  editVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    await request.post('/config/save-value', {
      configKey: editingRecord.value?.paramKey,
      configValue: editForm.paramValue,
    })
    message.success('参数已更新')
  } catch {
    message.error('参数更新失败')
  } finally {
    editVisible.value = false
    editingRecord.value = null
    saving.value = false
    fetchData()
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/config/list')
    list.value = res?.records || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
