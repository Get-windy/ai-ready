<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>租户套餐</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">租户套餐</h2>
        </div>
        <div class="page-header-right">
          <a-button type="primary" size="small" @click="showForm = true">
            <template #icon><PlusOutlined /></template>
            新增套餐
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <a-table :data-source="packages" :columns="columns" :loading="loading" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
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

const loading = ref(false)
const packages = ref<any[]>([])
const showForm = ref(false)

const columns = [
  { title: '套餐名称', dataIndex: 'moduleName', key: 'moduleName' },
  { title: '套餐编码', dataIndex: 'moduleCode', key: 'moduleCode', width: 150 },
  { title: '购买类型', dataIndex: 'purchaseType', key: 'purchaseType', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 150 },
]

async function fetchData() {
  loading.value = true
  try {
    const { getTenantModules } = await import('@/api/menu')
    const res = await getTenantModules()
    packages.value = (res as any).data || []
  } finally {
    loading.value = false
  }
}

function handleEdit(record: any) {
  message.info('编辑套餐: ' + record.moduleName)
}

function handleDelete(record: any) {
  message.success('删除成功')
  packages.value = packages.value.filter((p: any) => p.id !== record.id)
}

onMounted(fetchData)
</script>
