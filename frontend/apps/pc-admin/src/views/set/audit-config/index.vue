<template>
  <ErrorBoundary>
    <PageContainer title="审核设置">
      <a-alert
        type="info"
        show-icon
        class="tip-alert"
        message="审批流程定义对接自后端 /api/workflow/definitions 实时数据。"
        description="后端当前仅开放流程定义的列表 / 详情 / 新建接口，更新、停用、删除接口尚未开放，已创建的流程暂不可在线修改或删除。"
      />
      <div class="content-card">
        <div class="toolbar">
          <a-select
            v-model:value="queryType"
            placeholder="全部流程类型"
            allow-clear
            style="width: 180px"
            @change="loadData"
          >
            <a-select-option
              v-for="(v, k) in APPROVAL_FLOW_TYPE_MAP"
              :key="k"
              :value="k"
            >
              {{ v.label }}
            </a-select-option>
          </a-select>
          <a-button @click="loadData">
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
          </a-button>
          <a-button
            type="primary"
            @click="handleAdd"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新建审批流程
          </a-button>
        </div>
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="false"
          row-key="definitionId"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'">
              <a-tag :color="typeTag(record.type).color">
                {{ typeTag(record.type).label }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'nodeCount'">
              {{ record.nodes?.length ?? 0 }} 个节点
            </template>
            <template v-else-if="column.key === 'enabled'">
              <a-tag :color="record.enabled ? 'success' : 'default'">
                {{ record.enabled ? '启用' : '停用' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'updateTime'">
              {{ formatTime(record.updateTime || record.createTime) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                @click="handleDetail(record)"
              >
                详情
              </a-button>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>

    <!-- 流程详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      title="审批流程详情"
      width="640px"
    >
      <template v-if="currentDetail">
        <a-descriptions
          :column="2"
          bordered
          size="small"
        >
          <a-descriptions-item label="流程名称">
            {{ currentDetail.name }}
          </a-descriptions-item>
          <a-descriptions-item label="流程编码">
            {{ currentDetail.code }}
          </a-descriptions-item>
          <a-descriptions-item label="流程类型">
            <a-tag :color="typeTag(currentDetail.type).color">
              {{ typeTag(currentDetail.type).label }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="版本">
            v{{ currentDetail.version }}
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="currentDetail.enabled ? 'success' : 'default'">
              {{ currentDetail.enabled ? '启用' : '停用' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="更新时间">
            {{ formatTime(currentDetail.updateTime || currentDetail.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item
            label="流程描述"
            :span="2"
          >
            {{ currentDetail.description || '-' }}
          </a-descriptions-item>
        </a-descriptions>
        <h4 class="node-title">
          审批节点（{{ currentDetail.nodes?.length ?? 0 }}）
        </h4>
        <a-table
          :columns="nodeColumns"
          :data-source="currentDetail.nodes || []"
          :pagination="false"
          row-key="nodeId"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'seq'">
              {{ index + 1 }}
            </template>
            <template v-else-if="column.key === 'approverType'">
              {{ APPROVER_TYPE_MAP[record.approverType] || record.approverType || '-' }}
            </template>
            <template v-else-if="column.key === 'approverIds'">
              {{ (record.approverIds || []).join('、') || '-' }}
            </template>
            <template v-else-if="column.key === 'approveMode'">
              {{ APPROVE_MODE_MAP[record.approveMode] || record.approveMode || '-' }}
            </template>
            <template v-else-if="column.key === 'timeoutHours'">
              {{ record.timeoutHours ? `${record.timeoutHours} 小时` : '-' }}
            </template>
          </template>
        </a-table>
      </template>
    </a-drawer>

    <!-- 新建流程弹窗 -->
    <a-modal
      v-model:open="editVisible"
      title="新建审批流程"
      width="640px"
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
              label="流程名称"
              required
            >
              <a-input
                v-model:value="editForm.name"
                placeholder="如：销售订单审批"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="流程编码"
              required
            >
              <a-input
                v-model:value="editForm.code"
                placeholder="如：SALE_ORDER_APPROVAL"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="流程类型"
              required
            >
              <a-select
                v-model:value="editForm.type"
                placeholder="请选择流程类型"
              >
                <a-select-option
                  v-for="(v, k) in APPROVAL_FLOW_TYPE_MAP"
                  :key="k"
                  :value="k"
                >
                  {{ v.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否启用">
              <a-switch v-model:checked="editForm.enabled" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="流程描述">
          <a-textarea
            v-model:value="editForm.description"
            :rows="2"
            placeholder="流程用途说明"
          />
        </a-form-item>
        <a-form-item label="审批节点">
          <div
            v-for="(node, idx) in editForm.nodes"
            :key="idx"
            class="node-row"
          >
            <a-input
              v-model:value="node.nodeName"
              placeholder="节点名称"
              style="width: 150px"
            />
            <a-select
              v-model:value="node.approverType"
              placeholder="审批人类型"
              style="width: 140px"
            >
              <a-select-option
                v-for="(label, k) in APPROVER_TYPE_MAP"
                :key="k"
                :value="k"
              >
                {{ label }}
              </a-select-option>
            </a-select>
            <a-select
              v-model:value="node.approverIds"
              mode="tags"
              placeholder="审批人ID/角色编码"
              style="flex: 1; min-width: 140px"
              :token-separators="[',', ' ']"
            />
            <a-select
              v-model:value="node.approveMode"
              placeholder="审批模式"
              style="width: 150px"
            >
              <a-select-option
                v-for="(label, k) in APPROVE_MODE_MAP"
                :key="k"
                :value="k"
              >
                {{ label }}
              </a-select-option>
            </a-select>
            <a-button
              type="link"
              danger
              size="small"
              @click="editForm.nodes.splice(idx, 1)"
            >
              移除
            </a-button>
          </div>
          <a-button
            type="dashed"
            block
            @click="addNode"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            添加节点
          </a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import {
  approvalFlowApi,
  APPROVAL_FLOW_TYPE_MAP,
  APPROVER_TYPE_MAP,
  APPROVE_MODE_MAP,
  type ApprovalFlowDefinition,
  type ApprovalFlowNode
} from '@/api/workflow'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<ApprovalFlowDefinition[]>([])
const queryType = ref<string | undefined>(undefined)
const detailVisible = ref(false)
const currentDetail = ref<ApprovalFlowDefinition | null>(null)
const editVisible = ref(false)

const editForm = reactive<{
  name: string
  code: string
  type: string | undefined
  description: string
  enabled: boolean
  nodes: ApprovalFlowNode[]
}>({
  name: '',
  code: '',
  type: undefined,
  description: '',
  enabled: true,
  nodes: []
})

const columns: any[] = [
  { title: '流程名称', dataIndex: 'name', key: 'name', width: 180, ellipsis: true },
  { title: '流程编码', dataIndex: 'code', key: 'code', width: 180, ellipsis: true },
  { title: '流程类型', dataIndex: 'type', key: 'type', width: 110 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 70 },
  { title: '审批节点', key: 'nodeCount', width: 100 },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 90 },
  { title: '更新时间', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]

const nodeColumns: any[] = [
  { title: '#', key: 'seq', width: 50 },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', width: 130 },
  { title: '审批人类型', dataIndex: 'approverType', key: 'approverType', width: 110 },
  { title: '审批人', dataIndex: 'approverIds', key: 'approverIds', ellipsis: true },
  { title: '审批模式', dataIndex: 'approveMode', key: 'approveMode', width: 130 },
  { title: '超时', dataIndex: 'timeoutHours', key: 'timeoutHours', width: 90 }
]

function typeTag(type: string): { label: string; color: string } {
  return APPROVAL_FLOW_TYPE_MAP[type] || { label: type || '未知', color: 'default' }
}

function formatTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const res = await approvalFlowApi.list(queryType.value)
    tableData.value = res?.definitions || []
  } catch (e) {
    console.warn('[审核设置] 流程定义获取失败', e)
  } finally {
    loading.value = false
  }
}

async function handleDetail(record: any) {
  const row = record as ApprovalFlowDefinition
  try {
    currentDetail.value = await approvalFlowApi.getById(row.definitionId)
  } catch (e) {
    currentDetail.value = row
  }
  detailVisible.value = true
}

function handleAdd() {
  editForm.name = ''
  editForm.code = ''
  editForm.type = undefined
  editForm.description = ''
  editForm.enabled = true
  editForm.nodes = [{ nodeName: '', approverType: 'user', approverIds: [], approveMode: 'single' }]
  editVisible.value = true
}

function addNode() {
  editForm.nodes.push({ nodeName: '', approverType: 'user', approverIds: [], approveMode: 'single' })
}

async function handleSave() {
  if (!editForm.name.trim()) {
    message.warning('请输入流程名称')
    return
  }
  if (!editForm.code.trim()) {
    message.warning('请输入流程编码')
    return
  }
  if (!editForm.type) {
    message.warning('请选择流程类型')
    return
  }
  const nodes = editForm.nodes
    .filter(n => n.nodeName.trim())
    .map((n, idx) => ({ ...n, nodeId: `node_${idx + 1}`, nodeType: 'approval' }))
  saving.value = true
  try {
    await approvalFlowApi.create({
      name: editForm.name.trim(),
      code: editForm.code.trim(),
      type: editForm.type,
      description: editForm.description,
      enabled: editForm.enabled,
      nodes
    })
    message.success('创建成功')
    editVisible.value = false
    loadData()
  } catch (e) {
    console.warn('[审核设置] 创建流程失败', e)
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.tip-alert { margin-bottom: 12px; }
.content-card { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.toolbar { margin-bottom: 16px; display: flex; gap: 8px; }
.node-title { margin: 16px 0 8px; font-weight: 600; }
.node-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
</style>
