<template>
  <PageContainer title="新增供应商">
    <template #headerExtra>
      <a-button @click="handleCancel">返回</a-button>
    </template>

    <a-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      layout="vertical"
    >
      <a-card title="基本信息" style="margin-bottom: 16px">
        <a-row :gutter="24">
          <a-col :span="8">
            <a-form-item label="供应商编码" name="supplierCode">
              <a-input v-model:value="form.supplierCode" placeholder="留空自动生成">
                <template #suffix>
                  <a-button size="small" type="link" @click="generateSupplierCode">自动生成</a-button>
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

      <div style="text-align: right; margin-top: 24px">
        <a-space>
          <a-button @click="handleCancel">取消</a-button>
          <a-button type="primary" :loading="saving" @click="handleSubmit">提交</a-button>
        </a-space>
      </div>
    </a-form>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { supplierApi } from '@/api/supplier'
import { requiredRule, phoneRule, emailRule } from '@/utils/formRules'
import PageContainer from '@/components/PageContainer/PageContainer.vue'

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

const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive<SupplierForm>({
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
})

const formRules: Record<string, any> = {
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    phoneRule
  ],
  email: [emailRule],
  bankAccount: [{ pattern: /^\d{16,19}$/, message: '请输入正确的银行账号', trigger: 'blur' }]
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
  const timestamp = Date.now().toString().slice(-6)
  form.supplierCode = `SUP${timestamp}`
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  if (!form.supplierCode) {
    generateSupplierCode()
  }

  saving.value = true
  try {
    await supplierApi.create({ ...form })
    message.success('供应商创建成功')
    router.push('/supplier')
  } catch {
    message.error('创建失败')
  } finally {
    saving.value = false
  }
}

const handleCancel = () => {
  router.push('/supplier')
}
</script>
