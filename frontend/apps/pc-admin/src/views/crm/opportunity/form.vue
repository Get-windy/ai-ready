<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        商机表单（CRM → 商机管理 → 商机，菜单标签「添加」的目标，80220）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/商机开发文档.md §4
        · 字段名与后端实体 CustomerOpportunity 对齐；阶段值域统一为数字 1-5
          （终态「赢单/输单」由 status 表达，只能由 /win、/lose 端点改写，故本表单不提供状态手选）
        · ⚠️ BillFormPage 用 :model-value + @update:model-value 手动绑定：
          它每次以「新对象」emit，直接用 v-model 绑定 reactive 常量会赋值失败
      -->
      <BillFormPage
        :model-value="formData"
        :header="headerConfig"
        :basic-info-fields="fields"
        :show-bottom-panel="false"
        @update:model-value="handleModelUpdate"
        @field-change="handleFieldChange"
      >
        <template #footer>
          <div class="footer-right">
            <a-button
              size="large"
              :loading="saving"
              @click="handleSave"
            >
              保存<span class="shortcut-hint">Ctrl+S</span>
            </a-button>
            <a-button
              v-if="effectiveMode !== 'edit'"
              type="primary"
              size="large"
              :loading="saving"
              @click="handleSubmit"
            >
              提交<span class="shortcut-hint">Ctrl+Enter</span>
            </a-button>
          </div>
        </template>
      </BillFormPage>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import { useBasicForm } from '@/components/BillFormPage/useBasicForm'
import type { BasicInfoField } from '@/components/BillFormPage/types'
import { opportunityApi, crmCustomerApi, leadApi } from '@/api/crm'
import { userApi } from '@/api/user'

defineOptions({ name: 'CrmOpportunityForm' })

// ═══ 阶段 / 类型 / 来源 字典（与后端一致；阶段唯一权威值域 1-5） ═══
const STAGE_OPTIONS = [
  { label: '初步接触', value: 1 },
  { label: '需求确认', value: 2 },
  { label: '方案报价', value: 3 },
  { label: '商务谈判', value: 4 },
  { label: '成交', value: 5 },
]

const FIELDS: BasicInfoField[] = [
  { key: 'opportunityCode', label: '商机编号', type: 'input', disabled: true, placeholder: '保存后由系统生成' },
  { key: 'opportunityName', label: '商机名称', type: 'input', required: true },
  // 客户选择器：存 customer_id + 回显 customer_name（后端两列都维护，列表/看板直接读 name 快照）
  { key: 'customerId', label: '客户', type: 'select', required: true, placeholder: '请选择客户' },
  { key: 'leadId', label: '来源线索', type: 'select', placeholder: '请选择来源线索' },
  // 阶段必填：opportunity_stage 可空，而后端推进阶段用 int 强转，空值会 500
  { key: 'opportunityStage', label: '阶段', type: 'select', required: true, options: STAGE_OPTIONS },
  { key: 'estimatedAmount', label: '预计金额', type: 'number', precision: 2, min: 0 },
  { key: 'actualAmount', label: '实际金额', type: 'number', precision: 2, min: 0 },
  { key: 'probability', label: '赢单概率(%)', type: 'number', precision: 0, min: 0, max: 100 },
  { key: 'expectedCloseDate', label: '预计成交日期', type: 'date' },
  { key: 'salesPersonId', label: '负责销售', type: 'select', required: true, placeholder: '请选择负责销售' },
  {
    key: 'opportunityType', label: '商机类型', type: 'select',
    options: [
      { label: '新客户', value: 1 },
      { label: '老客户增购', value: 2 },
      { label: '续约', value: 3 },
    ],
  },
  {
    key: 'opportunitySource', label: '商机来源', type: 'select',
    options: [
      { label: '线索转化', value: 1 },
      { label: '客户主动', value: 2 },
      { label: '销售开发', value: 3 },
    ],
  },
  { key: 'productInterest', label: '意向产品', type: 'input', width: 'wide' },
  { key: 'requirement', label: '需求描述', type: 'textarea', width: 'wide' },
  { key: 'competitor', label: '竞争对手', type: 'input' },
  { key: 'winReason', label: '赢单原因', type: 'textarea', width: 'wide' },
  { key: 'loseReason', label: '输单原因', type: 'textarea', width: 'wide' },
  { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
]

const { fields, formData, saving, effectiveMode, handleSave, handleSubmit } = useBasicForm({
  api: { create: opportunityApi.create, update: opportunityApi.update, getById: opportunityApi.getById },
  redirectPath: '/crm/opportunity',
  fields: FIELDS,
})

const headerConfig = computed(() => ({
  title: '商机',
  orderNo: formData.opportunityCode || undefined,
}))

// ═══ 下拉候选（真实数据源，替换原先「硬编码客户/人员」） ═══
const customerOptions = ref<{ label: string; value: number }[]>([])
const leadOptions = ref<{ label: string; value: number }[]>([])
const userOptions = ref<{ label: string; value: number }[]>([])

/** BillFormPage 以「新对象」emit 更新，此处回写到 reactive 的 formData（避免 v-model 赋值失败） */
function handleModelUpdate(val: Record<string, any>) {
  Object.assign(formData, val)
}

/** 选择器变更时同步名称快照列（customer_name / sales_person_name） */
function handleFieldChange(key: string, val: any) {
  if (key === 'customerId') {
    formData.customerName = customerOptions.value.find(o => o.value === val)?.label
  } else if (key === 'salesPersonId') {
    formData.salesPersonName = userOptions.value.find(o => o.value === val)?.label
  }
}

function setFieldOptions(key: string, options: { label: string; value: number }[]) {
  const target = fields.value.find(f => f.key === key)
  if (target) target.options = options
}

function handleError(error: Error) {
  console.error('[商机表单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(async () => {
  try {
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({ label: c.name, value: c.id }))
    setFieldOptions('customerId', customerOptions.value)
  } catch (e) {
    console.warn('[商机表单] 客户下拉获取失败', e)
  }
  try {
    const res: any = await leadApi.page({ pageNum: 1, pageSize: 200 })
    leadOptions.value = (res?.records || []).map((l: any) => ({
      label: l.leadName || l.name || l.leadCode || String(l.id),
      value: l.id,
    }))
    setFieldOptions('leadId', leadOptions.value)
  } catch (e) {
    console.warn('[商机表单] 线索下拉获取失败', e)
  }
  try {
    const res: any = await userApi.getPage({ pageNum: 1, pageSize: 200, status: 1 })
    userOptions.value = (res?.records || []).map((u: any) => ({ label: u.nickname || u.username, value: u.id }))
    setFieldOptions('salesPersonId', userOptions.value)
  } catch (e) {
    console.warn('[商机表单] 负责销售下拉获取失败', e)
  }
})
</script>

<style scoped>
.footer-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}
.shortcut-hint {
  margin-left: 6px;
  font-size: 12px;
  color: #909399;
}
</style>
