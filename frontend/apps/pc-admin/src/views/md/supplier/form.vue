<template>
  <div class="form-page-wrapper">
    <div class="page-header">
      <a-space>
        <a-button
          type="text"
          size="small"
          @click="handleCancel"
        >
          <template #icon>
            <ArrowLeftOutlined />
          </template>
          返回
        </a-button>
        <span class="page-title">供应商新增</span>
      </a-space>
    </div>

    <!-- 其他业务关系 -->
    <div class="top-options">
      <span style="font-size:13px;color:#595959;margin-right:16px">该往来单位还有其他业务关系：</span>
      <a-checkbox
        v-for="opt in otherRoleOptions"
        :key="opt.value"
        v-model:checked="opt.checked"
        style="margin-right:16px"
      >
        {{ opt.label }}
      </a-checkbox>
      <a-checkbox
        v-if="hasCustomerRole"
        v-model:checked="enablePriceTrack"
        style="margin-left:24px"
      >
        启用销售价格跟踪
      </a-checkbox>
    </div>

    <div class="form-scroll-area">
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        layout="horizontal"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 18 }"
      >
        <!-- 基础信息 -->
        <div class="section-card">
          <div class="section-title">
            基础信息
          </div>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="16">
              <a-row :gutter="24">
                <a-col :span="12">
                  <a-form-item
                    label="供应商名称"
                    name="partnerName"
                  >
                    <a-input
                      v-model:value="form.partnerName"
                      placeholder="请输入供应商名称"
                      size="small"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item
                    label="供应商编号"
                    name="partnerCode"
                  >
                    <a-input
                      v-model:value="form.partnerCode"
                      placeholder="由系统自动生成"
                      size="small"
                      disabled
                    />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="24">
                <a-col :span="12">
                  <a-form-item
                    label="所属分类"
                    name="partnerCategoryId"
                  >
                    <a-select
                      v-model:value="form.partnerCategoryId"
                      placeholder="请选择分类"
                      allow-clear
                      size="small"
                    >
                      <a-select-option
                        v-for="c in categories"
                        :key="c.id"
                        :value="c.id"
                      >
                        {{ c.categoryName }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item
                    label="供应商级别"
                    name="partnerGradeId"
                  >
                    <a-select
                      v-model:value="form.partnerGradeId"
                      placeholder="请选择级别"
                      allow-clear
                      size="small"
                    >
                      <a-select-option
                        v-for="g in grades"
                        :key="g.id"
                        :value="g.id"
                      >
                        {{ g.gradeName }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="24">
                <a-col :span="12">
                  <a-form-item
                    label="默认经手人"
                    name="defaultHandlerId"
                  >
                    <a-select
                      v-model:value="form.defaultHandlerId"
                      placeholder="请选择经手人"
                      allow-clear
                      size="small"
                      show-search
                      :filter-option="filterUser"
                    >
                      <a-select-option
                        v-for="u in users"
                        :key="u.id"
                        :value="u.id"
                        :label="u.nickname || u.username"
                      >
                        {{ u.nickname || u.username }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item
                    label="所属仓库"
                    name="defaultWarehouseId"
                  >
                    <a-select
                      v-model:value="form.defaultWarehouseId"
                      placeholder="请选择仓库"
                      allow-clear
                      size="small"
                    >
                      <a-select-option
                        v-for="w in warehouses"
                        :key="w.id"
                        :value="w.id"
                      >
                        {{ w.warehouseName }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="24">
                <a-col :span="8">
                  <a-form-item label="所在省">
                    <a-input
                      v-model:value="form.province"
                      placeholder="省"
                      size="small"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="所在市">
                    <a-input
                      v-model:value="form.city"
                      placeholder="市"
                      size="small"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="所在区/县">
                    <a-input
                      v-model:value="form.district"
                      placeholder="区/县"
                      size="small"
                    />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="24">
                <a-col :span="12">
                  <a-form-item label="助记码">
                    <a-input
                      v-model:value="form.partnerShortName"
                      placeholder="输入拼音首字母等"
                      size="small"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item
                    label="结算方式"
                    name="settleType"
                  >
                    <a-select
                      v-model:value="form.settleType"
                      size="small"
                    >
                      <a-select-option value="MONTHLY">
                        月结
                      </a-select-option>
                      <a-select-option value="WEEKLY">
                        周结
                      </a-select-option>
                      <a-select-option value="CASH">
                        现结
                      </a-select-option>
                      <a-select-option value="ADVANCE">
                        预付
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="24">
                <a-col :span="12">
                  <a-form-item label="状态">
                    <a-switch
                      v-model:checked="statusChecked"
                      checked-children="启用"
                      un-checked-children="停用"
                      size="small"
                    />
                  </a-form-item>
                </a-col>
              </a-row>
            </a-col>
            <a-col
              :span="8"
              style="text-align:center"
            >
              <div class="avatar-upload">
                <div
                  class="avatar-placeholder"
                  @click="avatarFileInput.click()"
                >
                  <img
                    v-if="avatarUrl"
                    :src="avatarUrl"
                    class="avatar-preview"
                  >
                  <div
                    v-else
                    class="avatar-empty"
                  >
                    <UserOutlined style="font-size:48px;color:#d9d9d9" />
                  </div>
                </div>
                <input
                  ref="avatarFileInput"
                  type="file"
                  accept="image/*"
                  hidden
                  @change="(e)=>handleAvatarUpload(e)"
                >
                <a-button
                  size="small"
                  style="margin-top:8px"
                  @click="avatarFileInput?.click()"
                >
                  上传
                </a-button>
              </div>
            </a-col>
          </a-row>
        </div>

        <!-- 联系人 -->
        <div class="section-card">
          <ContactList
            :contacts="contacts"
            :show-mall-account="showMallAccount"
            @update="contacts = $event"
          />
        </div>

        <!-- 纳税人信息 -->
        <div class="section-card">
          <div class="section-title">
            纳税人信息
          </div>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="公司全称">
                <a-input
                  v-model:value="form.companyFullName"
                  placeholder="请输入公司全称"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="税号">
                <a-input
                  v-model:value="form.taxId"
                  placeholder="请输入纳税人识别号"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="法定代表人">
                <a-input
                  v-model:value="form.legalPerson"
                  placeholder="请输入法定代表人"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="统一信用代码">
                <a-input
                  v-model:value="form.unifiedSocialCode"
                  placeholder="请输入统一社会信用代码"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="公司电话">
                <a-input
                  v-model:value="form.companyPhone"
                  placeholder="请输入公司电话"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="公司邮箱">
                <a-input
                  v-model:value="form.companyEmail"
                  placeholder="请输入公司邮箱"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="详细地址">
                <a-input
                  v-model:value="detailAddress"
                  placeholder="请输入详细地址"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="税率(%)">
                <a-input-number
                  v-model:value="form.taxRate"
                  :precision="2"
                  :min="0"
                  :max="100"
                  style="width:100%"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="开户银行">
                <a-input
                  v-model:value="bankName"
                  placeholder="请输入开户银行"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="银行账号">
                <a-input
                  v-model:value="bankAccount"
                  placeholder="请输入银行账号"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <!-- 期初信息 -->
        <div class="section-card">
          <div class="section-title">
            期初信息
          </div>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="期初应付金额">
                <a-input-number
                  v-model:value="openingBalance"
                  :precision="2"
                  :min="0"
                  style="width:100%"
                  size="small"
                  placeholder="0"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="期初预付金额">
                <a-input-number
                  v-model:value="prePaidAmount"
                  :precision="2"
                  :min="0"
                  style="width:100%"
                  size="small"
                  placeholder="0"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <!-- 其他信息 -->
        <div class="section-card">
          <div
            class="section-title"
            style="cursor:pointer"
            @click="otherInfoCollapsed = !otherInfoCollapsed"
          >
            其他信息
            <span style="margin-left:8px;font-weight:400;color:#bfbfbf;font-size:13px">
              {{ otherInfoCollapsed ? '点击展开' : '点击收起' }}
              <CaretDownOutlined
                v-if="!otherInfoCollapsed"
                style="margin-left:4px"
              />
              <CaretRightOutlined
                v-else
                style="margin-left:4px"
              />
            </span>
          </div>
          <a-row
            v-if="!otherInfoCollapsed"
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="付款期限">
                <a-radio-group
                  v-model:value="paymentTermType"
                  size="small"
                >
                  <a-radio value="DYNAMIC">
                    动态付款期限(天)
                  </a-radio>
                  <a-radio value="FIXED">
                    固定账期
                  </a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col
              v-if="paymentTermType === 'DYNAMIC'"
              :span="8"
            >
              <a-form-item label="天数">
                <a-input-number
                  v-model:value="paymentDays"
                  :min="0"
                  :max="365"
                  style="width:100%"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col
              v-if="paymentTermType === 'FIXED'"
              :span="8"
            >
              <a-form-item label="账期日">
                <a-select
                  v-model:value="fixedPaymentDay"
                  size="small"
                >
                  <a-select-option
                    v-for="d in 28"
                    :key="d"
                    :value="d"
                  >
                    {{ d }}号
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="结算期">
                <a-select
                  v-model:value="settlementDay"
                  size="small"
                >
                  <a-select-option
                    v-for="d in 28"
                    :key="d"
                    :value="d"
                  >
                    {{ d }}号
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="经营系列">
                <a-input
                  v-model:value="operatingSeries"
                  placeholder="请输入经营系列"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="经营面积(㎡)">
                <a-input-number
                  v-model:value="operatingArea"
                  :precision="2"
                  :min="0"
                  style="width:100%"
                  size="small"
                  placeholder="0"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row
            v-if="!otherInfoCollapsed"
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="单位网址">
                <a-input
                  v-model:value="companyWebsite"
                  placeholder="请输入网址"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="所属行业">
                <a-input
                  v-model:value="form.industry"
                  placeholder="请输入所属行业"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="来源渠道">
                <a-input
                  v-model:value="form.sourceChannel"
                  placeholder="如：招标/推荐"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row
            v-if="!otherInfoCollapsed"
            :gutter="24"
            class="section-row"
          >
            <a-col :span="24">
              <a-form-item label="备注">
                <a-textarea
                  v-model:value="form.remark"
                  placeholder="请输入备注"
                  :rows="2"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <!-- 证件信息 -->
        <div class="section-card">
          <div class="section-title">
            证件信息
          </div>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <div class="cert-upload">
                <div
                  class="cert-placeholder"
                  @click="certFileInput1.click()"
                >
                  <img
                    v-if="certLicenseUrl"
                    :src="certLicenseUrl"
                    class="cert-preview"
                  >
                  <div
                    v-else
                    class="cert-empty"
                  >
                    <PictureOutlined style="font-size:32px;color:#d9d9d9" /><span>点击上传图片</span>
                  </div>
                </div>
                <input
                  ref="certFileInput1"
                  type="file"
                  accept="image/*"
                  hidden
                  @change="(e) => handleCertUpload(e, 'license')"
                >
                <div class="cert-label">
                  营业执照
                </div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="cert-upload">
                <div
                  class="cert-placeholder"
                  @click="certFileInput2.click()"
                >
                  <img
                    v-if="certPermitUrl"
                    :src="certPermitUrl"
                    class="cert-preview"
                  >
                  <div
                    v-else
                    class="cert-empty"
                  >
                    <PictureOutlined style="font-size:32px;color:#d9d9d9" /><span>点击上传图片</span>
                  </div>
                </div>
                <input
                  ref="certFileInput2"
                  type="file"
                  accept="image/*"
                  hidden
                  @change="(e) => handleCertUpload(e, 'permit')"
                >
                <div class="cert-label">
                  生产许可证
                </div>
              </div>
            </a-col>
          </a-row>
        </div>

        <!-- 附件 -->
        <div class="section-card">
          <AttachmentUpload :partner-id="savedPartnerId" />
        </div>
      </a-form>
    </div>

    <!-- 固定底部按钮栏 -->
    <div class="form-footer">
      <a-space>
        <a-button @click="handleCancel">
          取消
        </a-button>
        <a-button
          :loading="saving"
          @click="handleSaveAndNew"
        >
          保存并新增
        </a-button>
        <a-button
          type="primary"
          :loading="saving"
          @click="handleSubmit"
        >
          保存并返回列表
        </a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { ArrowLeftOutlined, CaretDownOutlined, CaretRightOutlined, PictureOutlined, UserOutlined } from '@ant-design/icons-vue'
import { partnerApi, partnerCategoryApi, partnerGradeApi, partnerContactApi, partnerBankAccountApi, partnerRoleApi, type PartyContact } from '@/api/erp/partner'
import type { PartnerCategory, PartnerGrade } from '@/api/erp/partner'
import ContactList, { type ContactRowData } from '../components/ContactList.vue'
import AttachmentUpload from '../components/AttachmentUpload.vue'
import { userApi, type UserInfo } from '@/api/user'
import { warehouseApi, type WmsWarehouse } from '@/api/wms/warehouse'
import request from '@/utils/request'
import { generatePartnerCodeAsync } from '../utils/generateCode'

async function generateCode() { form.partnerCode = await generatePartnerCodeAsync('supplier') }

const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const statusChecked = ref(true)
const otherRoleOptions = ref([
  { value: 'CUSTOMER', label: '客户', checked: false },
  { value: 'LOGISTICS', label: '物流', checked: false },
  { value: 'OTHER', label: '其他', checked: false },
])
const enablePriceTrack = ref(false)
const hasCustomerRole = computed(() => otherRoleOptions.value.some(o => o.value === 'CUSTOMER' && o.checked))
const showMallAccount = hasCustomerRole
const openingBalance = ref(0)
const prePaidAmount = ref(0)
const bankName = ref('')
const bankAccount = ref('')
const paymentTermType = ref('DYNAMIC')
const paymentDays = ref(30)
const fixedPaymentDay = ref(1)
const settlementDay = ref(1)
const operatingSeries = ref('')
const operatingArea = ref(0)
const otherInfoCollapsed = ref(true)
const categories = ref<PartnerCategory[]>([])
const grades = ref<PartnerGrade[]>([])
const users = ref<UserInfo[]>([])
const warehouses = ref<WmsWarehouse[]>([])
const contacts = ref<ContactRowData[]>([])
const savedPartnerId = ref<number>()
const avatarUrl = ref('')
const avatarFileInput = ref<HTMLInputElement>()
const certLicenseUrl = ref('')
const certPermitUrl = ref('')
const certFileInput1 = ref<HTMLInputElement>()
const certFileInput2 = ref<HTMLInputElement>()
const detailAddress = ref('')
const companyWebsite = ref('')

const form = reactive({
  partnerName: '', partnerShortName: '', partnerCode: '',
  partnerCategoryId: undefined as number | undefined,
  partnerGradeId: undefined as number | undefined,
  defaultHandlerId: undefined as number | undefined,
  defaultWarehouseId: undefined as number | undefined,
  province: '', city: '', district: '',
  companyFullName: '',
  unifiedSocialCode: '', taxId: '', legalPerson: '',
  companyPhone: '', companyEmail: '',
  industry: '', sourceChannel: '',
  settleType: 'MONTHLY', taxRate: 13, remark: '',
})

const formRules: Record<string, any> = { partnerName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }] }

async function loadCategories() { try { categories.value = await partnerCategoryApi.getTree('SUPPLIER') } catch { categories.value = [] } }
async function loadGrades() { try { grades.value = await partnerGradeApi.list('SUPPLIER') } catch { grades.value = [] } }
async function loadUsers() { try { const res = await userApi.getList(); users.value = (res as any)?.data || (res as any) || [] } catch { users.value = [] } }
async function loadWarehouses() { try { const res = await warehouseApi.listAll(); warehouses.value = (res as any)?.data || (res as any) || [] } catch { warehouses.value = [] } }
function filterUser(input: string, option: any) { return (option.label || '').toLowerCase().includes(input.toLowerCase()) }
function handleCancel() { router.push('/md/supplier/index') }

async function handleAvatarUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/file/upload', formData)
    avatarUrl.value = typeof res === 'string' ? res : (res as any)?.url || (res as any)?.data?.url
    message.success('上传成功')
  } catch { message.error('上传失败') }
  input.value = ''
}

async function handleCertUpload(e: Event, type: 'license' | 'permit') {
  const input = e.target as HTMLInputElement; const file = input.files?.[0]; if (!file) return
  try {
    const formData = new FormData(); formData.append('file', file)
    const res = await request.post('/file/upload', formData)
    const url = typeof res === 'string' ? res : (res as any)?.url || (res as any)?.data?.url
    if (type === 'license') certLicenseUrl.value = url; else certPermitUrl.value = url
    message.success('上传成功')
  } catch { message.error('上传失败') }
  input.value = ''
}

async function doSubmit(): Promise<number | null> {
  try { await formRef.value?.validate() } catch { return null }
  const roles = ['SUPPLIER', ...otherRoleOptions.value.filter(o => o.checked).map(o => o.value)]
  try {
    const result = await partnerApi.create({
      ...form, partnerType: 'SUPPLIER', status: statusChecked.value ? 'ENABLED' : 'DISABLED',
      roles, openingBalance: openingBalance.value, companyWebsite: companyWebsite.value,
      detailAddress: detailAddress.value, partnerAvatar: avatarUrl.value,
      paymentTerms: paymentTermType.value === 'DYNAMIC' ? `${paymentDays.value}天` : `每月${fixedPaymentDay.value}号`,
      operatingSeries: operatingSeries.value, operatingArea: operatingArea.value,
    } as any)
    return (result as any)?.id || 0
  } catch (err: any) { message.error(err?.response?.data?.message || err?.message || '创建失败'); return null }
}

async function submitContacts(partnerId: number) {
  const ordered = [...contacts.value].sort((a, b) => (b.isDefault || 0) - (a.isDefault || 0))
  for (const c of ordered) {
    const data = {
      ...c,
      partyId: partnerId,
      isPrimary: c.isDefault || 0,
      phone: c.contactPhone || c.phone || '',
      mobile: c.contactPhone || c.mobile || '',
      email: c.contactEmail || c.email || '',
      contactName: c.contactName || ''
    }
    delete (data as any).id
    delete (data as any).partnerId
    delete (data as any).isDefault
    delete (data as any).contactPhone
    delete (data as any).contactEmail
    await partnerContactApi.create(data)
  }
}

async function submitRoles(partnerId: number) {
  const allRoles = ['SUPPLIER', ...otherRoleOptions.value.filter(o => o.checked).map(o => o.value)]
  for (const roleType of allRoles) {
    try { await partnerRoleApi.addRole(partnerId, roleType, roleType === 'SUPPLIER') } catch {}
  }
}

async function submitBankAccount(partnerId: number) {
  if (!bankName.value && !bankAccount.value) return
  try { await partnerBankAccountApi.create({ partnerId, accountName: form.partnerName, bankName: bankName.value, accountNo: bankAccount.value, isDefault: 1 }) } catch {}
}

async function handleSubmit() {
  saving.value = true
  try {
    const pid = await doSubmit(); if (!pid) return
    await submitRoles(pid); await submitContacts(pid); await submitBankAccount(pid); savedPartnerId.value = pid
    message.success('供应商创建成功'); router.push('/md/supplier/index')
  } finally { saving.value = false }
}

async function handleSaveAndNew() {
  saving.value = true
  try {
    const pid = await doSubmit(); if (!pid) return
    await submitRoles(pid); await submitContacts(pid); await submitBankAccount(pid); savedPartnerId.value = pid
    message.success('供应商创建成功')
    Object.assign(form, { partnerName:'', partnerShortName:'', partnerCode:'', partnerCategoryId:undefined, partnerGradeId:undefined, defaultHandlerId:undefined, defaultWarehouseId:undefined, province:'', city:'', district:'', companyFullName:'', unifiedSocialCode:'', taxId:'', legalPerson:'', companyPhone:'', companyEmail:'', industry:'', sourceChannel:'', settleType:'MONTHLY', taxRate:13, remark:'' })
    contacts.value = []; bankName.value = ''; bankAccount.value = ''; detailAddress.value = ''
    openingBalance.value = 0; prePaidAmount.value = 0; companyWebsite.value = ''
    avatarUrl.value = ''
    operatingSeries.value = ''; operatingArea.value = 0
    certLicenseUrl.value = ''; certPermitUrl.value = ''
    await generateCode()
  } finally { saving.value = false }
}

function handleKeydown(e: KeyboardEvent) { if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSubmit() } }
onMounted(() => { loadCategories(); loadGrades(); loadUsers(); loadWarehouses(); document.addEventListener('keydown', handleKeydown) })
onUnmounted(() => { document.removeEventListener('keydown', handleKeydown) })
</script>

<style scoped>
.form-page-wrapper { display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.page-header { padding: 12px 16px; flex-shrink: 0; }
.page-title { font-size: 16px; font-weight: 600; color: #262626; }
.top-options { background: #fff; padding: 12px 20px; margin: 0 16px 8px; border-radius: 6px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); flex-shrink: 0; }
.form-scroll-area { flex: 1; overflow-y: auto; padding: 0 16px 8px; min-height: 0; }
.section-card { background: #fff; border-radius: 6px; padding: 20px 24px 12px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }
.section-title { font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 16px; padding-bottom: 10px; border-bottom: 1px solid #f0f0f0; }
.section-row :deep(.ant-form-item) { margin-bottom: 10px; }
.section-row :deep(.ant-form-item-label > label) { font-size: 13px; color: #595959; }
.form-footer { background: #fff; border-radius: 6px; padding: 16px 24px; text-align: right; box-shadow: 0 -1px 4px rgba(0,0,0,0.05); flex-shrink: 0; margin: 0 16px 16px; }
.cert-upload { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.cert-placeholder { width: 120px; height: 120px; border: 1px dashed #d9d9d9; border-radius: 6px; display: flex; align-items: center; justify-content: center; cursor: pointer; background: #fafafa; overflow: hidden; transition: border-color 0.2s; }
.cert-placeholder:hover { border-color: #40a9ff; }
.cert-preview { width: 100%; height: 100%; object-fit: cover; }
.cert-empty { display: flex; flex-direction: column; align-items: center; gap: 4px; color: #bfbfbf; font-size: 12px; }
.cert-label { font-size: 13px; color: #595959; }

/* 头像上传 */
.avatar-upload { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.avatar-placeholder {
  width: 100px; height: 100px; border: 1px dashed #d9d9d9; border-radius: 6px;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  background: #fafafa; overflow: hidden; transition: border-color 0.2s;
}
.avatar-placeholder:hover { border-color: #40a9ff; }
.avatar-preview { width: 100%; height: 100%; object-fit: cover; }
.avatar-empty { display: flex; align-items: center; justify-content: center; }
</style>
