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
            <a-breadcrumb-item>模板管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            模板管理
          </h2>
        </div>
        <div class="page-header-right">
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

    <a-alert
      type="warning"
      show-icon
      style="margin-bottom:16px"
      message="代码生成模板（codegen）后端端点尚未实现，待后端补全。当前页对接导入模板（/import-templates）真实数据，支持预览字段结构、下载模板与删除。"
    />

    <a-card
      :bordered="false"
      title="导入模板"
    >
      <a-table
        :data-source="list"
        :columns="columns"
        :loading="loading"
        row-key="templateId"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dataType'">
            <a-tag color="blue">{{ record.dataType }}</a-tag>
          </template>
          <template v-if="column.key === 'fieldCount'">
            {{ record.fields?.length ?? 0 }}
          </template>
          <template v-if="column.key === 'maxImportRows'">
            {{ formatNumber(record.maxImportRows) }}
          </template>
          <template v-if="column.key === 'strictValidation'">
            <a-tag :color="record.strictValidation ? 'orange' : 'default'">
              {{ record.strictValidation ? '严格校验' : '常规校验' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="openPreview(record as ImportTemplateItem)">预览</a>
              <a-divider type="vertical" />
              <a @click="downloadTemplate(record as ImportTemplateItem)">下载</a>
              <a-divider type="vertical" />
              <a-popconfirm
                title="确定删除此模板?"
                @confirm="handleDelete(record as ImportTemplateItem)"
              >
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 模板预览弹窗 -->
    <a-modal
      v-model:open="previewVisible"
      title="模板结构预览"
      width="900px"
      :footer="null"
    >
      <a-spin :spinning="previewLoading">
        <a-descriptions
          :column="2"
          size="small"
          bordered
          style="margin-bottom:16px"
        >
          <a-descriptions-item label="模板ID">
            {{ preview?.templateId }}
          </a-descriptions-item>
          <a-descriptions-item label="模板名称">
            {{ preview?.templateName }}
          </a-descriptions-item>
          <a-descriptions-item label="最大导入行数">
            {{ formatNumber(preview?.maxImportRows) }}
          </a-descriptions-item>
          <a-descriptions-item label="描述">
            {{ preview?.description || '-' }}
          </a-descriptions-item>
        </a-descriptions>

        <a-table
          :data-source="preview?.fields || []"
          :columns="fieldColumns"
          row-key="fieldName"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'required'">
              <a-tag :color="record.required ? 'red' : 'default'">
                {{ record.required ? '必填' : '选填' }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { importTemplateApi, type ImportTemplateItem, type TemplatePreview } from '@/api/admin'

const loading = ref(false)
const list = ref<ImportTemplateItem[]>([])
const previewVisible = ref(false)
const previewLoading = ref(false)
const preview = ref<TemplatePreview | null>(null)

const columns = [
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName', minWidth: 160, ellipsis: true },
  { title: '模板ID', dataIndex: 'templateId', key: 'templateId', width: 150, ellipsis: true },
  { title: '数据类型', dataIndex: 'dataType', key: 'dataType', width: 110 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '字段数', key: 'fieldCount', width: 80, align: 'right' as const },
  { title: '最大导入行数', dataIndex: 'maxImportRows', key: 'maxImportRows', width: 110, align: 'right' as const },
  { title: '校验模式', dataIndex: 'strictValidation', key: 'strictValidation', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 170 },
]

const fieldColumns = [
  { title: '字段名', dataIndex: 'fieldName', key: 'fieldName', width: 150 },
  { title: '字段标题', dataIndex: 'fieldTitle', key: 'fieldTitle', width: 130 },
  { title: '类型', dataIndex: 'fieldType', key: 'fieldType', width: 90 },
  { title: '是否必填', dataIndex: 'required', key: 'required', width: 90 },
  { title: '最大长度', dataIndex: 'maxLength', key: 'maxLength', width: 90, align: 'right' as const },
  { title: '示例值', dataIndex: 'sampleValue', key: 'sampleValue', width: 120, ellipsis: true },
  { title: '说明', dataIndex: 'description', key: 'description', ellipsis: true },
]

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try {
    const res = await importTemplateApi.list()
    list.value = Array.isArray(res) ? res : []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function openPreview(record: ImportTemplateItem) {
  previewVisible.value = true
  previewLoading.value = true
  preview.value = null
  try {
    preview.value = await importTemplateApi.preview(record.templateId)
  } catch (e: any) {
    message.error(e?.message || '预览加载失败')
  } finally {
    previewLoading.value = false
  }
}

async function downloadTemplate(record: ImportTemplateItem) {
  try {
    const blob = await importTemplateApi.download(record.templateId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${record.templateName || record.templateId}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('模板下载成功')
  } catch (e: any) {
    message.error(e?.message || '模板下载失败')
  }
}

async function handleDelete(record: ImportTemplateItem) {
  try {
    await importTemplateApi.remove(record.templateId)
    message.success('模板已删除')
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
}
</style>
