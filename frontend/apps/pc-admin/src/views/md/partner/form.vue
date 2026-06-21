<template>
  <PartnerFormLayout active-key="partner" page-title="其他往来单位">
    <div class="form-card">
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        layout="vertical"
        hide-required-mark
      >
        <!-- 基本信息 -->
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="已有往来单位" name="partnerId">
              <a-select
                v-model:value="form.partnerId"
                placeholder="可选择已有往来单位关联"
                show-search
                :filter-option="filterOption"
                size="small"
                allow-clear
              >
                <a-select-option v-for="p in partnerOptions" :key="p.id" :value="p.id">
                  {{ p.partnerName }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单位名称" name="partnerName">
              <a-input v-model:value="form.partnerName" placeholder="请输入单位名称" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单位简称" name="shortName">
              <a-input v-model:value="form.shortName" placeholder="请输入单位简称" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单位编码" name="partnerCode">
              <a-input v-model:value="form.partnerCode" placeholder="留空自动生成" size="small">
                <template #suffix>
                  <a-button size="small" type="link" @click="generateCode">重新生成</a-button>
                </template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="往来单位类别" name="category">
              <a-select v-model:value="form.category" placeholder="请选择类别" size="small">
                <a-select-option v-for="item in categoryOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合作关系" name="relationType">
              <a-select v-model:value="form.relationType" placeholder="请选择合作关系" size="small">
                <a-select-option v-for="item in relationOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 联系信息 -->
        <a-divider orientation="left" style="margin: 8px 0 16px">联系信息</a-divider>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="联系人" name="contactPerson">
              <a-input v-model:value="form.contactPerson" placeholder="请输入联系人" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="contactPhone">
              <a-input v-model:value="form.contactPhone" placeholder="请输入联系电话" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系邮箱" name="contactEmail">
              <a-input v-model:value="form.contactEmail" placeholder="请输入联系邮箱" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单位网址" name="website">
              <a-input v-model:value="form.website" placeholder="请输入单位网址" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="单位地址" name="address">
              <a-input v-model:value="form.address" placeholder="请输入单位地址" size="small" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 业务信息 -->
        <a-divider orientation="left" style="margin: 8px 0 16px">业务信息</a-divider>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="统一社会信用代码" name="unifiedSocialCode">
              <a-input v-model:value="form.unifiedSocialCode" placeholder="请输入统一社会信用代码" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="纳税人识别号" name="taxId">
              <a-input v-model:value="form.taxId" placeholder="请输入纳税人识别号" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="开户银行" name="bankName">
              <a-input v-model:value="form.bankName" placeholder="请输入开户银行" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="银行账号" name="bankAccount">
              <a-input v-model:value="form.bankAccount" placeholder="请输入银行账号" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结算方式" name="settleType">
              <a-select v-model:value="form.settleType" size="small">
                <a-select-option value="MONTHLY">月结</a-select-option>
                <a-select-option value="WEEKLY">周结</a-select-option>
                <a-select-option value="CASH">现结</a-select-option>
                <a-select-option value="ADVANCE">预付</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="税率(%)" name="taxRate">
              <a-input-number v-model:value="form.taxRate" :precision="2" :min="0" :max="100" style="width: 100%" size="small" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 备注和状态 -->
        <a-row :gutter="24">
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="form.remark" placeholder="请输入备注" :rows="3" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-switch v-model:checked="statusChecked" checked-children="启用" un-checked-children="停用" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序" name="sortOrder">
              <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" size="small" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <div class="form-footer">
        <a-space>
          <a-button @click="handleCancel">取消</a-button>
          <a-button type="primary" :loading="saving" @click="handleSubmit">确定</a-button>
        </a-space>
      </div>
    </div>
  </PartnerFormLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import PartnerFormLayout from '../components/PartnerFormLayout.vue'
import { partnerApi } from '@/api/erp/partner'
import type { Partner } from '@/api/erp/partner'

const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const statusChecked = ref(true)
const partnerOptions = ref<Partner[]>([])

const form = reactive({
  partnerId: undefined as number | undefined,
  partnerName: '',
  shortName: '',
  partnerCode: '',
  category: '',
  relationType: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  website: '',
  address: '',
  unifiedSocialCode: '',
  taxId: '',
  bankName: '',
  bankAccount: '',
  settleType: 'MONTHLY',
  taxRate: 13,
  remark: '',
  status: 1,
  sortOrder: 0,
})

const formRules: Record<string, any> = {
  partnerName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择单位类别', trigger: 'change' }],
  contactPhone: [{ pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$/, message: '请输入正确的联系电话', trigger: 'blur' }],
  contactEmail: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
}

const categoryOptions = [
  { value: 'REPAIR', label: '配套维修合作方' },
  { value: 'TRAINING', label: '培训公司' },
  { value: 'FINANCE', label: '财务公司' },
  { value: 'LEGAL', label: '法律服务' },
  { value: 'INSURANCE', label: '保险公司' },
  { value: 'IT', label: 'IT服务商' },
  { value: 'CONSULTING', label: '咨询公司' },
  { value: 'WAREHOUSE', label: '仓储合作方' },
  { value: 'INSPECTION', label: '质检机构' },
  { value: 'OTHER', label: '其他' },
]

const relationOptions = [
  { value: 'COOPERATION', label: '合作伙伴' },
  { value: 'VENDOR', label: '服务提供商' },
  { value: 'SUBCONTRACTOR', label: '分包商' },
  { value: 'FRANCHISE', label: '加盟商' },
  { value: 'AGENT', label: '代理商' },
  { value: 'OTHER', label: '其他' },
]

function generateCode() {
  const ts = Date.now().toString(36).slice(-4).toUpperCase()
  const rand = Math.random().toString(36).slice(2, 6).toUpperCase()
  form.partnerCode = `PTR${ts}${rand}`
}

function filterOption(input: string, option: any) {
  const label = option?.children?.[0]?.children || option?.label || ''
  return label.toString().toLowerCase().includes(input.toLowerCase())
}

async function loadPartnerOptions() {
  try {
    const list = await partnerApi.search('')
    partnerOptions.value = list || []
  } catch {
    partnerOptions.value = []
  }
}

function handleCancel() {
  router.push('/md/partner')
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  if (!form.partnerCode) {
    generateCode()
  }

  form.status = statusChecked.value ? 1 : 0
  saving.value = true
  try {
    await partnerApi.create({
      partnerCode: form.partnerCode,
      partnerName: form.partnerName,
      partnerShortName: form.shortName,
      partnerType: 'OTHER',
      contactPerson: form.contactPerson,
      contactPhone: form.contactPhone,
      contactEmail: form.contactEmail,
      detailAddress: form.address,
      unifiedSocialCode: form.unifiedSocialCode,
      taxId: form.taxId,
      settleType: form.settleType,
      taxRate: form.taxRate,
      remark: form.remark,
      status: form.status === 1 ? 'ENABLED' : 'DISABLED',
      ...form,
    })
    message.success('往来单位创建成功')
    router.push('/md/partner')
  } catch (err: any) {
    message.error(err?.response?.data?.message || err?.message || '创建失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
  }
}

onMounted(() => {
  generateCode()
  loadPartnerOptions()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.form-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.form-footer {
  text-align: right;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

:deep(.ant-form-item) {
  margin-bottom: 14px;
}
:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #595959;
}
:deep(.ant-divider-inner-text) {
  font-size: 13px;
  color: #8c8c8c;
  font-weight: 500;
}
</style>
