<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>配额管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">配额管理</h2>
        </div>
        <div class="page-header-right">
          <a-button type="primary" size="small" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新增配额
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除?" @confirm="handleDelete(record)">
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
import { PlusOutlined } from '@ant-design/icons-vue'

interface QuotaItem {
  id: number
  tenantName: string
  tenantCode: string
  maxUsers: number
  maxStorage: string
  maxApiCalls: number
  createdTime: string
}

const list = ref<QuotaItem[]>([])
const loading = ref(false)

const columns = [
  { title: '租户名称', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '租户编码', dataIndex: 'tenantCode', key: 'tenantCode', width: 150 },
  { title: '最大用户数', dataIndex: 'maxUsers', key: 'maxUsers', width: 120 },
  { title: '存储配额', dataIndex: 'maxStorage', key: 'maxStorage', width: 120 },
  { title: 'API调用限制', dataIndex: 'maxApiCalls', key: 'maxApiCalls', width: 130 },
  { title: '创建时间', dataIndex: 'createdTime', key: 'createdTime', width: 180 },
  { title: '操作', key: 'action', width: 150 },
]

async function fetchData() {
  loading.value = true
  try {
    const { tenantApi } = await import('@/api/tenant')
    const res = await tenantApi.getPage({ pageNum: 1, pageSize: 200 })
    list.value = (res.data.records || []).map((t: any) => ({
      id: t.id,
      tenantName: t.tenantName,
      tenantCode: t.tenantCode,
      maxUsers: t.maxUsers || 10,
      maxStorage: t.maxStorage || '10GB',
      maxApiCalls: t.maxApiCalls || 10000,
      createdTime: t.createTime,
    }))
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  message.info('新增配额功能开发中')
}

function handleEdit(record: QuotaItem) {
  message.info('编辑配额: ' + record.tenantName)
}

function handleDelete(record: QuotaItem) {
  message.success('删除成功')
  list.value = list.value.filter((item) => item.id !== record.id)
}

onMounted(fetchData)
</script>
