<template>
  <PartnerFormLayout active-key="supplier" page-title="供应商">
    <div class="form-card">
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        layout="vertical"
        hide-required-mark
      >
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="往来单位" name="partnerId">
              <a-select
                v-model:value="form.partnerId"
                placeholder="请选择往来单位"
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
            <a-form-item label="供应商名称" name="supplierName">
              <a-input v-model:value="form.supplierName" placeholder="请输入供应商名称" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="供应商简称" name="shortName">
              <a-input v-model:value="form.shortName" placeholder="请输入供应商简称" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="供应商编码" name="supplierCode">
              <a-input v-model:value="form.supplierCode" placeholder="留空自动生成" size="small">
                <template #suffix>
                  <a-button size="small" type="link" @click="generateCode">重新生成</a-button>
                </template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="供应商类型" name="supplierType">
              <a-select v-model:value="form.supplierType" placeholder="请选择供应商类型" size="small">
                <a-select-option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
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
          <a-col :span="24">
            <a-form-item label="供应商地址" name="address">
              <a-input v-model:value="form.address" placeholder="请输入供应商地址" size="small" />
            </a-form-item>
          </a-col>
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
import { supplierApi } from '@/api/supplier'
import { partnerApi } from '@/api/erp/partner'
import type { Partner } from '@/api/erp/partner'

const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const statusChecked = ref(true)
const partnerOptions = ref<Partner[]>([])

const form = reactive({
  partnerId: undefined as number | undefined,
  supplierName: '',
  shortName: '',
  supplierCode: '',
  supplierType: 1,
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  remark: '',
  status: 1,
  sortOrder: 0,
})

const formRules: Record<string, any> = {
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  contactPhone: [{ pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$/, message: '请输入正确的联系电话', trigger: 'blur' }],
  contactEmail: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
}

const typeOptions = [
  { value: 1, label: '生产型' },
  { value: 2, label: '贸易型' },
  { value: 3, label: '代理型' },
  { value: 4, label: '服务型' },
]

function generateCode() {
  const ts = Date.now().toString(36).slice(-4).toUpperCase()
  const rand = Math.random().toString(36).slice(2, 6).toUpperCase()
  form.supplierCode = `SUP${ts}${rand}`
}

function filterOption(input: string, option: any) {
  const label = option?.children?.[0]?.children || option?.label || ''
  return label.toString().toLowerCase().includes(input.toLowerCase())
}

async function loadPartnerOptions() {
  try {
    const list = await partnerApi.search('', 'SUPPLIER')
    partnerOptions.value = list || []
  } catch {
    partnerOptions.value = []
  }
}

function handleCancel() {
  router.push('/md/supplier')
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  if (!form.supplierCode) {
    generateCode()
  }

  form.status = statusChecked.value ? 1 : 0
  saving.value = true
  try {
    await supplierApi.create({ ...form })
    message.success('供应商创建成功')
    router.push('/md/supplier')
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
</style>
