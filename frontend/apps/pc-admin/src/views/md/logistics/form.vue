<template>
  <PartnerFormLayout active-key="logistics" page-title="物流公司">
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
            <a-form-item label="物流公司名称" name="logisticsName">
              <a-input v-model:value="form.logisticsName" placeholder="请输入物流公司名称" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="物流公司简称" name="shortName">
              <a-input v-model:value="form.shortName" placeholder="请输入物流公司简称" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="物流公司编码" name="logisticsCode">
              <a-input v-model:value="form.logisticsCode" placeholder="留空自动生成" size="small">
                <template #suffix>
                  <a-button size="small" type="link" @click="generateCode">重新生成</a-button>
                </template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="物流类型" name="logisticsType">
              <a-select v-model:value="form.logisticsType" placeholder="请选择物流类型" size="small">
                <a-select-option v-for="item in logisticsTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="服务区域" name="serviceArea">
              <a-input v-model:value="form.serviceArea" placeholder="如：全国/华东地区/广东省" size="small" />
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
            <a-form-item label="客服电话" name="servicePhone">
              <a-input v-model:value="form.servicePhone" placeholder="请输入客服电话" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="公司地址" name="address">
              <a-input v-model:value="form.address" placeholder="请输入公司地址" size="small" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 运输能力 -->
        <a-divider orientation="left" style="margin: 8px 0 16px">运输能力</a-divider>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="运输方式" name="transportMode">
              <a-select v-model:value="form.transportMode" placeholder="请选择运输方式" size="small" mode="multiple">
                <a-select-option value="ROAD">公路运输</a-select-option>
                <a-select-option value="RAIL">铁路运输</a-select-option>
                <a-select-option value="AIR">航空运输</a-select-option>
                <a-select-option value="SEA">海运</a-select-option>
                <a-select-option value="MULTIMODAL">多式联运</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="车辆数量" name="vehicleCount">
              <a-input-number v-model:value="form.vehicleCount" :min="0" style="width: 100%" size="small" placeholder="请输入车辆数量" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否支持冷链" name="coldChain">
              <a-switch v-model:checked="form.coldChain" checked-children="是" un-checked-children="否" size="small" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否支持危化品" name="hazardous">
              <a-switch v-model:checked="form.hazardous" checked-children="是" un-checked-children="否" size="small" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 财务信息 -->
        <a-divider orientation="left" style="margin: 8px 0 16px">结算信息</a-divider>
        <a-row :gutter="24">
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
import request from '@/utils/request'

const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const statusChecked = ref(true)
const partnerOptions = ref<Partner[]>([])

const form = reactive({
  partnerId: undefined as number | undefined,
  logisticsName: '',
  shortName: '',
  logisticsCode: '',
  logisticsType: 1,
  serviceArea: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  servicePhone: '',
  address: '',
  transportMode: [] as string[],
  vehicleCount: 0,
  coldChain: false,
  hazardous: false,
  settleType: 'MONTHLY',
  taxRate: 9,
  remark: '',
  status: 1,
  sortOrder: 0,
})

const formRules: Record<string, any> = {
  logisticsName: [{ required: true, message: '请输入物流公司名称', trigger: 'blur' }],
  contactPhone: [{ pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$/, message: '请输入正确的联系电话', trigger: 'blur' }],
  contactEmail: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
}

const logisticsTypeOptions = [
  { value: 1, label: '快递物流' },
  { value: 2, label: '零担物流' },
  { value: 3, label: '整车运输' },
  { value: 4, label: '冷链物流' },
  { value: 5, label: '危化品运输' },
  { value: 6, label: '综合物流' },
]

function generateCode() {
  const ts = Date.now().toString(36).slice(-4).toUpperCase()
  const rand = Math.random().toString(36).slice(2, 6).toUpperCase()
  form.logisticsCode = `LOG${ts}${rand}`
}

function filterOption(input: string, option: any) {
  const label = option?.children?.[0]?.children || option?.label || ''
  return label.toString().toLowerCase().includes(input.toLowerCase())
}

async function loadPartnerOptions() {
  try {
    const list = await partnerApi.search('', 'OTHER')
    partnerOptions.value = list || []
  } catch {
    partnerOptions.value = []
  }
}

function handleCancel() {
  router.push('/md/logistics')
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  if (!form.logisticsCode) {
    generateCode()
  }

  form.status = statusChecked.value ? 1 : 0
  saving.value = true
  try {
    await request.post('/logistics', { ...form })
    message.success('物流公司创建成功')
    router.push('/md/logistics')
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
