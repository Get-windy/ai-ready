<template>
  <ErrorBoundary>
  <PageContainer title="新增供应商">
    <template #headerExtra>
      <a-button @click="handleCancel">返回</a-button>
    </template>

    <a-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      layout="vertical"
      hide-required-mark
      :scroll-to-first-error="true"
    >
      <a-card title="基本信息" style="margin-bottom: 16px">
        <a-row :gutter="24">
          <a-col :span="8">
            <a-form-item label="供应商编码" name="supplierCode">
              <a-input v-model:value="form.supplierCode" placeholder="留空自动生成">
                <template #suffix>
                  <a-button size="small" type="link" @click="generateSupplierCode">重新生成</a-button>
                </template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="供应商名称" name="supplierName">
              <a-input v-model:value="form.supplierName" placeholder="请输入供应商名称" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="简称" name="shortName">
              <a-input v-model:value="form.shortName" placeholder="请输入简称" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="供应商类型" name="supplierType">
              <a-select v-model:value="form.supplierType">
                <a-select-option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="供应商等级" name="supplierLevel">
              <a-select v-model:value="form.supplierLevel">
                <a-select-option v-for="level in levelOptions" :key="level" :value="level">{{ level }}级</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="合作状态" name="cooperationStatus">
              <a-select v-model:value="form.cooperationStatus">
                <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-card>

      <a-card title="联系信息" style="margin-bottom: 16px">
        <a-row :gutter="24">
          <a-col :span="8">
            <a-form-item label="联系人" name="contactPerson">
              <a-input v-model:value="form.contactPerson" placeholder="请输入联系人姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="联系电话" name="contactPhone">
              <a-input v-model:value="form.contactPhone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="form.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="省份" name="province">
              <a-input v-model:value="form.province" placeholder="请输入省份" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="城市" name="city">
              <a-input v-model:value="form.city" placeholder="请输入城市" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="详细地址" name="address">
              <a-input v-model:value="form.address" placeholder="请输入详细地址" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-card>

      <a-card title="财务信息" style="margin-bottom: 16px">
        <a-row :gutter="24">
          <a-col :span="8">
            <a-form-item label="开户银行" name="bankName">
              <a-input v-model:value="form.bankName" placeholder="请输入开户银行" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="银行账号" name="bankAccount">
              <a-input v-model:value="form.bankAccount" placeholder="请输入银行账号" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="税号" name="taxNumber">
              <a-input v-model:value="form.taxNumber" placeholder="请输入纳税人识别号" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-card>

      <a-card title="其他信息">
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="form.remark" placeholder="请输入备注信息" :rows="3" />
        </a-form-item>
      </a-card>

      <div class="form-footer">
        <a-space>
          <a-button @click="handleReset" :disabled="!formDirty">重置</a-button>
          <a-button @click="handleCancel">取消</a-button>
          <a-button v-permission.disabled="'supplier:add'" type="primary" :loading="saving" @click="handleSubmit">
            <template #icon><SaveOutlined /></template>
            提交
          </a-button>
          <span class="submit-hint">Ctrl + Enter</span>
        </a-space>
      </div>
    </a-form>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { SaveOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { supplierApi } from '@/api/supplier'
import { requiredRule, phoneRule, emailRule } from '@/utils/formRules'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'

const SUPPLIER_CODE_PREFIX = 'SUP'

const router = useRouter()

interface SupplierForm {
  supplierCode: string
  supplierName: string
  shortName: string
  supplierType: number
  supplierLevel: string
  cooperationStatus: number
  contactPerson: string
  contactPhone: string
  email: string
  province: string
  city: string
  address: string
  bankName: string
  bankAccount: string
  taxNumber: string
  remark: string
}

const INITIAL_FORM: SupplierForm = {
  supplierCode: '',
  supplierName: '',
  shortName: '',
  supplierType: 1,
  supplierLevel: 'C',
  cooperationStatus: 4,
  contactPerson: '',
  contactPhone: '',
  email: '',
  province: '',
  city: '',
  address: '',
  bankName: '',
  bankAccount: '',
  taxNumber: '',
  remark: ''
}

const formRef = ref<FormInstance>()
const saving = ref(false)
const formDirty = ref(false)
let watchReady = false

const form = reactive<SupplierForm>({ ...INITIAL_FORM })

watch(form, () => {
  if (watchReady) formDirty.value = true
}, { deep: true })

const formRules: Record<string, any> = {
  supplierName: [requiredRule('供应商名称')],
  contactPerson: [requiredRule('联系人')],
  contactPhone: [
    requiredRule('联系电话'),
    phoneRule
  ],
  email: [emailRule],
  bankAccount: [{ pattern: /^\d{16,19}$/, message: '请输入正确的银行账号（16-19位数字）', trigger: 'blur' }]
}

const levelOptions = ['A', 'B', 'C', 'D', 'E']
const typeOptions = [
  { value: 1, label: '生产型' },
  { value: 2, label: '贸易型' }
]
const statusOptions = [
  { value: 1, label: '正常合作' },
  { value: 2, label: '暂停合作' },
  { value: 3, label: '终止合作' },
  { value: 4, label: '潜在供应商' }
]

const generateSupplierCode = () => {
  const ts = Date.now().toString(36).slice(-4).toUpperCase()
  const rand = Math.random().toString(36).slice(2, 6).toUpperCase()
  form.supplierCode = `${SUPPLIER_CODE_PREFIX}${ts}${rand}`
}

const handleReset = () => {
  Object.assign(form, { ...INITIAL_FORM })
  formRef.value?.clearValidate()
  formDirty.value = false
  generateSupplierCode()
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch (err) {
    console.warn('[供应商] 表单验证失败', err)
    return
  }

  if (!form.supplierCode) {
    generateSupplierCode()
  }

  saving.value = true
  try {
    await supplierApi.create({ ...form })
    formDirty.value = false
    console.warn('[供应商] 操作成功: 供应商创建成功')
    message.success('供应商创建成功')
    router.push('/supplier')
  } catch (err: any) {
    console.warn('[供应商] 创建供应商失败', err)
    message.error(err?.response?.data?.message || err?.message || '创建失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const handleCancel = () => {
  router.push('/supplier')
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
  }
}

onBeforeRouteLeave((to, from, next) => {
  if (!formDirty.value) { next(); return }
  Modal.confirm({
    title: '确认离开',
    content: '您有未保存的修改，确定要离开吗？',
    okText: '离开',
    cancelText: '继续编辑',
    onOk: () => next(),
    onCancel: () => next(false)
  })
})

onMounted(() => {
  generateSupplierCode()
  document.addEventListener('keydown', handleKeydown)
  nextTick(() => { watchReady = true })
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.form-footer {
  text-align: right;
  margin-top: 24px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
}
.submit-hint {
  font-size: 12px;
  color: #bbb;
  line-height: 32px;
  vertical-align: middle;
  user-select: none;
}
</style>
