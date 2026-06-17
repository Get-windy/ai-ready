<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>租户列表</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">租户列表</h2>
        </div>
        <div class="page-header-right">
          <a-button type="primary" size="small" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新增租户
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <a-form layout="inline" class="search-form">
        <a-form-item label="租户名称">
          <a-input v-model:value="query.tenantName" placeholder="搜索租户名称" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear style="width:120px">
            <a-select-option :value="1">正常</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
          <a-button style="margin-left:8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :data-source="list"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '正常' : '禁用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除该租户?" @confirm="handleDelete(record)">
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { tenantApi, type TenantInfo, type TenantQuery } from '@/api/tenant'

const list = ref<TenantInfo[]>([])
const loading = ref(false)
const query = ref<TenantQuery>({ pageNum: 1, pageSize: 20 })
const pagination = ref({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '租户编码', dataIndex: 'tenantCode', key: 'tenantCode', width: 150 },
  { title: '租户名称', dataIndex: 'tenantName', key: 'tenantName', minWidth: 180 },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 120 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '到期时间', dataIndex: 'expireDate', key: 'expireDate', width: 180 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' },
]

async function fetchData() {
  loading.value = true
  try {
    const res = await tenantApi.getPage(query.value)
    list.value = res.data.records || []
    pagination.value = { current: res.data.current, pageSize: res.data.size, total: res.data.total }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.value.pageNum = 1
  fetchData()
}

function handleReset() {
  query.value = { pageNum: 1, pageSize: 20 }
  fetchData()
}

function handleTableChange(pag: any) {
  query.value.pageNum = pag.current
  query.value.pageSize = pag.pageSize
  fetchData()
}

function handleCreate() {
  message.info('创建租户功能开发中')
}

function handleEdit(record: TenantInfo) {
  message.info(`编辑租户: ${record.tenantName}`)
}

async function handleDelete(record: TenantInfo) {
  try {
    await tenantApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

onMounted(fetchData)
</script>
