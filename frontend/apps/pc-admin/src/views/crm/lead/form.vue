<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        线索表单页（CRM → 线索管理 → 线索 的「添加」标签入口；路由 crm/lead/form）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/线索开发文档.md §4.2
        · 外壳：ErrorBoundary > PageContainer(full-height) > BillFormPage（与本模块其它表单页一致）
        · 表单为「基础信息流式表单」（无明细区、无底部 Tab），字段名与后端 CustomerLead 实体逐一对齐
        · 路由带 ?id= 时进入编辑模式：useBasicForm 自行 loadDetail + update
        · 本轮改造：
          ① 补 ErrorBoundary + PageContainer 外壳与样式一致性（原文件只有裸 BillFormPage）
          ② 修「v-model 直绑 reactive 常量」陷阱（《billformpage-v-model-reactive-trap》）——
             原先 `v-model="formData"` 而 formData 来自 useBasicForm 的 reactive 常量，
             更新事件会写到常量上，输入能打字但状态不回流 ⇒ 改为 :model-value + @update:model-value
          ③ 提交不再复用 composable 的 handleSubmit（它注入的是 `status: 1`，而线索状态字段是
             `leadStatus`，注入被 Jackson 静默忽略）——改为本页显式推进 leadStatus
          ④ 「负责销售」由纯文本改为平台用户下拉，同时回写 salesPersonId + salesPersonName
          ⑤ 补「预计成交日期」（实体已有 expected_close_date 列）
      -->
      <BillFormPage
        :model-value="formData"
        :header="{ title: isEdit ? '编辑线索' : '新增线索' }"
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
              :loading="saving"
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
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import { useBasicForm } from '@/components/BillFormPage/useBasicForm'
import type { BasicInfoField } from '@/components/BillFormPage/types'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import { leadApi } from '@/api/crm'
import { optionsApi } from '@/api/options'

defineOptions({ name: 'CrmLeadForm' })

// ═══ 字段定义（与后端 CustomerLead 实体字段逐一对齐；选项值域为数字，见线索开发文档 §4.2） ═══
const BASE_FIELDS: BasicInfoField[] = [
  { key: 'leadCode', label: '线索编号', type: 'input', disabled: true },
  { key: 'leadName', label: '线索名称', type: 'input', required: true },
  { key: 'companyName', label: '公司名称', type: 'input', required: true },
  { key: 'contactName', label: '联系人', type: 'input' },
  { key: 'contactPhone', label: '联系电话', type: 'input' },
  { key: 'contactEmail', label: '联系邮箱', type: 'input' },
  {
    key: 'industryType', label: '所属行业', type: 'select',
    options: [
      { label: '食品加工', value: 1 },
      { label: '餐饮服务', value: 2 },
      { label: '批发零售', value: 3 },
      { label: '其他', value: 4 },
    ],
  },
  {
    key: 'leadSource', label: '线索来源', type: 'select',
    options: [
      { label: '网络推广', value: 1 },
      { label: '客户介绍', value: 2 },
      { label: '电话营销', value: 3 },
      { label: '展会', value: 4 },
      { label: '其他', value: 5 },
    ],
  },
  {
    key: 'leadLevel', label: '线索等级', type: 'select',
    options: [
      { label: '高', value: 1 },
      { label: '中', value: 2 },
      { label: '低', value: 3 },
    ],
  },
  { key: 'estimatedAmount', label: '预计金额', type: 'number', precision: 2 },
  { key: 'expectedCloseDate', label: '预计成交日期', type: 'date' },
  { key: 'province', label: '省份', type: 'input' },
  { key: 'city', label: '城市', type: 'input' },
  { key: 'address', label: '详细地址', type: 'input', width: 'wide' },
  { key: 'requirement', label: '需求描述', type: 'textarea', width: 'wide' },
  // 负责销售：下拉取平台用户，选中后同时回写 salesPersonId + salesPersonName（见 handleModelUpdate）
  { key: 'salesPersonId', label: '负责销售', type: 'select', options: [] },
  {
    key: 'leadStatus', label: '状态', type: 'select',
    options: [
      { label: '新线索', value: 0 },
      { label: '跟进中', value: 1 },
      { label: '已转化', value: 2 },
      { label: '已关闭', value: 3 },
    ],
  },
  { key: 'remark', label: '备注', type: 'textarea', width: 'wide' },
]

const { formData, saving, effectiveMode, handleSave } = useBasicForm({
  api: { create: leadApi.create, update: leadApi.update, getById: leadApi.getById },
  redirectPath: '/crm/lead',
  fields: BASE_FIELDS,
})

const isEdit = computed(() => effectiveMode.value === 'edit')

// ═══ 负责销售下拉（平台用户；异步加载后经 computed 注入字段配置） ═══
const salesOptions = ref<{ label: string; value: number }[]>([])

const formFields = computed<BasicInfoField[]>(() =>
  BASE_FIELDS.map(f => (f.key === 'salesPersonId' ? { ...f, options: salesOptions.value } : f))
)

async function loadSalesOptions() {
  try {
    const users: any = await optionsApi.getUsers()
    const list = (users as any)?.data || users || []
    salesOptions.value = (Array.isArray(list) ? list : []).map((u: any) => ({
      label: u.nickname || u.name || u.realName || u.username || String(u.id),
      value: u.id,
    }))
  } catch (e) {
    console.warn('[线索表单] 销售人员下拉获取失败', e)
    salesOptions.value = []
  }
}

/**
 * BillFormPage 每次编辑都以「新对象」emit（{ ...modelValue, [key]: val }），
 * 故这里合并回 reactive 的 formData（不可整体替换——它是 composable 持有的常量）。
 */
function handleModelUpdate(next: Record<string, any>) {
  const prevSalesId = formData.salesPersonId
  Object.assign(formData, next || {})
  if (formData.salesPersonId !== prevSalesId) {
    formData.salesPersonName = salesOptions.value.find(o => String(o.value) === String(formData.salesPersonId))?.label
  }
}

/**
 * 提交：新建时把线索状态推进到「跟进中(1)」；编辑时不覆盖用户已选状态。
 * ⚠️ 不复用 composable 的 handleSubmit —— 它注入的是 `status: 1`，而线索实体的状态字段是
 * `leadStatus`（注入会被 Jackson 静默忽略），属通用 composable 与线索域的字段名错配，已登记缺口。
 */
function handleSubmit() {
  if (!isEdit.value) formData.leadStatus = 1
  handleSave()
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
const pageConfigStorageKey = 'crm-lead-form-page-config'

function isButtonEnabled(key: string): boolean {
  const found = functionButtonConfig.value.find(b => b.key === key)
  return found ? found.enabled : true
}
function handlePageConfigChange(config: any) {
  if (config?.queryFields) queryFieldsConfig.value = config.queryFields
  if (config?.functionButtons) functionButtonConfig.value = config.functionButtons
}

function handleError(error: Error) {
  console.error('[线索表单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadSalesOptions()
})
</script>

<style scoped>
.footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.shortcut-hint {
  margin-left: 6px;
  font-size: 12px;
  color: #909399;
}
</style>
