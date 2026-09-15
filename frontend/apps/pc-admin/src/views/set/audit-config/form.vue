<template>
  <ErrorBoundary>
    <PageContainer title="审批流程编辑器">
      <template #extra>
        <a-button type="text" @click="handleBack">
          <template #icon><ArrowLeftOutlined /></template>返回
        </a-button>
      </template>

      <div class="form-scroll-area">
        <a-form ref="formRef" :model="form" layout="vertical">
          <!-- 基本信息 -->
          <FormSection title="基本信息">
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="流程名称" required>
                  <a-input v-model:value="form.name" placeholder="如：销售订单审批" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="流程编码" required>
                  <a-input v-model:value="form.code" placeholder="如：SALE_ORDER_APPROVAL" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="流程类型" required>
                  <a-select v-model:value="form.type">
                    <a-select-option v-for="(v, k) in APPROVAL_FLOW_TYPE_MAP" :key="k" :value="k">{{ v.label }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="版本">
                  <a-input-number v-model:value="form.version" :min="1" style="width:100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="启用状态">
                  <a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="停用" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="流程描述">
              <a-textarea v-model:value="form.description" :rows="2" placeholder="流程用途说明" />
            </a-form-item>
          </FormSection>

          <!-- 审批节点 -->
          <FormSection title="审批节点">
            <div class="node-list">
              <div v-for="(node, idx) in form.nodes" :key="idx" class="node-card">
                <div class="node-header">
                  <span class="node-seq">节点 {{ idx + 1 }}</span>
                  <a-button type="link" danger size="small" @click="form.nodes.splice(idx, 1)">移除</a-button>
                </div>
                <a-row :gutter="16" class="node-fields">
                  <a-col :span="8">
                    <a-form-item label="节点名称">
                      <a-input v-model:value="node.nodeName" placeholder="如：部门审批" size="small" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="8">
                    <a-form-item label="审批人类型">
                      <a-select v-model:value="node.approverType" size="small">
                        <a-select-option v-for="(label, k) in APPROVER_TYPE_MAP" :key="k" :value="k">{{ label }}</a-select-option>
                      </a-select>
                    </a-form-item>
                  </a-col>
                  <a-col :span="8">
                    <a-form-item label="审批模式">
                      <a-select v-model:value="node.approveMode" size="small">
                        <a-select-option v-for="(label, k) in APPROVE_MODE_MAP" :key="k" :value="k">{{ label }}</a-select-option>
                      </a-select>
                    </a-form-item>
                  </a-col>
                </a-row>
                <a-row :gutter="16">
                  <a-col :span="12">
                    <a-form-item label="审批人（ID/角色编码，逗号分隔）">
                      <a-select v-model:value="node.approverIds" mode="tags" placeholder="输入" :token-separators="[',', ' ']" size="small" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="6">
                    <a-form-item label="超时（小时）">
                      <a-input-number v-model:value="node.timeoutHours" :min="0" size="small" style="width:100%" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="6">
                    <a-form-item label="超时处理">
                      <a-select v-model:value="node.timeoutAction" size="small" allow-clear>
                        <a-select-option value="auto_approve">自动通过</a-select-option>
                        <a-select-option value="auto_reject">自动驳回</a-select-option>
                        <a-select-option value="escalate">升级处理</a-select-option>
                      </a-select>
                    </a-form-item>
                  </a-col>
                </a-row>
              </div>
            </div>
            <a-button type="dashed" block @click="addNode">
              <template #icon><PlusOutlined /></template>添加节点
            </a-button>
          </FormSection>

          <!-- 工作流引擎联动验证 -->
          <FormSection title="工作流引擎联动">
            <a-alert type="info" show-icon message="工作流引擎联动验证" description="当前审批流程配置通过 approvalFlowApi 对接后端 workflow/definitions 端点。流程发布后将可在业务单据（订单/采购/报销等）中调用该流程进行审批流转。" />
            <div class="linkage-status">
              <a-descriptions :column="3" size="small" bordered>
                <a-descriptions-item label="API 端点">/workflow/definitions</a-descriptions-item>
                <a-descriptions-item label="流程类型映射">
                  <a-tag v-for="(v, k) in APPROVAL_FLOW_TYPE_MAP" :key="k" :color="v.color" style="margin:2px">{{ v.label }}</a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="审批模式">单人/或签/会签</a-descriptions-item>
              </a-descriptions>
            </div>
          </FormSection>
        </a-form>

        <div class="form-footer">
          <a-space>
            <a-button @click="handleBack">取消</a-button>
            <a-button :loading="saving" type="primary" @click="handleSave">保存流程</a-button>
          </a-space>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import FormSection from '@/components/FormSection/index.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import {
  approvalFlowApi, type ApprovalFlowDefinition, type ApprovalFlowNode,
  APPROVAL_FLOW_TYPE_MAP, APPROVER_TYPE_MAP, APPROVE_MODE_MAP
} from '@/api/workflow'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const saving = ref(false)
const editingId = ref<string | null>(null)

const form = reactive<{
  name: string; code: string; type: string | undefined; description: string;
  version: number; enabled: boolean; nodes: ApprovalFlowNode[]
}>({
  name: '', code: '', type: undefined, description: '',
  version: 1, enabled: true,
  nodes: [{ nodeName: '', approverType: 'user', approverIds: [], approveMode: 'single', timeoutHours: 0 }]
})

function addNode() {
  form.nodes.push({ nodeName: '', approverType: 'user', approverIds: [], approveMode: 'single', timeoutHours: 0 })
}

function handleBack() {
  router.push('/set/audit-config/index')
}

async function loadData(id: string) {
  try {
    const res = await approvalFlowApi.getById(id)
    Object.assign(form, res)
  } catch { message.error('加载失败') }
}

async function handleSave() {
  if (!form.name.trim() || !form.code.trim() || !form.type) {
    message.warning('请填写流程名称、编码和类型')
    return
  }
  const nodes = form.nodes
    .filter(n => n.nodeName.trim())
    .map((n, idx) => ({ ...n, nodeId: n.nodeId || `node_${idx + 1}`, nodeType: 'approval' }))
  saving.value = true
  try {
    if (editingId.value) {
      // 后端暂无更新接口，通知用户
      message.info('当前版本不支持在线修改已有流程，请新建流程')
    } else {
      await approvalFlowApi.create({ ...form, nodes })
      message.success('流程创建成功')
    }
    router.push('/set/audit-config/index')
  } catch (e: any) { message.error(e?.response?.data?.message || '保存失败') }
  finally { saving.value = false }
}

onMounted(() => {
  const id = route.params.id as string
  if (id) { editingId.value = id; loadData(id) }
})
</script>

<style scoped>
.form-scroll-area { flex: 1; overflow-y: auto; padding: 0 16px 16px; }
.form-footer { background: #fff; border-radius: 6px; padding: 16px 24px; text-align: right; box-shadow: 0 -1px 4px rgba(0,0,0,0.05); margin-top: 12px; }

.node-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 12px; }
.node-card { border: 1px solid #f0f0f0; border-radius: 6px; padding: 12px 16px; background: #fafafa; }
.node-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.node-seq { font-weight: 600; font-size: 13px; color: #1890ff; }

.linkage-status { margin-top: 12px; }
</style>
