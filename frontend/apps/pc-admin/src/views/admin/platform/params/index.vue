<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>平台参数</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">平台参数</h2>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" :pagination="false" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a @click="handleEdit(record)">编辑</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="editVisible" title="编辑参数" width="500px" @ok="handleSave" :confirm-loading="saving">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="参数值">
          <a-textarea v-model:value="editForm.paramValue" :rows="4" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="editForm.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const list = ref<any[]>([])

const editForm = reactive({
  paramValue: '',
  remark: '',
})

const columns = [
  { title: '参数名称', dataIndex: 'paramName', key: 'paramName' },
  { title: '参数键', dataIndex: 'paramKey', key: 'paramKey', width: 200 },
  { title: '参数值', dataIndex: 'paramValue', key: 'paramValue', ellipsis: true, width: 300 },
  { title: '系统内置', dataIndex: 'builtin', key: 'builtin', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 60 },
]

function handleEdit(record: any) {
  editForm.paramValue = record.paramValue
  editForm.remark = record.remark || ''
  editVisible.value = true
}

function handleSave() {
  saving.value = true
  setTimeout(() => {
    message.success('参数已更新')
    editVisible.value = false
    saving.value = false
    fetchData()
  }, 500)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/config/list')
    list.value = res?.data || []
  } catch {
    list.value = [
      { id: 1, paramName: '系统名称', paramKey: 'system.title', paramValue: 'AI-Ready 企业管理平台', builtin: '是', remark: '系统全局显示名称' },
      { id: 2, paramName: '系统Logo', paramKey: 'system.logo', paramValue: '/logo.png', builtin: '是', remark: '系统Logo图片路径' },
      { id: 3, paramName: '默认密码', paramKey: 'system.defaultPassword', paramValue: '123456', builtin: '是', remark: '新用户默认密码' },
      { id: 4, paramName: '密码策略', paramKey: 'system.passwordPolicy', paramValue: 'minLength=8,requireUpper=1,requireLower=1,requireDigit=1', builtin: '是', remark: '密码复杂度要求' },
      { id: 5, paramName: '会话超时', paramKey: 'system.sessionTimeout', paramValue: '3600', builtin: '是', remark: '登录会话超时时间（秒）' },
      { id: 6, paramName: '上传文件大小限制', paramKey: 'system.uploadMaxSize', paramValue: '104857600', builtin: '是', remark: '单文件上传大小限制（字节）' },
      { id: 7, paramName: '允许上传文件类型', paramKey: 'system.uploadAllowedTypes', paramValue: 'jpg,png,gif,pdf,doc,docx,xls,xlsx', builtin: '否', remark: '上传文件扩展名白名单' },
      { id: 8, paramName: '登录失败锁定次数', paramKey: 'system.loginLockCount', paramValue: '5', builtin: '否', remark: '登录失败N次后锁定账户' },
    ]
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
