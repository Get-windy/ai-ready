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
            <a-breadcrumb-item>配额管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            配额管理
          </h2>
        </div>
        <div class="page-header-right">
          <a-button
            type="primary"
            size="small"
            @click="openForm()"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新增配额
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
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'usage'">
            <a-progress
              :percent="calcUsagePercent(record)"
              :size="'small'"
              :status="calcUsagePercent(record) > 80 ? 'exception' : undefined"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="openForm(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm
                title="确定删除?"
                @confirm="handleDelete(record)"
              >
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="formVisible"
      :title="editingId ? '编辑配额' : '新增配额'"
      :confirm-loading="saving"
      destroy-on-close
      @ok="handleSave"
    >
      <a-form
        :model="form"
        layout="vertical"
      >
        <a-form-item
          label="租户名称"
          required
        >
          <a-input
            v-model:value="form.tenantName"
            placeholder="请输入租户名称"
          />
        </a-form-item>
        <a-form-item
          label="租户编码"
          required
        >
          <a-input
            v-model:value="form.tenantCode"
            placeholder="请输入租户编码"
          />
        </a-form-item>
        <a-form-item label="最大用户数">
          <a-input-number
            v-model:value="form.maxUsers"
            :min="1"
            style="width:100%"
          />
        </a-form-item>
        <a-form-item label="存储配额">
          <a-select v-model:value="form.maxStorage">
            <a-select-option value="5GB">
              5GB
            </a-select-option>
            <a-select-option value="10GB">
              10GB
            </a-select-option>
            <a-select-option value="50GB">
              50GB
            </a-select-option>
            <a-select-option value="100GB">
              100GB
            </a-select-option>
            <a-select-option value="500GB">
              500GB
            </a-select-option>
            <a-select-option value="1TB">
              1TB
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="API调用限制/月">
          <a-input-number
            v-model:value="form.maxApiCalls"
            :min="0"
            style="width:100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { tenantQuotaApi, type TenantQuotaInfo } from '@/api/tenant'

const list = ref<TenantQuotaInfo[]>([])
const loading = ref(false)
const formVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = ref<Partial<TenantQuotaInfo>>({
  tenantName: '',
  tenantCode: '',
  maxUsers: 10,
  maxStorage: '10GB',
  maxApiCalls: 10000,
})

const columns = [
  { title: '租户名称', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '租户编码', dataIndex: 'tenantCode', key: 'tenantCode', width: 150 },
  { title: '最大用户数', dataIndex: 'maxUsers', key: 'maxUsers', width: 100 },
  { title: '存储配额', dataIndex: 'maxStorage', key: 'maxStorage', width: 100 },
  { title: 'API调用限制', dataIndex: 'maxApiCalls', key: 'maxApiCalls', width: 120 },
  { title: '使用率', key: 'usage', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 150 },
]

function calcUsagePercent(record: Record<string, any>): number {
  const maxApiCalls = record.maxApiCalls as number
  const usedApiCalls = record.usedApiCalls as number
  if (!maxApiCalls || !usedApiCalls) return 0
  return Math.min(Math.round((usedApiCalls / maxApiCalls) * 100), 100)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await tenantQuotaApi.getList()
    list.value = res.records || []
  } finally {
    loading.value = false
  }
}

function openForm(record?: Record<string, any>) {
  if (record) {
    editingId.value = record.id as number
    form.value = { ...(record as TenantQuotaInfo) }
  } else {
    editingId.value = null
    form.value = {
      tenantName: '',
      tenantCode: '',
      maxUsers: 10,
      maxStorage: '10GB',
      maxApiCalls: 10000,
    }
  }
  formVisible.value = true
}

async function handleSave() {
  if (!form.value.tenantName || !form.value.tenantCode) {
    message.warning('请填写租户名称和编码')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await tenantQuotaApi.update(editingId.value, form.value)
      message.success('更新成功')
    } else {
      await tenantQuotaApi.create(form.value)
      message.success('创建成功')
    }
    formVisible.value = false
    await fetchData()
  } catch {
    message.error('操作失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: Record<string, any>) {
  try {
    await tenantQuotaApi.delete(record.id)
    message.success('删除成功')
    await fetchData()
  } catch {
    message.error('删除失败')
  }
}

onMounted(fetchData)
</script>
