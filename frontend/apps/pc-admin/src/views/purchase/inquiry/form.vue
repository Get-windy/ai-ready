<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <div class="inquiry-form">
        <!-- 顶部操作 -->
        <div class="form-header">
          <div class="form-title">
            {{ isEdit ? '编辑采购询价' : '新增采购询价' }}
            <span v-if="form.inquiryNo" class="form-sub-no">{{ form.inquiryNo }}</span>
          </div>
          <a-space>
            <a-button @click="goBack">返回</a-button>
            <a-button type="primary" :loading="saving" @click="handleSave">保存</a-button>
          </a-space>
        </div>

        <!-- 基本信息 -->
        <a-card size="small" title="基本信息" class="form-card">
          <a-form
            ref="formRef"
            :model="form"
            :rules="rules"
            :label-col="{ span: 7 }"
            :wrapper-col="{ span: 17 }"
          >
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="询价标题" name="title">
                  <a-input v-model:value="form.title" placeholder="请输入询价标题" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="询价类型" name="inquiryType">
                  <a-auto-complete
                    v-model:value="form.inquiryType"
                    :options="inquiryTypeOptions"
                    placeholder="如：物料采购 / 服务采购"
                    allow-clear
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="紧急程度" name="urgencyLevel">
                  <a-select
                    v-model:value="form.urgencyLevel"
                    placeholder="请选择"
                    allow-clear
                    :options="urgencyOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="截止日期" name="deadlineDate">
                  <a-date-picker
                    v-model:value="form.deadlineDate"
                    show-time
                    style="width: 100%"
                    placeholder="报价截止时间"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="预算" name="budget">
                  <a-input-number
                    v-model:value="form.budget"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                    placeholder="含税预算金额"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="部门" name="departmentId">
                  <a-select
                    v-model:value="form.departmentId"
                    placeholder="请选择部门"
                    allow-clear
                    show-search
                    :filter-option="filterOption"
                    :options="departmentOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="需求人" name="requesterId">
                  <a-select
                    v-model:value="form.requesterId"
                    placeholder="请选择需求人"
                    allow-clear
                    show-search
                    :filter-option="filterOption"
                    :options="userOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="采购员" name="purchaserId">
                  <a-select
                    v-model:value="form.purchaserId"
                    placeholder="请选择采购员"
                    allow-clear
                    show-search
                    :filter-option="filterOption"
                    :options="userOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="邀请供应商" name="invitedSupplierIds">
                  <a-select
                    v-model:value="invitedSupplierIds"
                    mode="multiple"
                    placeholder="请选择要邀请报价的供应商"
                    allow-clear
                    show-search
                    :filter-option="filterOption"
                    :options="supplierOptions"
                    :max-tag-count="3"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </a-card>

        <!-- 需求说明 -->
        <a-card size="small" title="需求说明" class="form-card">
          <a-form :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
            <a-form-item label="需求描述">
              <a-textarea
                v-model:value="form.requirementDesc"
                :rows="6"
                placeholder="请描述需采购的物料/服务、规格要求、交付时间等"
                show-count
                :maxlength="2000"
              />
            </a-form-item>
          </a-form>
        </a-card>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 采购询价 - 新增/编辑表单页
 *
 * 背景：询价功能的后端（13 个端点）、权限码（purchase:inquiry:*）、列表页与「详情」路由都齐备，
 * 但**一直没有建单入口**——列表页工具栏只有刷新/配置，点「编辑」也跳到只读的详情页
 * （goEdit 与 goDetail 指向同一个 path）。本页补上这个缺口。
 *
 * 说明：询价单在数据模型里是**纯表头**单据（明细/报价在 purchase_inquiry_item、
 * purchase_supplier_quote 两张表，走报价流程写入），所以这里不需要明细表格，
 * 用「分区卡片 + 两列栅格 + 行内校验」的档案式布局，而不是 BillFormPage 的单据流。
 */
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { inquiryApi } from '@/api/erp'
import optionsApi from '@/api/options'

defineOptions({ name: 'PurchaseInquiryForm' })

const route = useRoute()
const router = useRouter()

const formRef = ref()
const saving = ref(false)

const editId = computed(() => (route.query.id ? String(route.query.id) : ''))
const isEdit = computed(() => !!editId.value)

interface FormState {
  id?: number | string
  inquiryNo: string
  title: string
  inquiryType: string
  urgencyLevel: string | undefined
  deadlineDate: Dayjs | undefined
  budget: number | undefined
  departmentId: number | undefined
  requesterId: number | undefined
  purchaserId: number | undefined
  requirementDesc: string
}

const form = reactive<FormState>({
  inquiryNo: '',
  title: '',
  inquiryType: '',
  urgencyLevel: undefined,
  deadlineDate: undefined,
  budget: undefined,
  departmentId: undefined,
  requesterId: undefined,
  purchaserId: undefined,
  requirementDesc: '',
})

/** 邀请的供应商：后端是逗号分隔字符串，前端用数组更好操作 */
const invitedSupplierIds = ref<(string | number)[]>([])

const rules = {
  title: [{ required: true, message: '请输入询价标题', trigger: 'blur' }],
  inquiryType: [{ required: true, message: '请选择或输入询价类型', trigger: 'change' }],
  deadlineDate: [{ required: true, message: '请选择报价截止时间', trigger: 'change' }],
}

const inquiryTypeOptions = [
  { value: '物料采购' },
  { value: '服务采购' },
  { value: '工程采购' },
  { value: '设备采购' },
]
const urgencyOptions = [
  { label: '普通', value: '普通' },
  { label: '紧急', value: '紧急' },
  { label: '特急', value: '特急' },
]

const userOptions = ref<any[]>([])
const departmentOptions = ref<any[]>([])
const supplierOptions = ref<any[]>([])

const filterOption = (input: string, option: any) =>
  String(option?.label ?? '').toLowerCase().includes(String(input).toLowerCase())

onMounted(async () => {
  loadOptions()
  if (isEdit.value) {
    await loadDetail()
  }
})

async function loadOptions() {
  try {
    const [users, depts, suppliers] = await Promise.all([
      optionsApi.getUsers().catch(() => []),
      optionsApi.getDepartments().catch(() => []),
      optionsApi.getSuppliers().catch(() => []),
    ])
    userOptions.value = (users || []).map((u: any) => ({ label: u.name, value: u.id }))
    departmentOptions.value = (depts || []).map((d: any) => ({ label: d.name, value: d.id }))
    supplierOptions.value = (suppliers || []).map((s: any) => ({ label: s.name, value: s.id }))
  } catch {
    // 下拉取不到不阻断录单，用户可先只填必填项
  }
}

async function loadDetail() {
  try {
    const res: any = await inquiryApi.getById(Number(editId.value))
    const data = res?.data || res || {}
    form.id = data.id
    form.inquiryNo = data.inquiryNo || ''
    form.title = data.title || ''
    form.inquiryType = data.inquiryType || ''
    form.urgencyLevel = data.urgencyLevel || undefined
    // 后端为 LocalDateTime，这里只做展示与提交格式转换
    form.deadlineDate = data.deadlineDate ? (await import('dayjs')).default(data.deadlineDate) : undefined
    form.budget = data.budget ?? undefined
    form.departmentId = data.departmentId ?? undefined
    form.requesterId = data.requesterId ?? undefined
    form.purchaserId = data.purchaserId ?? undefined
    form.requirementDesc = data.requirementDesc || ''
    invitedSupplierIds.value = data.invitedSupplierIds
      ? String(data.invitedSupplierIds).split(',').filter(Boolean)
      : []
  } catch {
    message.error('加载询价单失败')
  }
}

function buildPayload() {
  return {
    title: form.title,
    inquiryType: form.inquiryType,
    urgencyLevel: form.urgencyLevel,
    deadlineDate: form.deadlineDate ? form.deadlineDate.format('YYYY-MM-DDTHH:mm:ss') : null,
    budget: form.budget,
    departmentId: form.departmentId,
    requesterId: form.requesterId,
    purchaserId: form.purchaserId,
    requirementDesc: form.requirementDesc,
    invitedSupplierIds: invitedSupplierIds.value.join(','),
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await inquiryApi.update(Number(editId.value), buildPayload())
      message.success('保存成功')
    } else {
      await inquiryApi.create(buildPayload())
      message.success('创建成功')
    }
    goBack()
  } catch (err: any) {
    message.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/purchase/inquiry')
}

function handleError(err: unknown) {
  console.error('[采购询价表单] 页面异常', err)
}
</script>

<style scoped>
.inquiry-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  height: 100%;
  overflow: auto;
}
.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.form-title {
  font-size: 16px;
  font-weight: 600;
}
.form-sub-no {
  margin-left: 8px;
  font-size: 13px;
  font-weight: 400;
  color: #8c8c8c;
}
.form-card {
  background: #fff;
}
</style>
