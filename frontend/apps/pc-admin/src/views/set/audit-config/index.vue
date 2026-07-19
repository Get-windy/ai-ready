<template>
  <ErrorBoundary>
    <PageContainer title="审核设置">
      <div class="content-card">
        <div class="toolbar">
          <a-button
            type="primary"
            @click="handleAdd"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新增审核规则
          </a-button>
          <a-button @click="handleRefresh">
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
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
      :title="editingId ? '编辑审核规则' : '新增审核规则'"
      width="560px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form
        :model="editForm"
        layout="vertical"
      >
        <a-form-item
          label="规则名称"
          required
        >
          <a-input
            v-model:value="editForm.ruleName"
            placeholder="请输入规则名称"
          />
        </a-form-item>
        <a-form-item
          label="单据类型"
          required
        >
          <a-select
            v-model:value="editForm.billType"
            placeholder="请选择单据类型"
          >
            <a-select-option value="sale_order">
              销售订单
            </a-select-option>
            <a-select-option value="purchase_order">
              采购订单
            </a-select-option>
            <a-select-option value="sale_outbound">
              销售出库
            </a-select-option>
            <a-select-option value="purchase_inbound">
              采购入库
            </a-select-option>
            <a-select-option value="expense">
              费用报销
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="审核模式">
          <a-radio-group v-model:value="editForm.approvalMode">
            <a-radio value="single">
              单人审核
            </a-radio>
            <a-radio value="multi">
              多人会签
            </a-radio>
            <a-radio value="or">
              或签（任一通过）
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审核人">
          <a-select
            v-model:value="editForm.approverIds"
            mode="multiple"
            placeholder="请选择审核人"
          >
            <a-select-option value="admin">
              系统管理员
            </a-select-option>
            <a-select-option value="manager">
              部门经理
            </a-select-option>
            <a-select-option value="finance">
              财务
            </a-select-option>
            <a-select-option value="director">
              总监
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="金额下限（元）">
              <a-input-number
                v-model:value="editForm.minAmount"
                style="width: 100%"
                :min="0"
                :precision="2"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="金额上限（元）">
              <a-input-number
                v-model:value="editForm.maxAmount"
                style="width: 100%"
                :min="0"
                :precision="2"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="是否启用">
          <a-switch v-model:checked="editForm.enabled" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea
            v-model:value="editForm.remark"
            :rows="2"
            placeholder="备注"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const editVisible = ref(false)
const editingId = ref<number | null>(null)

const editForm = reactive<any>({
  ruleName: '',
  billType: undefined,
  approvalMode: 'single',
  approverIds: [],
  minAmount: 0,
  maxAmount: undefined,
  enabled: true,
  remark: ''
})

const columns: any[] = [
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 160 },
  { title: '单据类型', dataIndex: 'billType', key: 'billType', width: 120 },
  { title: '审核模式', dataIndex: 'approvalMode', key: 'approvalMode', width: 100 },
  { title: '金额下限', dataIndex: 'minAmount', key: 'minAmount', width: 100 },
  { title: '金额上限', dataIndex: 'maxAmount', key: 'maxAmount', width: 100 },
  { title: '是否启用', dataIndex: 'enabled', key: 'enabled', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await request.get('/audit/config/page', {
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
function handleRefresh() { loadData() }

function handleAdd() {
  editingId.value = null
  editForm.ruleName = ''
  editForm.billType = undefined
  editForm.approvalMode = 'single'
  editForm.approverIds = []
  editForm.minAmount = 0
  editForm.maxAmount = undefined
  editForm.enabled = true
  editForm.remark = ''
  editVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(editForm, {
    ruleName: record.ruleName,
    billType: record.billType,
    approvalMode: record.approvalMode,
    approverIds: record.approverIds || [],
    minAmount: record.minAmount,
    maxAmount: record.maxAmount,
    enabled: record.enabled,
    remark: record.remark
  })
  editVisible.value = true
}

async function handleSave() {
  if (!editForm.ruleName) {
    message.warning('请输入规则名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/audit/config/${editingId.value}`, editForm)
    } else {
      await request.post('/audit/config', editForm)
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
    await request.put(`/audit/config/${record.id}`, { ...record, enabled: checked })
  } catch (e) {
    message.error('操作失败')
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除审核规则 "${record.ruleName}" 吗？`,
    okType: 'danger',
    onOk: async () => {
      await request.delete(`/audit/config/${record.id}`)
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
