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
          <a-button type="primary" size="small" @click="openForm()">
            <template #icon><PlusOutlined /></template>
            新增套餐
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'purchaseType'">
            {{ { monthly: '按月', yearly: '按年', perpetual: '永久' }[record.purchaseType] || record.purchaseType }}
          </template>
          <template v-if="column.key === 'price'">
            {{ record.price ? '¥' + (record.price / 100).toFixed(2) : '-' }}
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="openForm(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除?" @confirm="handleDelete(record)">
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formVisible" :title="editingId ? '编辑套餐' : '新增套餐'" @ok="handleSave" :confirm-loading="saving" destroy-on-close>
      <a-form :model="form" layout="vertical">
        <a-form-item label="套餐名称" required>
          <a-input v-model:value="form.packageName" placeholder="请输入套餐名称" />
        </a-form-item>
        <a-form-item label="套餐编码" required>
          <a-input v-model:value="form.packageCode" placeholder="请输入套餐编码" />
        </a-form-item>
        <a-form-item label="购买类型" required>
          <a-select v-model:value="form.purchaseType">
            <a-select-option value="monthly">按月</a-select-option>
            <a-select-option value="yearly">按年</a-select-option>
            <a-select-option value="perpetual">永久</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="价格（元）">
          <a-input-number v-model:value="formPriceYuan" :min="0" :precision="2" style="width:100%" placeholder="请输入价格" />
        </a-form-item>
        <a-form-item label="最大用户数">
          <a-input-number v-model:value="form.maxUsers" :min="1" style="width:100%" />
        </a-form-item>
        <a-form-item label="存储配额(GB)">
          <a-input-number v-model:value="form.storageQuota" :min="0" style="width:100%" />
        </a-form-item>
        <a-form-item label="API调用限制/月">
          <a-input-number v-model:value="form.apiCallLimit" :min="0" style="width:100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="formStatus" checked-children="启用" un-checked-children="停用" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { tenantPackageApi, type TenantPackageInfo } from '@/api/tenant'

const list = ref<TenantPackageInfo[]>([])
const loading = ref(false)
const formVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = ref<Partial<TenantPackageInfo>>({
  packageName: '',
  packageCode: '',
  purchaseType: 'monthly',
  maxUsers: 10,
  storageQuota: 10,
  apiCallLimit: 10000,
  status: 1,
  description: '',
})

const formPriceYuan = computed({
  get: () => (form.value.price ?? 0) / 100,
  set: (val: number) => { form.value.price = Math.round(val * 100) },
})

const formStatus = computed({
  get: () => form.value.status === 1,
  set: (val: boolean) => { form.value.status = val ? 1 : 0 },
})

const columns = [
  { title: '套餐名称', dataIndex: 'packageName', key: 'packageName' },
  { title: '套餐编码', dataIndex: 'packageCode', key: 'packageCode', width: 120 },
  { title: '购买类型', key: 'purchaseType', width: 80 },
  { title: '价格', key: 'price', width: 100 },
  { title: '最大用户数', dataIndex: 'maxUsers', key: 'maxUsers', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 150 },
]

async function fetchData() {
  loading.value = true
  try {
    const res = await tenantPackageApi.getList()
    list.value = res.records || []
  } finally {
    loading.value = false
  }
}

function openForm(record?: TenantPackageInfo) {
  if (record) {
    editingId.value = record.id
    form.value = { ...record }
  } else {
    editingId.value = null
    form.value = {
      packageName: '',
      packageCode: '',
      purchaseType: 'monthly',
      maxUsers: 10,
      storageQuota: 10,
      apiCallLimit: 10000,
      status: 1,
      description: '',
    }
  }
  formVisible.value = true
}

async function handleSave() {
  if (!form.value.packageName || !form.value.packageCode) {
    message.warning('请填写套餐名称和编码')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await tenantPackageApi.update(editingId.value, form.value)
      message.success('更新成功')
    } else {
      await tenantPackageApi.create(form.value)
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

async function handleDelete(record: TenantPackageInfo) {
  try {
    await tenantPackageApi.delete(record.id)
    message.success('删除成功')
    await fetchData()
  } catch {
    message.error('删除失败')
  }
}

onMounted(fetchData)
</script>
