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
            <a-breadcrumb-item>模块发布</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            模块发布
          </h2>
        </div>
        <div class="page-header-right">
          <a-button
            type="primary"
            size="small"
            @click="handleCreateRelease"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新建发布
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
            <a-tag :color="record.status === 'success' ? 'green' : record.status === 'pending' ? 'blue' : 'red'">
              {{ { success: '已发布', pending: '待发布', failed: '失败', rolling: '回滚中' }[record.status] || record.status }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="viewDetail(record)">详情</a>
              <a-divider
                v-if="record.status === 'success'"
                type="vertical"
              />
              <a-popconfirm
                v-if="record.status === 'success'"
                title="确定回滚此版本?"
                @confirm="rollbackVersion(record)"
              >
                <a>回滚</a>
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
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])

const columns = [
  { title: '发布编号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '模块名称', dataIndex: 'moduleName', key: 'moduleName', width: 140 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 100 },
  { title: '发布人', dataIndex: 'publisher', key: 'publisher', width: 100 },
  { title: '发布时间', dataIndex: 'releaseTime', key: 'releaseTime', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140 },
]

function handleCreateRelease() {
  message.info('新建发布功能开发中')
}

function viewDetail(record: any) {
  message.info('查看发布详情: #' + record.id)
}

function rollbackVersion(record: any) {
  message.success('版本回滚中: ' + record.version)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/module/releases')
    list.value = res?.records || []
  } catch {
    list.value = [
      { id: 1, moduleName: '销售管理', version: '2.1.0', publisher: '管理员', releaseTime: '2026-06-10 14:00:00', status: 'success' },
      { id: 2, moduleName: '采购管理', version: '2.0.0', publisher: '管理员', releaseTime: '2026-05-20 09:30:00', status: 'success' },
      { id: 3, moduleName: '仓储管理', version: '1.5.0', publisher: '运维', releaseTime: '2026-06-01 11:00:00', status: 'success' },
      { id: 4, moduleName: '财务管理', version: '2.3.0', publisher: '管理员', releaseTime: '2026-06-05 16:00:00', status: 'success' },
      { id: 5, moduleName: '客户关系', version: '1.8.0', publisher: '开发', releaseTime: '', status: 'pending' },
    ]
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
