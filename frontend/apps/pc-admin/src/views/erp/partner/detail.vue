<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">ERP / 往来单位管理 / {{ isNew ? '新增' : '编辑' }}</span>
          <h2 class="page-header__title">{{ isNew ? '新增单位' : form.partnerName || '编辑单位' }}</h2>
        </div>
        <div class="page-header__right">
          <a-space>
            <a-button @click="goBack">取消</a-button>
            <a-button type="primary" :loading="saving" @click="handleSave">
              <SaveOutlined /> 保存
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <div class="detail-body">
      <a-spin :spinning="loading">
        <!-- 基本信息 -->
        <a-card title="基本信息" class="detail-card">
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="单位编码" required>
                <a-input v-model:value="form.partnerCode" placeholder="自动生成或手动输入" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="单位名称" required>
                <a-input v-model:value="form.partnerName" placeholder="请输入单位名称" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="简称">
                <a-input v-model:value="form.partnerShortName" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="单位类型" required>
                <a-select v-model:value="form.partnerType" size="small">
                  <a-select-option value="CUSTOMER">客户</a-select-option>
                  <a-select-option value="SUPPLIER">供应商</a-select-option>
                  <a-select-option value="BOTH">购销(客户+供应商)</a-select-option>
                  <a-select-option value="OTHER">其他</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="分类">
                <a-tree-select
                  v-model:value="form.partnerCategoryId"
                  :tree-data="categoryTree"
                  :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
                  placeholder="请选择分类"
                  allow-clear
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="等级">
                <a-select v-model:value="form.partnerGradeId" placeholder="请选择等级" allow-clear size="small">
                  <a-select-option v-for="g in grades" :key="g.id" :value="g.id">{{ g.gradeName }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="状态">
                <a-switch v-model:checked="statusChecked" checked-children="启用" un-checked-children="停用" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <!-- 工商信息 -->
        <a-card title="工商信息" class="detail-card">
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="统一社会信用代码">
                <a-input v-model:value="form.unifiedSocialCode" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="税务登记号">
                <a-input v-model:value="form.taxId" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="法定代表人">
                <a-input v-model:value="form.legalPerson" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="注册资本(元)">
                <a-input-number v-model:value="form.registeredCapital" :precision="2" :min="0" style="width:100%" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="公司电话">
                <a-input v-model:value="form.companyPhone" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="公司邮箱">
                <a-input v-model:value="form.companyEmail" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <!-- 联系人信息 -->
        <a-card title="联系人信息" class="detail-card">
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="默认联系人">
                <a-input v-model:value="form.contactPerson" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="联系电话">
                <a-input v-model:value="form.contactPhone" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="联系邮箱">
                <a-input v-model:value="form.contactEmail" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <!-- 财务/结算 -->
        <a-card title="财务与结算信息" class="detail-card">
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="信用额度(元)">
                <a-input-number v-model:value="form.creditLimit" :precision="2" :min="0" style="width:100%" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="账期天数">
                <a-input-number v-model:value="form.creditDays" :min="0" style="width:100%" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="税率(%)">
                <a-input-number v-model:value="form.taxRate" :precision="2" :min="0" :max="100" style="width:100%" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="结算方式">
                <a-select v-model:value="form.settleType" size="small">
                  <a-select-option value="MONTHLY">月结</a-select-option>
                  <a-select-option value="WEEKLY">周结</a-select-option>
                  <a-select-option value="CASH">现结</a-select-option>
                  <a-select-option value="ADVANCE">预付</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="期初欠款(元)">
                <a-input-number v-model:value="form.openingBalance" :precision="2" style="width:100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <!-- 备注 -->
        <a-card title="备注" class="detail-card">
          <a-textarea v-model:value="form.remark" :rows="3" size="small" />
        </a-card>

        <!-- 扩展信息(仅编辑时显示) -->
        <a-card v-if="!isNew" title="扩展信息" class="detail-card">
          <a-tabs v-model:active-key="extTabKey" size="small">
            <a-tab-pane key="contacts" tab="联系人">
              <PartnerContactsPanel :partner-id="partnerId" />
            </a-tab-pane>
            <a-tab-pane key="addresses" tab="地址">
              <PartnerAddressesPanel :partner-id="partnerId" />
            </a-tab-pane>
            <a-tab-pane key="banks" tab="银行账户">
              <PartnerBanksPanel :partner-id="partnerId" />
            </a-tab-pane>
            <a-tab-pane key="tags" tab="标签">
              <PartnerTagsPanel :partner-id="partnerId" />
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </a-spin>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SaveOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { partnerApi, partnerCategoryApi, partnerGradeApi } from '@/api/erp/partner'
import type { PartnerCategory, PartnerGrade } from '@/api/erp/partner'
import PartnerContactsPanel from './components/PartnerContactsPanel.vue'
import PartnerAddressesPanel from './components/PartnerAddressesPanel.vue'
import PartnerBanksPanel from './components/PartnerBanksPanel.vue'
import PartnerTagsPanel from './components/PartnerTagsPanel.vue'

const route = useRoute()
const router = useRouter()

const partnerId = computed(() => route.params.id ? Number(route.params.id) : 0)
const isNew = computed(() => !partnerId.value || route.path.includes('/create'))

const loading = ref(false)
const saving = ref(false)
const extTabKey = ref('contacts')

const statusChecked = ref(true)

const form = reactive({
  partnerCode: '',
  partnerName: '',
  partnerShortName: '',
  partnerType: 'CUSTOMER',
  partnerCategoryId: undefined as number | undefined,
  partnerGradeId: undefined as number | undefined,
  unifiedSocialCode: '',
  taxId: '',
  legalPerson: '',
  registeredCapital: undefined as number | undefined,
  companyPhone: '',
  companyEmail: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  creditLimit: undefined as number | undefined,
  creditDays: undefined as number | undefined,
  taxRate: 13,
  settleType: 'MONTHLY',
  openingBalance: undefined as number | undefined,
  remark: '',
  status: 'ENABLED'
})

const categoryTree = ref<PartnerCategory[]>([])
const grades = ref<PartnerGrade[]>([])

async function init() {
  loading.value = true
  try {
    const [cats, gds] = await Promise.all([
      partnerCategoryApi.getTree(),
      partnerGradeApi.list()
    ])
    categoryTree.value = cats
    grades.value = gds

    if (!isNew.value && partnerId.value) {
      const partner = await partnerApi.getById(partnerId.value)
      Object.assign(form, {
        partnerCode: partner.partnerCode,
        partnerName: partner.partnerName,
        partnerShortName: partner.partnerShortName || '',
        partnerType: partner.partnerType,
        partnerCategoryId: partner.partnerCategoryId,
        partnerGradeId: partner.partnerGradeId,
        unifiedSocialCode: partner.unifiedSocialCode || '',
        taxId: partner.taxId || '',
        legalPerson: partner.legalPerson || '',
        registeredCapital: partner.registeredCapital,
        companyPhone: partner.companyPhone || '',
        companyEmail: partner.companyEmail || '',
        contactPerson: partner.contactPerson || '',
        contactPhone: partner.contactPhone || '',
        contactEmail: partner.contactEmail || '',
        creditLimit: partner.creditLimit,
        creditDays: partner.creditDays,
        taxRate: partner.taxRate ?? 13,
        settleType: partner.settleType || 'MONTHLY',
        openingBalance: partner.openingBalance,
        remark: partner.remark || '',
        status: partner.status || 'ENABLED'
      })
      statusChecked.value = form.status === 'ENABLED'
    }
  } catch {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.partnerCode) { message.warning('请输入单位编码'); return }
  if (!form.partnerName) { message.warning('请输入单位名称'); return }

  form.status = statusChecked.value ? 'ENABLED' : 'DISABLED'
  saving.value = true
  try {
    if (isNew.value) {
      await partnerApi.create({ ...form } as any)
      message.success('创建成功')
    } else {
      await partnerApi.update(partnerId.value, { ...form } as any)
      message.success('保存成功')
    }
    goBack()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '保存失败'
    message.error(msg)
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/erp/partner')
}

onMounted(init)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.page-header__left { display: flex; flex-direction: column; gap: 2px; }
.page-header__breadcrumb { font-size: 12px; color: #999; }
.page-header__title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }

.detail-body {
  overflow-y: auto;
  padding-bottom: 24px;
}

.detail-card {
  margin-bottom: 16px;
  border-radius: 8px;
}
:deep(.ant-card-body) { padding: 16px; }
:deep(.ant-form-item) { margin-bottom: 12px; }

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
