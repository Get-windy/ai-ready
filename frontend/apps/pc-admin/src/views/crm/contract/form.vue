<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
      title="合同"
    >
      <!--
        合同表单页（CRM → 合同管理 → 合同 的「添加」标签入口；路由 crm/contract/form）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/合同开发文档.md §4.2
        · 外壳：ErrorBoundary > PageContainer(full-height) > BillFormPage（与本模块其它表单页一致）
        · 本轮改造：
          ① 修 P0「表单页存不了」：原 `v-model="formData"` 直绑 useBasicForm 返回的
             reactive 常量 —— BillFormPage 每次编辑都 emit 一个新对象，v-model 会尝试把新对象
             赋给不可重新赋值的 const 常量 ⇒ 赋值失败、状态不回流（能打字、提交读到的仍是空值）。
             改为 :model-value + @update:model-value 显式合并。
          ② 修 P0「合同类型值域错位」：原 1 销售 / 2 服务 / 3 框架，而后端 ContractType 是
             1 销售 / 2 采购 / 3 服务 / 4 项目 / 5 框架 / 6 合作伙伴 / 7 其他 ——
             选「服务合同」实际会存成「采购合同」。现与后端枚举逐值对齐。
          ③ 字段收敛到后端 ContractCreateDTO 真实存在的列：去掉 signPerson/terms/contractNo
             等「填了也存不下来」的字段；已收/待收/状态为只读回显（后端维护）。
          ④ 客户由自由文本改 CRM 客户下拉（crmCustomerApi.dropdown，回写 customerId + customerName）。
          ⑤ 保存/提交不再复用 useBasicForm（它在 update 时做 Number(editId)，而雪花 ID 超
             2^53 会精度失真；且注入的 status:1 会被 DTO 静默丢弃）——改为本页按字符串 ID
             直调 contractApi，且「提交」= 保存 + 调 /{id}/submit 真实进入待审批。
      -->
      <BillFormPage
        :model-value="formData"
        :header="{ title: isEdit ? '编辑合同' : '新增合同' }"
        :basic-info-fields="formFields"
        :show-bottom-panel="false"
        @update:model-value="handleModelUpdate"
      >
        <template #footer>
          <a-tooltip
            title="页面配置"
            placement="bottom"
          >
            <a-button
              size="small"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
          </a-tooltip>
          <div class="footer-right">
            <a-button
              v-if="isButtonEnabled('save')"
              size="large"
              :loading="saving"
              @click="handleSave"
            >
              保存<span class="shortcut-hint">Ctrl+S</span>
            </a-button>
            <a-button
              v-if="isButtonEnabled('submit')"
              type="primary"
              size="large"
              :loading="submitting"
              @click="handleSubmit"
            >
              提交<span class="shortcut-hint">Ctrl+Enter</span>
            </a-button>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 页面配置（表单页无查询条件，仅控制底部功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonConfig"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import type { BasicInfoField } from '@/components/BillFormPage/types'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import { contractApi, crmCustomerApi } from '@/api/crm'

defineOptions({ name: 'CrmContractForm' })

const route = useRoute()
const router = useRouter()

// ═══ 字段定义（与后端 ContractCreateDTO / ContractVO 逐一对齐） ═══
const BASE_FIELDS: BasicInfoField[] = [
  { key: 'contractNo', label: '合同编号', type: 'input', disabled: true, placeholder: '保存后由后端号段生成' },
  { key: 'contractName', label: '合同名称', type: 'input', required: true },
  {
    // 值域 = 后端 ContractType 枚举（Integer 1..7），见合同开发文档 §5.4
    key: 'contractType', label: '合同类型', type: 'select', required: true,
    options: [
      { label: '销售合同', value: 1 },
      { label: '采购合同', value: 2 },
      { label: '服务合同', value: 3 },
      { label: '项目合同', value: 4 },
      { label: '框架合同', value: 5 },
      { label: '合作伙伴合同', value: 6 },
      { label: '其他合同', value: 7 },
    ],
  },
  // 客户：选项异步注入（crm_contract.customer_id 指向 CRM 客户 crm_customer）
  { key: 'customerId', label: '客户名称', type: 'select', required: true, options: [] },
  { key: 'opportunityName', label: '关联商机', type: 'input' },
  { key: 'contactName', label: '联系人', type: 'input' },
  { key: 'signDate', label: '签订日期', type: 'date' },
  { key: 'startDate', label: '开始日期', type: 'date', required: true },
  { key: 'endDate', label: '结束日期', type: 'date', required: true },
  { key: 'contractAmount', label: '合同金额', type: 'number', precision: 2, required: true },
  {
    key: 'currency', label: '币种', type: 'select',
    options: [
      { label: '人民币', value: 'CNY' },
      { label: '美元', value: 'USD' },
    ],
  },
  // 已收/待收由付款计划确认收款时汇总回写（后端 crm_contract_payment 闭环），表单只读回显
  { key: 'paidAmount', label: '已收金额', type: 'number', precision: 2, disabled: true },
  { key: 'pendingAmount', label: '待收金额', type: 'number', precision: 2, disabled: true },
  {
    // 付款方式后端无枚举类、无字典表（合同开发文档 §5.4），沿用本模块既有 1/2/3 词表
    key: 'paymentMethod', label: '结算方式', type: 'select',
    options: [
      { label: '一次性付款', value: 1 },
      { label: '分期付款', value: 2 },
      { label: '货到付款', value: 3 },
    ],
  },
  { key: 'paymentTerms', label: '付款条款', type: 'textarea', width: 'wide' },
  { key: 'deliveryTerms', label: '交付条款', type: 'textarea', width: 'wide' },
  { key: 'warrantyTerms', label: '质保条款', type: 'textarea', width: 'wide' },
  { key: 'serviceTerms', label: '服务条款', type: 'textarea', width: 'wide' },
  { key: 'salesPersonName', label: '负责人', type: 'input' },
  {
    // 状态：由后端状态机维护，DTO 无 status 字段，此处只读回显（值域 = ContractStatus 11 态）
    key: 'status', label: '状态', type: 'select', disabled: true,
    options: [
      { label: '草稿', value: 0 },
      { label: '待审批', value: 1 },
      { label: '已审批', value: 2 },
      { label: '待签署', value: 3 },
      { label: '已签署', value: 4 },
      { label: '生效中', value: 5 },
      { label: '执行中', value: 6 },
      { label: '已完成', value: 7 },
      { label: '已终止', value: 8 },
      { label: '已过期', value: 9 },
      { label: '已取消', value: 10 },
    ],
  },
  { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
]

// ═══ 表单模型（本页自持，不用 useBasicForm：见文件头 ①⑤ 说明） ═══
const formData = reactive<Record<string, any>>({})
for (const field of BASE_FIELDS) {
  formData[field.key] = field.type === 'number' ? 0 : undefined
}
formData.currency = 'CNY'

const saving = ref(false)
const submitting = ref(false)

const editId = computed<string | undefined>(() => {
  const id = route.params.id || route.query.id
  return id ? String(id) : undefined
})
const isEdit = computed(() => !!editId.value)

// ═══ 客户下拉（异步注入字段 options） ═══
const customerOptions = ref<{ label: string; value: any }[]>([])

const formFields = computed<BasicInfoField[]>(() =>
  BASE_FIELDS.map(f => (f.key === 'customerId' ? { ...f, options: customerOptions.value } : f))
)

async function loadCustomerOptions() {
  try {
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({
      label: c.name,
      value: c.id,
    }))
  } catch (e) {
    console.warn('[合同表单] 客户下拉获取失败', e)
    message.warning('客户下拉加载失败，请稍后重试')
    customerOptions.value = []
  }
}

/** BillFormPage 每次编辑都 emit 新对象（{ ...modelValue, [key]: val }），需合并回本页 reactive 模型 */
function handleModelUpdate(next: Record<string, any>) {
  const prevCustomerId = formData.customerId
  Object.assign(formData, next || {})
  if (formData.customerId !== prevCustomerId) {
    formData.customerName = customerOptions.value.find(o => String(o.value) === String(formData.customerId))?.label
  }
}

// ═══ 校验与提交体 ═══
function validate(): boolean {
  for (const field of BASE_FIELDS) {
    if (!field.required) continue
    const val = formData[field.key]
    if (val === undefined || val === null || val === '') {
      message.warning(`请填写${field.label}`)
      return false
    }
  }
  return true
}

/** 日期字段统一转 YYYY-MM-DD 字符串（BillFormPage 的日期控件可能给出 dayjs 对象） */
function toDateStr(v: any): string | undefined {
  if (!v) return undefined
  if (typeof v === 'string') return v.slice(0, 10)
  if (typeof v?.format === 'function') return v.format('YYYY-MM-DD')
  return undefined
}

/** 只发 ContractCreateDTO 真实存在的字段；contractNo/已收/待收/status 由后端维护，不回传 */
function buildPayload() {
  return {
    contractName: formData.contractName,
    contractType: formData.contractType,
    customerId: formData.customerId,
    customerName: formData.customerName || customerOptions.value.find(o => String(o.value) === String(formData.customerId))?.label,
    opportunityName: formData.opportunityName || undefined,
    contactName: formData.contactName || undefined,
    signDate: toDateStr(formData.signDate),
    startDate: toDateStr(formData.startDate),
    endDate: toDateStr(formData.endDate),
    contractAmount: formData.contractAmount,
    currency: formData.currency || 'CNY',
    paymentMethod: formData.paymentMethod,
    paymentTerms: formData.paymentTerms || undefined,
    deliveryTerms: formData.deliveryTerms || undefined,
    warrantyTerms: formData.warrantyTerms || undefined,
    serviceTerms: formData.serviceTerms || undefined,
    salesPersonName: formData.salesPersonName || undefined,
    remark: formData.remark || undefined,
  }
}

/** 保存或更新；返回落库后的合同 ID（字符串，雪花 ID 不做 Number 转换） */
async function persist(): Promise<string | null> {
  const payload = buildPayload()
  if (isEdit.value && editId.value) {
    const updated: any = await contractApi.update(editId.value as any, payload as any)
    return String(updated?.id ?? updated?.data?.id ?? editId.value)
  }
  const created: any = await contractApi.create(payload as any)
  const id = created?.id ?? created?.data?.id
  return id === undefined || id === null ? null : String(id)
}

async function handleSave() {
  if (!validate()) return
  saving.value = true
  try {
    await persist()
    message.success('保存成功')
    router.push('/crm/contract')
  } catch (err: any) {
    console.warn('[合同表单] 保存失败', err)
    message.error(err?.response?.data?.message || err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

/** 提交 = 保存 + 把该合同提交审批（原实现只保存，且注入的 status:1 被 DTO 丢弃） */
async function handleSubmit() {
  if (!validate()) return
  if (isEdit.value && Number(formData.status) !== 0) {
    message.warning('只有草稿状态的合同可以提交审批')
    return
  }
  submitting.value = true
  try {
    const id = await persist()
    if (!id) {
      message.warning('保存成功，但未取到合同 ID，请到列表页手动提交审批')
      router.push('/crm/contract')
      return
    }
    try {
      await contractApi.submitForApproval(id as any)
      message.success('已提交审批')
    } catch (err: any) {
      console.warn('[合同表单] 提交审批失败', err)
      message.warning(err?.response?.data?.message || err?.message || '已保存，但提交审批失败')
    }
    router.push('/crm/contract')
  } catch (err: any) {
    console.warn('[合同表单] 提交失败', err)
    message.error(err?.response?.data?.message || err?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// ═══ 编辑模式：按字符串 ID 回填（避免 Number(雪花ID) 精度失真） ═══
async function loadDetail() {
  if (!editId.value) return
  try {
    const data: any = await contractApi.getById(editId.value as any)
    if (data) Object.assign(formData, data)
  } catch (err: any) {
    message.error('加载详情失败: ' + (err?.message || ''))
  }
}

// ═══ 快捷键（Ctrl+S 保存 / Ctrl+Enter 提交） ═══
function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); handleSave() }
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSubmit() }
}

// ═══ 页面配置（本页无查询条件，仅「功能按钮」生效） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = []
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'save', label: '保存(Ctrl+S)', enabled: true },
  { key: 'submit', label: '提交(Ctrl+Enter)', enabled: true },
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>([])
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-contract-form-page-config'

function isButtonEnabled(key: string): boolean {
  const found = functionButtonConfig.value.find(b => b.key === key)
  return found ? found.enabled : true
}
function handlePageConfigChange(config: any) {
  if (config?.queryFields) queryFieldsConfig.value = config.queryFields
  if (config?.functionButtons) functionButtonConfig.value = config.functionButtons
}

function handleError(error: Error) {
  console.error('[合同表单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadCustomerOptions()
  loadDetail()
  document.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => document.removeEventListener('keydown', handleKeydown))
</script>

<style scoped>
.footer-right { display: flex; align-items: center; gap: 8px; }
.shortcut-hint { margin-left: 6px; font-size: 12px; color: #909399; }
</style>
