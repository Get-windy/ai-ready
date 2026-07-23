<template>
  <ARReportPage
    ref="reportRef"
    title="配送参数"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    row-key="configKey"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'configValue'">
        <span class="config-value-text">{{ record.configValue }}</span>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-button
          type="link"
          size="small"
          @click="handleEdit(record as any)"
        >
          编辑
        </a-button>
      </template>
    </template>
  </ARReportPage>

  <!-- ═══ 编辑参数弹窗 ═══ -->
  <a-modal
    v-model:open="editModalVisible"
    title="编辑配送参数"
    width="520px"
    :confirm-loading="editSubmitting"
    @ok="handleEditConfirm"
  >
    <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
      <a-form-item label="配置键">
        <a-input v-model:value="editForm.configKey" disabled />
      </a-form-item>
      <a-form-item label="配置值" required>
        <a-textarea
          v-model:value="editForm.configValue"
          :rows="3"
          placeholder="请输入配置值"
        />
      </a-form-item>
      <a-form-item label="描述">
        <a-input v-model:value="editForm.configDesc" disabled />
      </a-form-item>
      <a-form-item label="作用域">
        <a-input v-model:value="editForm.scope" disabled />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { configApi, type DmsConfig } from '@/api/dms/config'

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '配置键 / 描述', width: 200 }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '配置键', dataIndex: 'configKey', key: 'configKey', width: 200 },
  { title: '配置值', dataIndex: 'configValue', key: 'configValue', width: 260, ellipsis: true },
  { title: '描述', dataIndex: 'configDesc', key: 'configDesc', ellipsis: true },
  { title: '作用域', dataIndex: 'scope', key: 'scope', width: 100 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

const reportRef = ref<any>(null)

// ═══ 数据请求（后端 /dms/config/list 不分页，关键字前端过滤） ═══
async function fetcher(params: Record<string, any>) {
  const list = await configApi.list()
  const all = Array.isArray(list) ? list : []
  const keyword = String(params.keyword || '').trim().toLowerCase()
  if (!keyword) return all
  return all.filter(c =>
    (c.configKey || '').toLowerCase().includes(keyword) ||
    (c.configDesc || '').toLowerCase().includes(keyword)
  )
}

// ═══ 编辑 ═══
const editModalVisible = ref(false)
const editSubmitting = ref(false)
const editForm = reactive({ configKey: '', configValue: '', configDesc: '', scope: '' })

function handleEdit(record: DmsConfig) {
  Object.assign(editForm, {
    configKey: record.configKey,
    configValue: record.configValue,
    configDesc: record.configDesc,
    scope: record.scope
  })
  editModalVisible.value = true
}

async function handleEditConfirm() {
  if (!editForm.configValue?.trim()) {
    message.warning('请输入配置值')
    return
  }
  editSubmitting.value = true
  try {
    await configApi.update(editForm.configKey, { configValue: editForm.configValue })
    message.success('参数已更新')
    editModalVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[配送参数] 更新失败', e)
  } finally {
    editSubmitting.value = false
  }
}
</script>

<style scoped>
.config-value-text {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  word-break: break-all;
}
</style>
