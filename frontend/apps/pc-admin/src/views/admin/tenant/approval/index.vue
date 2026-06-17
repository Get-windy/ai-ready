<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>租户审批</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">租户审批</h2>
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
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag color="orange">待审批</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleApprove(record)">通过</a-button>
              <a-button type="link" danger size="small" @click="handleReject(record)">驳回</a-button>
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
import { tenantApi } from '@/api/tenant'

const list = ref<any[]>([])
const loading = ref(false)

const columns = [
  { title: '企业名称', dataIndex: 'tenantName', key: 'tenantName', minWidth: 180 },
  { title: '企业编码', dataIndex: 'tenantCode', key: 'tenantCode', width: 150 },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 120 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' },
]

async function fetchData() {
  loading.value = true
  try {
    const res = await tenantApi.getPending()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleApprove(record: any) {
  try {
    await tenantApi.approve(record.id)
    message.success('已审批通过')
    fetchData()
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}

async function handleReject(record: any) {
  try {
    await tenantApi.reject(record.id, '驳回')
    message.success('已驳回')
    fetchData()
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}

onMounted(fetchData)
</script>
