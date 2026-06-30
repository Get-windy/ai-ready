<template>
  <ErrorBoundary>
    <PageContainer title="企业信息">
      <div class="content-card">
        <a-spin :spinning="loading">
          <a-form :model="form" layout="vertical" class="company-form">
            <a-divider>基本信息</a-divider>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="企业名称" required>
                  <a-input v-model:value="form.companyName" placeholder="请输入企业名称" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="统一社会信用代码">
                  <a-input v-model:value="form.creditCode" placeholder="请输入统一社会信用代码" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="企业类型">
                  <a-select v-model:value="form.companyType" placeholder="请选择">
                    <a-select-option value="有限责任公司">有限责任公司</a-select-option>
                    <a-select-option value="股份有限公司">股份有限公司</a-select-option>
                    <a-select-option value="合伙企业">合伙企业</a-select-option>
                    <a-select-option value="个体工商户">个体工商户</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="法定代表人">
                  <a-input v-model:value="form.legalPerson" placeholder="请输入法定代表人" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="注册资本（万元）">
                  <a-input-number v-model:value="form.registeredCapital" style="width: 100%" :min="0" :precision="2" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="联系电话">
                  <a-input v-model:value="form.contactPhone" placeholder="请输入联系电话" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="企业邮箱">
                  <a-input v-model:value="form.contactEmail" placeholder="请输入企业邮箱" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="企业地址">
              <a-input v-model:value="form.address" placeholder="请输入企业地址" />
            </a-form-item>

            <a-divider>经营信息</a-divider>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="所属行业">
                  <a-select v-model:value="form.industry" placeholder="请选择行业" allow-clear>
                    <a-select-option value="IT/互联网">IT/互联网</a-select-option>
                    <a-select-option value="制造业">制造业</a-select-option>
                    <a-select-option value="批发零售">批发零售</a-select-option>
                    <a-select-option value="物流运输">物流运输</a-select-option>
                    <a-select-option value="金融">金融</a-select-option>
                    <a-select-option value="其他">其他</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="企业规模">
                  <a-select v-model:value="form.companyScale" placeholder="请选择规模">
                    <a-select-option value="小型（<50人）">小型（&lt;50人）</a-select-option>
                    <a-select-option value="中型（50-500人）">中型（50-500人）</a-select-option>
                    <a-select-option value="大型（>500人）">大型（&gt;500人）</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="成立日期">
                  <a-date-picker v-model:value="form.establishDate" style="width: 100%" value-format="YYYY-MM-DD" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="经营范围">
              <a-textarea v-model:value="form.businessScope" :rows="3" placeholder="请输入经营范围" />
            </a-form-item>

            <a-divider>联系信息</a-divider>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="联系人">
                  <a-input v-model:value="form.contactPerson" placeholder="请输入联系人" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="联系人电话">
                  <a-input v-model:value="form.contactPersonPhone" placeholder="请输入联系人电话" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="联系人邮箱">
                  <a-input v-model:value="form.contactPersonEmail" placeholder="请输入联系人邮箱" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-divider />
            <div class="form-footer">
              <a-button type="primary" :loading="saving" @click="handleSave">
                <template #icon><SaveOutlined /></template>
                保存
              </a-button>
              <a-button style="margin-left: 8px" @click="handleResetForm">重置</a-button>
            </div>
          </a-form>
        </a-spin>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SaveOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  companyName: '',
  creditCode: '',
  companyType: undefined as string | undefined,
  legalPerson: '',
  registeredCapital: undefined as number | undefined,
  contactPhone: '',
  contactEmail: '',
  address: '',
  industry: undefined as string | undefined,
  companyScale: undefined as string | undefined,
  establishDate: undefined as string | undefined,
  businessScope: '',
  contactPerson: '',
  contactPersonPhone: '',
  contactPersonEmail: ''
})

async function loadCompanyInfo() {
  loading.value = true
  try {
    // 从租户信息接口获取企业信息
    const tenantId = localStorage.getItem('tenantId') || '1'
    const result = await request.get(`/tenant/${tenantId}`)
    if (result) {
      Object.assign(form, {
        companyName: result.companyName || result.tenantName || '',
        creditCode: result.creditCode || '',
        companyType: result.companyType || undefined,
        legalPerson: result.legalPerson || result.contactPerson || '',
        registeredCapital: result.registeredCapital || undefined,
        contactPhone: result.contactPhone || result.phone || '',
        contactEmail: result.contactEmail || result.email || '',
        address: result.address || '',
        industry: result.industry || undefined,
        companyScale: result.companyScale || undefined,
        establishDate: result.establishDate || undefined,
        businessScope: result.businessScope || '',
        contactPerson: result.contactPerson || '',
        contactPersonPhone: result.contactPersonPhone || '',
        contactPersonEmail: result.contactPersonEmail || ''
      })
    }
  } catch (e) {
    // 失败时使用空表单
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.companyName) {
    message.warning('请输入企业名称')
    return
  }
  saving.value = true
  try {
    const tenantId = localStorage.getItem('tenantId') || '1'
    await request.put(`/tenant/${tenantId}`, form)
    message.success('保存成功')
  } catch (e) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

function handleResetForm() {
  loadCompanyInfo()
}

onMounted(loadCompanyInfo)
</script>

<style scoped>
.content-card { background: #fff; padding: 24px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.company-form { max-width: 960px; margin: 0 auto; }
.form-footer { text-align: center; padding-top: 16px; }
</style>
