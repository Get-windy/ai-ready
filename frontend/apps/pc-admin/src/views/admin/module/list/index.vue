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
              <a @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm
                title="确定停用此模块?"
                @confirm="toggleStatus(record)"
              >
                <a :class="record.status === 1 ? 'text-danger' : ''">{{ record.status === 1 ? '停用' : '启用' }}</a>
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
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])

const columns = [
  { title: '模块名称', dataIndex: 'moduleName', key: 'moduleName' },
  { title: '模块编码', dataIndex: 'moduleCode', key: 'moduleCode', width: 150 },
  { title: '版本号', dataIndex: 'version', key: 'version', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 140 },
]

function handleEdit(record: any) {
  message.info('编辑模块: ' + record.moduleName)
}

async function toggleStatus(record: any) {
  try {
    await request.put('/module/' + record.id + '/status')
    record.status = record.status === 1 ? 0 : 1
    message.success(record.status === 1 ? '模块已启用' : '模块已停用')
  } catch {
    message.error('操作失败')
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/module/list')
    list.value = res?.records || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
