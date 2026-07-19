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
            <a-breadcrumb-item>模块版本</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            模块版本
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
          <template v-if="column.key === 'releaseStatus'">
            <a-tag :color="record.releaseStatus === 'released' ? 'green' : record.releaseStatus === 'beta' ? 'blue' : 'default'">
              {{ { released: '已发布', beta: '测试版', draft: '草稿' }[record.releaseStatus] || record.releaseStatus }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="viewDetail(record)">详情</a>
              <a-divider type="vertical" />
              <a
                v-if="record.releaseStatus !== 'released'"
                @click="handlePublish(record)"
              >发布</a>
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
  { title: '模块名称', dataIndex: 'moduleName', key: 'moduleName', width: 140 },
  { title: '版本号', dataIndex: 'version', key: 'version', width: 100 },
  { title: '更新日志', dataIndex: 'changelog', key: 'changelog', ellipsis: true },
  { title: '发布状态', dataIndex: 'releaseStatus', key: 'releaseStatus', width: 100 },
  { title: '发布时间', dataIndex: 'releaseTime', key: 'releaseTime', width: 170 },
  { title: '操作', key: 'action', width: 140 },
]

function viewDetail(record: any) {
  message.info('查看版本详情: ' + record.version)
}

function handlePublish(record: any) {
  record.releaseStatus = 'released'
  message.success('版本已发布: ' + record.version)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/module/versions')
    list.value = res?.records || []
  } catch {
    list.value = [
      { id: 1, moduleName: '销售管理', version: '2.1.0', changelog: '新增批量导入功能，修复已知Bug', releaseStatus: 'released', releaseTime: '2026-06-10 14:00:00' },
      { id: 2, moduleName: '销售管理', version: '2.2.0-beta', changelog: '优化报表加载速度，新增图表分析', releaseStatus: 'beta', releaseTime: '2026-06-15 10:00:00' },
      { id: 3, moduleName: '采购管理', version: '2.0.0', changelog: '重构采购流程，支持多仓库', releaseStatus: 'released', releaseTime: '2026-05-20 09:30:00' },
      { id: 4, moduleName: '仓储管理', version: '1.5.0', changelog: '新增WMS作业模块', releaseStatus: 'released', releaseTime: '2026-06-01 11:00:00' },
      { id: 5, moduleName: '财务管理', version: '2.3.0', changelog: '新增费用管理模块', releaseStatus: 'released', releaseTime: '2026-06-05 16:00:00' },
      { id: 6, moduleName: '客户关系', version: '1.8.0', changelog: '优化客户导入功能', releaseStatus: 'draft', releaseTime: '' },
    ]
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
