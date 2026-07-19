<template>
  <ErrorBoundary>
    <PageContainer title="支付配置">
      <div class="content-card">
        <div class="toolbar">
          <a-button
            type="primary"
            @click="handleAdd"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新增配置
          </a-button>
        </div>
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          size="small"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'enabled'">
              <a-switch
                v-model:checked="record.enabled"
                @change="(checked: boolean) => handleToggle(record, checked)"
              />
            </template>
            <template v-if="column.key === 'configJson'">
              <a-button
                type="link"
                size="small"
                @click="showConfigJson(record)"
              >
                查看
              </a-button>
            </template>
            <template v-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                type="link"
                size="small"
                danger
                @click="handleDelete(record)"
              >
                删除
              </a-button>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>

    <a-modal
      v-model:open="editVisible"
      :title="editingId ? '编辑支付配置' : '新增支付配置'"
      width="600px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form
        :model="editForm"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="支付方式"
              required
            >
              <a-select
                v-model:value="editForm.paymentMethod"
                placeholder="请选择支付方式"
              >
                <a-select-option value="alipay">
                  支付宝
                </a-select-option>
                <a-select-option value="wechat">
                  微信支付
                </a-select-option>
                <a-select-option value="unionpay">
                  银联支付
                </a-select-option>
                <a-select-option value="bank_transfer">
                  银行转账
                </a-select-option>
                <a-select-option value="cash">
                  现金
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="支付渠道">
              <a-input
                v-model:value="editForm.channelName"
                placeholder="渠道名称"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item
          label="APP ID / 商户号"
          required
        >
          <a-input
            v-model:value="editForm.appId"
            placeholder="请输入APP ID或商户号"
          />
        </a-form-item>
        <a-form-item label="API密钥">
          <a-input-password
            v-model:value="editForm.apiSecret"
            placeholder="请输入API密钥"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="公钥/证书">
              <a-input
                v-model:value="editForm.publicKey"
                placeholder="公钥或证书路径"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="私钥">
              <a-input
                v-model:value="editForm.privateKey"
                placeholder="私钥或证书路径"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="异步通知URL">
          <a-input
            v-model:value="editForm.notifyUrl"
            placeholder="http://..."
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number
                v-model:value="editForm.sort"
                style="width: 100%"
                :min="0"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否启用">
              <a-switch v-model:checked="editForm.enabled" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-textarea
            v-model:value="editForm.remark"
            :rows="2"
            placeholder="备注信息"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="jsonVisible"
      title="配置详情"
      width="600px"
      :footer="null"
    >
      <pre style="max-height: 400px; overflow: auto; background: #f5f5f5; padding: 12px; border-radius: 4px;">{{ currentJson }}</pre>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const editVisible = ref(false)
const editingId = ref<number | null>(null)
const jsonVisible = ref(false)
const currentJson = ref('')

const editForm = reactive<any>({
  paymentMethod: undefined,
  channelName: '',
  appId: '',
  apiSecret: '',
  publicKey: '',
  privateKey: '',
  notifyUrl: '',
  sort: 0,
  enabled: true,
  remark: ''
})

const columns: any[] = [
  { title: '支付方式', dataIndex: 'paymentMethod', key: 'paymentMethod', width: 100 },
  { title: '渠道名称', dataIndex: 'channelName', key: 'channelName', width: 120 },
  { title: 'APP ID', dataIndex: 'appId', key: 'appId', width: 180, ellipsis: true },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 60 },
  { title: '是否启用', dataIndex: 'enabled', key: 'enabled', width: 80 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 160 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await request.get('/payment/config/page', {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (result && result.records) {
      tableData.value = result.records
      pagination.total = result.total
    } else if (Array.isArray(result)) {
      tableData.value = result
      pagination.total = result.length
    }
  } catch (e) {
    // 静默失败
  } finally {
    loading.value = false
  }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function handleAdd() {
  editingId.value = null
  editForm.paymentMethod = undefined
  editForm.channelName = ''
  editForm.appId = ''
  editForm.apiSecret = ''
  editForm.publicKey = ''
  editForm.privateKey = ''
  editForm.notifyUrl = ''
  editForm.sort = 0
  editForm.enabled = true
  editForm.remark = ''
  editVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(editForm, record)
  editVisible.value = true
}

function showConfigJson(record: any) {
  currentJson.value = JSON.stringify(record, null, 2)
  jsonVisible.value = true
}

async function handleSave() {
  if (!editForm.paymentMethod) {
    message.warning('请选择支付方式')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/payment/config/${editingId.value}`, editForm)
    } else {
      await request.post('/payment/config', editForm)
    }
    message.success('保存成功')
    editVisible.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleToggle(record: any, checked: boolean) {
  try {
    await request.put(`/payment/config/${record.id}`, { ...record, enabled: checked })
  } catch (e) {
    message.error('操作失败')
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除支付配置 "${record.paymentMethod}" 吗？`,
    okType: 'danger',
    onOk: async () => {
      await request.delete(`/payment/config/${record.id}`)
      message.success('删除成功')
      loadData()
    }
  })
}

onMounted(loadData)
</script>

<style scoped>
.content-card { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.toolbar { margin-bottom: 16px; display: flex; gap: 8px; }
</style>
