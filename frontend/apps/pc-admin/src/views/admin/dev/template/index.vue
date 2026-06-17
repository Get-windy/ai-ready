<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>模板管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">模板管理</h2>
        </div>
        <div class="page-header-right">
          <a-button type="primary" size="small" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新增模板
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" :pagination="false" size="small">
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
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])

const columns = [
  { title: '模板名称', dataIndex: 'name', key: 'name' },
  { title: '模板编码', dataIndex: 'code', key: 'code', width: 150 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 140 },
]

function handleCreate() {
  message.info('新增模板功能开发中')
}

function handleEdit(record: any) {
  message.info('编辑模板: ' + record.name)
}

function handleDelete(record: any) {
  list.value = list.value.filter((l: any) => l.id !== record.id)
  message.success('模板已删除')
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/codegen/templates')
    list.value = res?.data || []
  } catch {
    list.value = [
      { id: 1, name: '标准CRUD模板', code: 'std_crud', type: '控制器', description: '标准增删改查控制器模板', updateTime: '2026-06-10 14:00:00' },
      { id: 2, name: 'Vue列表页模板', code: 'vue_list', type: '前端', description: 'Vue列表页组件模板', updateTime: '2026-06-08 10:30:00' },
      { id: 3, name: 'Vue表单页模板', code: 'vue_form', type: '前端', description: 'Vue表单页组件模板', updateTime: '2026-06-08 10:30:00' },
      { id: 4, name: 'MyBatis Mapper模板', code: 'mybatis_mapper', type: '数据层', description: 'MyBatis Mapper XML模板', updateTime: '2026-06-05 09:00:00' },
      { id: 5, name: 'Service接口模板', code: 'service_iface', type: '业务层', description: 'Service接口和实现类模板', updateTime: '2026-06-03 16:00:00' },
    ]
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
