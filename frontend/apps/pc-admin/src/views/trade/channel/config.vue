<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">渠道配置</h2>
      </div>
      <div class="page-header__right">
        <a-button type="primary" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>
          新增渠道
        </a-button>
      </div>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="false" row-key="id">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'channelCode'">
              <a-tag :color="CHANNEL_CODE_MAP[record.channelCode]?.type === 'ECOMMERCE' ? 'blue' : CHANNEL_CODE_MAP[record.channelCode]?.type === 'SOCIAL' ? 'green' : 'orange'">
                {{ CHANNEL_CODE_MAP[record.channelCode]?.name || record.channelCode }}
              </a-tag>
            </template>
            <template v-if="column.key === 'channelType'">
              <a-tag :color="CHANNEL_TYPE_MAP[record.channelType]?.color">{{ CHANNEL_TYPE_MAP[record.channelType]?.name }}</a-tag>
            </template>
            <template v-if="column.key === 'syncEnabled'">
              <a-switch :checked="record.syncEnabled === 1" @change="(checked: boolean) => handleToggleSync(record, checked)" />
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'success' : 'error'">{{ record.status === 1 ? '正常' : '禁用' }}</a-tag>
            </template>
            <template v-if="column.key === 'connected'">
              <a-tag :color="record.accessToken ? 'success' : 'warning'">{{ record.accessToken ? '已连接' : '待配置' }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="showEditModal(record)">配置</a-button>
                <a-button type="link" size="small" v-if="!record.accessToken" @click="handleInitialize(record)">初始化</a-button>
                <a-button type="link" size="small" @click="handleSync(record)">同步</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑渠道配置' : '新增渠道'" width="700px" @ok="handleSave">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="渠道编码">
          <a-select v-model:value="form.channelCode" placeholder="请选择渠道" :disabled="!!editingId">
            <a-select-option value="TAOBAO">淘宝/天猫</a-select-option>
            <a-select-option value="JD">京东</a-select-option>
            <a-select-option value="PDD">拼多多</a-select-option>
            <a-select-option value="DOUYIN">抖音</a-select-option>
            <a-select-option value="WECHAT_MINI">微信小程序</a-select-option>
            <a-select-option value="SELF_MALL">自有商城</a-select-option>
            <a-select-option value="POS">POS终端</a-select-option>
            <a-select-option value="ERP_API">ERP对接</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="渠道名称">
          <a-input v-model:value="form.channelName" placeholder="渠道名称" />
        </a-form-item>
        <a-form-item label="API端点">
          <a-input v-model:value="form.apiEndpoint" placeholder="如: https://eco.taobao.com/router/rest" />
        </a-form-item>
        <a-form-item label="App ID">
          <a-input v-model:value="form.appId" placeholder="应用ID" />
        </a-form-item>
        <a-form-item label="App Secret">
          <a-input-password v-model:value="form.appSecret" placeholder="应用密钥" />
        </a-form-item>
        <a-form-item label="同步间隔">
          <a-input-number v-model:value="form.syncInterval" min="5" max="120" addon-after="分钟" />
        </a-form-item>
        <a-form-item label="扩展配置">
          <a-textarea v-model:value="form.configJson" placeholder="JSON格式配置" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { channelConfigApi, CHANNEL_CODE_MAP, CHANNEL_TYPE_MAP, type ExternalChannelConfig } from '@/api/trade'

const loading = ref(false)
const tableData = ref<ExternalChannelConfig[]>([])
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  channelCode: '', channelName: '', channelType: 'ECOMMERCE',
  apiEndpoint: '', appId: '', appSecret: '',
  syncEnabled: 1, syncInterval: 30, configJson: '', status: 1
})

const columns: any[] = [
  { title: '渠道', dataIndex: 'channelCode', key: 'channelCode', width: 120 },
  { title: '渠道名称', dataIndex: 'channelName', key: 'channelName', width: 150 },
  { title: '类型', dataIndex: 'channelType', key: 'channelType', width: 100 },
  { title: '连接状态', key: 'connected', width: 80 },
  { title: '同步开关', dataIndex: 'syncEnabled', key: 'syncEnabled', width: 80 },
  { title: '同步间隔', dataIndex: 'syncInterval', key: 'syncInterval', width: 80 },
  { title: '最后同步', dataIndex: 'lastSyncTime', key: 'lastSyncTime', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await channelConfigApi.listEnabled()
    tableData.value = result.data.data
  } finally { loading.value = false }
}

function showCreateModal() {
  editingId.value = null
  Object.assign(form, {
    channelCode: '', channelName: '', channelType: 'ECOMMERCE',
    apiEndpoint: '', appId: '', appSecret: '',
    syncEnabled: 1, syncInterval: 30, configJson: '', status: 1
  })
  modalVisible.value = true
}

function showEditModal(record: any) {
  editingId.value = record.id
  Object.assign(form, {
    channelCode: record.channelCode,
    channelName: record.channelName,
    channelType: record.channelType,
    apiEndpoint: record.apiEndpoint || '',
    appId: record.appId || '',
    appSecret: record.appSecret || '',
    syncEnabled: record.syncEnabled,
    syncInterval: record.syncInterval,
    configJson: record.configJson || '',
    status: record.status
  })
  modalVisible.value = true
}

async function handleSave() {
  if (!form.channelCode) { message.warning('请选择渠道'); return }
  form.channelName = CHANNEL_CODE_MAP[form.channelCode]?.name || form.channelName
  form.channelType = CHANNEL_CODE_MAP[form.channelCode]?.type || 'ECOMMERCE'
  try {
    if (editingId.value) {
      await channelConfigApi.update(editingId.value, form as ExternalChannelConfig)
      message.success('更新成功')
    } else {
      await channelConfigApi.create(form as ExternalChannelConfig)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) { message.error('保存失败') }
}

async function handleToggleSync(record: any, checked: boolean) {
  await channelConfigApi.toggleStatus(record.id, checked)
  message.success(checked ? '已启用同步' : '已禁用同步')
  loadData()
}

async function handleInitialize(record: any) {
  await channelConfigApi.initialize(record.id)
  message.success('初始化完成')
  loadData()
}

async function handleSync(record: any) {
  message.info('开始同步...')
  try {
    const result = await channelConfigApi.sync(record.id)
    const data = result.data
    message.success(`同步完成！同步订单: ${data.syncedOrders}，同步商品: ${data.syncedProducts}`)
    loadData()
  } catch (e: any) {
    message.error('同步失败: ' + (e.message || '未知错误'))
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.table-card { background: #fff; }
</style>