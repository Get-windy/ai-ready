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
            <a-breadcrumb-item>模块列表</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            模块列表
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
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="openEdit(record as ModuleItem)">编辑</a>
              <a-divider type="vertical" />
              <a @click="openVersions(record as ModuleItem)">版本</a>
              <a-divider type="vertical" />
              <a-popconfirm
                :title="record.status === 1 ? '确定停用此模块?' : '确定启用此模块?'"
                @confirm="toggleStatus(record as ModuleItem)"
              >
                <a :class="record.status === 1 ? 'text-danger' : ''">{{ record.status === 1 ? '停用' : '启用' }}</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑模块弹窗 -->
    <a-modal
      v-model:open="editVisible"
      title="编辑模块"
      width="520px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 17 }"
        style="margin-top:16px"
      >
        <a-form-item label="模块编码">
          <a-input
            :value="editingRecord?.moduleCode"
            disabled
          />
        </a-form-item>
        <a-form-item
          label="模块名称"
          required
        >
          <a-input
            v-model:value="editForm.moduleName"
            placeholder="请输入模块名称"
          />
        </a-form-item>
        <a-form-item label="版本号">
          <a-input
            v-model:value="editForm.version"
            placeholder="如: 1.0.0"
          />
        </a-form-item>
        <a-form-item label="排序号">
          <a-input-number
            v-model:value="editForm.sortOrder"
            :min="0"
            style="width:100%"
          />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="editForm.description"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 版本记录弹窗 -->
    <a-modal
      v-model:open="versionsVisible"
      :title="`版本记录 - ${versionsRecord?.moduleName || ''}`"
      width="760px"
      :footer="null"
    >
      <a-table
        :data-source="versions"
        :columns="versionColumns"
        :loading="versionsLoading"
        row-key="id"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'releaseStatus'">
            <a-tag :color="record.releaseStatus === 'released' ? 'green' : record.releaseStatus === 'draft' ? 'default' : 'orange'">
              {{ record.releaseStatus || '-' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { moduleApi, type ModuleItem, type ModuleVersionItem } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const list = ref<ModuleItem[]>([])

const editVisible = ref(false)
const editingRecord = ref<ModuleItem | null>(null)
const editForm = reactive({
  moduleName: '',
  version: '',
  sortOrder: 0,
  description: '',
})

const versionsVisible = ref(false)
const versionsLoading = ref(false)
const versions = ref<ModuleVersionItem[]>([])
const versionsRecord = ref<ModuleItem | null>(null)

const columns = [
  { title: '模块名称', dataIndex: 'moduleName', key: 'moduleName', minWidth: 140 },
  { title: '模块编码', dataIndex: 'moduleCode', key: 'moduleCode', width: 150 },
  { title: '版本号', dataIndex: 'version', key: 'version', width: 100 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 70, align: 'right' as const },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 180 },
]

const versionColumns = [
  { title: '版本号', dataIndex: 'version', key: 'version', width: 100 },
  { title: '更新说明', dataIndex: 'changelog', key: 'changelog', ellipsis: true },
  { title: '发布状态', dataIndex: 'releaseStatus', key: 'releaseStatus', width: 100 },
  { title: '发布人', dataIndex: 'publisher', key: 'publisher', width: 110 },
  { title: '发布时间', dataIndex: 'releaseTime', key: 'releaseTime', width: 170 },
]

function openEdit(record: ModuleItem) {
  editingRecord.value = record
  editForm.moduleName = record.moduleName
  editForm.version = record.version || ''
  editForm.sortOrder = record.sortOrder ?? 0
  editForm.description = record.description || ''
  editVisible.value = true
}

async function handleSave() {
  if (!editingRecord.value) return
  if (!editForm.moduleName?.trim()) { message.warning('请输入模块名称'); return }
  saving.value = true
  try {
    await moduleApi.update(editingRecord.value.id, {
      moduleName: editForm.moduleName,
      version: editForm.version,
      sortOrder: editForm.sortOrder,
      description: editForm.description,
    })
    message.success('模块已更新')
    editVisible.value = false
    editingRecord.value = null
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

async function openVersions(record: ModuleItem) {
  versionsRecord.value = record
  versionsVisible.value = true
  versionsLoading.value = true
  try {
    const res = await moduleApi.versions(record.id)
    versions.value = res?.records || []
  } catch {
    versions.value = []
  } finally {
    versionsLoading.value = false
  }
}

async function toggleStatus(record: ModuleItem) {
  try {
    await moduleApi.toggleStatus(record.id)
    record.status = record.status === 1 ? 0 : 1
    message.success(record.status === 1 ? '模块已启用' : '模块已停用')
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await moduleApi.list()
    list.value = res?.records || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
}
</style>
