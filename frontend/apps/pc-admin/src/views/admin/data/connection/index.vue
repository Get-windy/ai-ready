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
          <a-button
            type="primary"
            size="small"
            @click="showCreateForm = true"
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
              :status="record.status === 'connected' ? 'success' : 'error'"
              :text="record.status === 'connected' ? '已连接' : '断开'"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="testConnection(record)">测试</a>
              <a-divider type="vertical" />
              <a @click="editConnection(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm
                title="确定删除此连接?"
                @confirm="deleteConnection(record)"
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
      v-model:open="showCreateForm"
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
            v-model:value="form.database"
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
            <a-form-item label="密码">
              <a-input-password v-model:value="form.password" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const showCreateForm = ref(false)
const editRecord = ref<any>(null)

const form = reactive({
  name: '',
  dbType: 'PostgreSQL',
  host: '',
  port: 5432,
  database: '',
  username: '',
  password: '',
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
  { title: '数据库名', dataIndex: 'database', key: 'database', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 180 },
]

function resetForm() {
  form.name = ''
  form.dbType = 'PostgreSQL'
  form.host = ''
  form.port = 5432
  form.database = ''
  form.username = ''
  form.password = ''
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function editConnection(record: any) {
  editRecord.value = record
  form.name = record.name
  form.dbType = record.dbType
  form.host = record.host
  form.port = record.port
  form.database = record.database
  form.username = record.username
  form.password = ''
  showCreateForm.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (editRecord.value) {
      await request.put('/data-source/' + editRecord.value.id, form)
    } else {
      await request.post('/data-source', form)
    }
    message.success(editRecord.value ? '连接已更新' : '连接已创建')
    showCreateForm.value = false
    editRecord.value = null
    resetForm()
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
    fetchData()
  }
}

async function testConnection(record: any) {
  try {
    await request.post('/data-source/test', { id: record.id })
    message.success('连接测试成功')
  } catch {
    message.error('连接测试失败')
  }
}

async function deleteConnection(record: any) {
  try {
    await request.delete('/data-source/' + record.id)
    message.success('连接已删除')
  } catch {
    message.error('删除失败')
  }
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/data-source/list', { params: { page: pagination.current, pageSize: pagination.pageSize } })
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
