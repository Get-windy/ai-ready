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
            <a-breadcrumb-item>连接管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            连接管理
          </h2>
        </div>
        <div class="page-header-right">
          <a-input
            v-model:value="keyword"
            placeholder="连接名称/主机搜索"
            allow-clear
            size="small"
            style="width:200px;margin-right:8px"
            @press-enter="handleSearch"
          />
          <a-button
            size="small"
            style="margin-right:8px"
            @click="handleSearch"
          >
            <template #icon>
              <SearchOutlined />
            </template>
            查询
          </a-button>
          <a-button
            type="primary"
            size="small"
            @click="openCreate"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新增连接
          </a-button>
          <a-button
            size="small"
            :loading="loading"
            style="margin-left:8px"
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
        :pagination="pagination"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-badge
              :status="STATUS_MAP[record.status]?.badge || 'default'"
              :text="STATUS_MAP[record.status]?.label || record.status || '未知'"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a
                :class="{ 'text-disabled': testingId === record.id }"
                @click="testConnection(record as DataSourceItem)"
              >测试</a>
              <a-divider type="vertical" />
              <a @click="openEdit(record as DataSourceItem)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm
                title="确定删除此连接?"
                @confirm="deleteConnection(record as DataSourceItem)"
              >
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑连接弹窗 -->
    <a-modal
      v-model:open="showForm"
      :title="editRecord ? '编辑连接' : '新增连接'"
      width="560px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form
        :model="form"
        layout="vertical"
      >
        <a-form-item
          label="连接名称"
          required
        >
          <a-input
            v-model:value="form.name"
            placeholder="输入连接名称"
          />
        </a-form-item>
        <a-form-item
          label="数据库类型"
          required
        >
          <a-select v-model:value="form.dbType">
            <a-select-option value="MySQL">
              MySQL
            </a-select-option>
            <a-select-option value="PostgreSQL">
              PostgreSQL
            </a-select-option>
            <a-select-option value="Oracle">
              Oracle
            </a-select-option>
            <a-select-option value="SQLServer">
              SQL Server
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="主机地址"
              required
            >
              <a-input
                v-model:value="form.host"
                placeholder="localhost"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="端口"
              required
            >
              <a-input-number
                v-model:value="form.port"
                style="width:100%"
                :min="1"
                :max="65535"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item
          label="数据库名"
          required
        >
          <a-input
            v-model:value="form.databaseName"
            placeholder="输入数据库名"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="用户名"
              required
            >
              <a-input v-model:value="form.username" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="editRecord ? '密码（留空则不修改）' : '密码'">
              <a-input-password v-model:value="form.password" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="form.description"
            :rows="2"
            placeholder="连接用途说明"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { dataSourceApi, type DataSourceItem } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const testingId = ref<number | null>(null)
const list = ref<DataSourceItem[]>([])
const keyword = ref('')
const showForm = ref(false)
const editRecord = ref<DataSourceItem | null>(null)

// 连接状态（与后端 DataSource.status 一致：connected/disconnected/error）
const STATUS_MAP: Record<string, { label: string; badge: 'default' | 'error' | 'warning' | 'success' | 'processing' }> = {
  connected: { label: '已连接', badge: 'success' },
  disconnected: { label: '未连接', badge: 'default' },
  error: { label: '异常', badge: 'error' }
}

const form = reactive({
  name: '',
  dbType: 'PostgreSQL',
  host: '',
  port: 5432,
  databaseName: '',
  username: '',
  password: '',
  description: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
})

const columns = [
  { title: '连接名称', dataIndex: 'name', key: 'name' },
  { title: '数据库类型', dataIndex: 'dbType', key: 'dbType', width: 120 },
  { title: '主机地址', dataIndex: 'host', key: 'host', width: 150 },
  { title: '端口', dataIndex: 'port', key: 'port', width: 80 },
  { title: '数据库名', dataIndex: 'databaseName', key: 'databaseName', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 180 },
]

function resetForm() {
  form.name = ''
  form.dbType = 'PostgreSQL'
  form.host = ''
  form.port = 5432
  form.databaseName = ''
  form.username = ''
  form.password = ''
  form.description = ''
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function openCreate() {
  editRecord.value = null
  resetForm()
  showForm.value = true
}

function openEdit(record: DataSourceItem) {
  editRecord.value = record
  form.name = record.name
  form.dbType = record.dbType
  form.host = record.host
  form.port = record.port
  form.databaseName = record.databaseName
  form.username = record.username
  form.password = ''
  form.description = record.description || ''
  showForm.value = true
}

async function handleSave() {
  if (!form.name?.trim()) { message.warning('请输入连接名称'); return }
  if (!form.host?.trim()) { message.warning('请输入主机地址'); return }
  if (!form.databaseName?.trim()) { message.warning('请输入数据库名'); return }
  if (!form.username?.trim()) { message.warning('请输入用户名'); return }
  saving.value = true
  try {
    // 后端为 null-safe 部分更新：编辑时密码留空则不提交，避免覆盖原密码
    const payload: Partial<DataSourceItem> = {
      name: form.name,
      dbType: form.dbType,
      host: form.host,
      port: form.port,
      databaseName: form.databaseName,
      username: form.username,
      description: form.description,
      ...(form.password ? { password: form.password } : {}),
    }
    if (editRecord.value) {
      await dataSourceApi.update(editRecord.value.id, payload)
      message.success('连接已更新')
    } else {
      await dataSourceApi.create(payload)
      message.success('连接已创建')
    }
    showForm.value = false
    editRecord.value = null
    resetForm()
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function testConnection(record: DataSourceItem) {
  if (testingId.value) return
  testingId.value = record.id
  try {
    await dataSourceApi.test(record.id)
    message.success('连接测试成功')
  } catch (e: any) {
    message.error(e?.message || '连接测试失败')
  } finally {
    testingId.value = null
    fetchData()
  }
}

async function deleteConnection(record: DataSourceItem) {
  try {
    await dataSourceApi.remove(record.id)
    message.success('连接已删除')
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await dataSourceApi.page({
      keyword: keyword.value || undefined,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    list.value = res?.records || []
    pagination.total = res?.total || 0
  } catch {
    list.value = []
    pagination.total = 0
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
.text-disabled {
  color: #bbb;
  pointer-events: none;
}
</style>
