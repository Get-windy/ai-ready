<template>
  <ErrorBoundary>
    <PageContainer
      title="企业信息"
      full-height
    >
      <div class="company-info-page">
        <!-- 内容滚动区：Tab 栏吸顶，表单在卡片内滚动 -->
        <div class="form-scroll-area">
          <a-spin :spinning="loading">
            <!--
              档案（主数据）表单：分区卡片（FormSection）+ 两列栅格 + 行内校验。
              不用 BillFormPage 的 inline flow —— 那是**单据**表头快速录单用的单行内联布局，
              企业档案字段多、语义可分组、编辑频率低，按《表单布局选型：单据 vs 档案》走档案形态。
            -->
            <a-form
              ref="formRef"
              :model="form"
              :rules="rules"
              layout="horizontal"
              :label-col="{ span: 7 }"
              :wrapper-col="{ span: 17 }"
              :colon="false"
              size="small"
            >
              <a-tabs
                v-model:activeKey="activeTab"
                class="company-tabs"
              >
                <!-- ═══ Tab① 企业信息 ═══ -->
                <a-tab-pane
                  key="company"
                  tab="企业信息"
                >
                  <FormSection
                    title="企业标识"
                    tip="企业 LOGO 建议 225×225 像素、2MB 以内；上传后需点「保存」才落库"
                  >
                    <a-row :gutter="24">
                      <a-col :span="24">
                        <a-form-item label="企业 LOGO">
                          <div class="logo-row">
                            <!--
                              LOGO 走平台通用上传契约 `POST /api/file/upload`（FileUploadController），
                              返回 url 后先只存在表单里，点「保存」才随档案落库
                              （与 CertUploadList / 银行账户收款码同一口径：先上传拿 url、后保存落库）。
                            -->
                            <a-upload
                              :show-upload-list="false"
                              :before-upload="beforeLogoUpload"
                              accept="image/*"
                            >
                              <a-button
                                size="small"
                                :loading="logoUploading"
                              >
                                <template #icon>
                                  <UploadOutlined />
                                </template>
                                {{ form.logoUrl ? '重新上传' : '上传 LOGO' }}
                              </a-button>
                            </a-upload>
                            <a-button
                              v-if="form.logoUrl"
                              size="small"
                              danger
                              @click="form.logoUrl = ''"
                            >
                              移除
                            </a-button>
                            <img
                              v-if="form.logoUrl"
                              :src="form.logoUrl"
                              class="logo-preview"
                              alt="企业 LOGO"
                            >
                            <span
                              v-else
                              class="logo-empty"
                            >未上传</span>
                          </div>
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </FormSection>

                  <FormSection
                    title="基本信息"
                    tip="企业名称为必填；统一社会信用代码为 18 位大写字母或数字"
                  >
                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item
                          label="企业名称"
                          name="tenantName"
                        >
                          <a-input
                            v-model:value="form.tenantName"
                            placeholder="请输入企业名称"
                            :maxlength="50"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          label="统一社会信用代码"
                          name="creditCode"
                        >
                          <a-input
                            v-model:value="form.creditCode"
                            placeholder="请输入 18 位统一社会信用代码"
                            :maxlength="32"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item
                          label="企业类型"
                          name="companyType"
                        >
                          <a-select
                            v-model:value="form.companyType"
                            placeholder="请选择企业类型"
                            allow-clear
                          >
                            <a-select-option
                              v-for="item in COMPANY_TYPE_OPTIONS"
                              :key="item"
                              :value="item"
                            >
                              {{ item }}
                            </a-select-option>
                          </a-select>
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          label="法定代表人"
                          name="legalPerson"
                        >
                          <a-input
                            v-model:value="form.legalPerson"
                            placeholder="请输入法定代表人姓名"
                            :maxlength="64"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item
                          label="注册资本（万元）"
                          name="registeredCapital"
                        >
                          <a-input-number
                            v-model:value="form.registeredCapital"
                            style="width: 100%"
                            placeholder="请输入注册资本"
                            :min="0"
                            :precision="2"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          label="成立日期"
                          name="establishDate"
                        >
                          <a-date-picker
                            v-model:value="form.establishDate"
                            style="width: 100%"
                            placeholder="请选择成立日期"
                            value-format="YYYY-MM-DD"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item
                          label="所属行业"
                          name="industry"
                        >
                          <a-select
                            v-model:value="form.industry"
                            placeholder="请选择所属行业"
                            allow-clear
                          >
                            <a-select-option
                              v-for="item in INDUSTRY_OPTIONS"
                              :key="item"
                              :value="item"
                            >
                              {{ item }}
                            </a-select-option>
                          </a-select>
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          label="企业规模"
                          name="companyScale"
                        >
                          <a-select
                            v-model:value="form.companyScale"
                            placeholder="请选择企业规模"
                            allow-clear
                          >
                            <a-select-option
                              v-for="item in COMPANY_SCALE_OPTIONS"
                              :key="item"
                              :value="item"
                            >
                              {{ item }}
                            </a-select-option>
                          </a-select>
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="24">
                      <a-col :span="24">
                        <a-form-item
                          label="经营范围"
                          name="businessScope"
                        >
                          <a-textarea
                            v-model:value="form.businessScope"
                            :rows="3"
                            :maxlength="2000"
                            placeholder="请输入经营范围"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </FormSection>

                  <FormSection
                    title="联系信息"
                    tip="用于业务单据、对账与系统通知的联系方式"
                  >
                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item
                          label="联系人"
                          name="contactPerson"
                        >
                          <a-input
                            v-model:value="form.contactPerson"
                            placeholder="请输入联系人"
                            :maxlength="32"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          label="联系电话"
                          name="contactPhone"
                        >
                          <a-input
                            v-model:value="form.contactPhone"
                            placeholder="请输入企业联系电话"
                            :maxlength="32"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item
                          label="企业邮箱"
                          name="contactEmail"
                        >
                          <a-input
                            v-model:value="form.contactEmail"
                            placeholder="请输入企业邮箱"
                            :maxlength="128"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          label="企业地址"
                          name="address"
                        >
                          <a-input
                            v-model:value="form.address"
                            placeholder="请输入企业地址"
                            :maxlength="255"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item
                          label="联系人电话"
                          name="contactPersonPhone"
                        >
                          <a-input
                            v-model:value="form.contactPersonPhone"
                            placeholder="请输入联系人电话"
                            :maxlength="32"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          label="联系人邮箱"
                          name="contactPersonEmail"
                        >
                          <a-input
                            v-model:value="form.contactPersonEmail"
                            placeholder="请输入联系人邮箱"
                            :maxlength="128"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </FormSection>

                  <FormSection
                    title="系统信息"
                    tip="由平台维护，仅供查看，不可编辑"
                  >
                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item label="当前账套 ID">
                          <a-input
                            :value="info.id || '—'"
                            disabled
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item label="租户编码">
                          <a-input
                            :value="info.tenantCode || '—'"
                            disabled
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item label="注册时间">
                          <a-input
                            :value="info.createTime || '—'"
                            disabled
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item label="到期时间">
                          <a-input
                            :value="info.expireTime || '未设置'"
                            disabled
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item label="租户状态">
                          <a-input
                            :value="statusText"
                            disabled
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item label="租户等级">
                          <a-input
                            :value="levelText"
                            disabled
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </FormSection>
                </a-tab-pane>

                <!-- ═══ Tab② 纳税人信息 ═══ -->
                <a-tab-pane
                  key="taxpayer"
                  tab="纳税人信息"
                >
                  <FormSection
                    title="纳税人信息"
                    tip="开票资料；公司名称与企业信息 Tab 为同一字段"
                  >
                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item label="公司名称">
                          <!-- 与 Tab① 的「企业名称」共用 tenant_name，此处只读，避免两处同时改同一列 -->
                          <a-input
                            :value="form.tenantName"
                            disabled
                            placeholder="—"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          label="纳税人识别号"
                          name="taxNumber"
                        >
                          <a-input
                            v-model:value="form.taxNumber"
                            placeholder="请输入纳税人识别号（20 字以内）"
                            :maxlength="20"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item
                          label="地址"
                          name="taxpayerAddress"
                        >
                          <a-input
                            v-model:value="form.taxpayerAddress"
                            placeholder="请输入开票地址（50 字以内）"
                            :maxlength="50"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          label="电话"
                          name="taxpayerPhone"
                        >
                          <a-input
                            v-model:value="form.taxpayerPhone"
                            placeholder="请输入开票电话（20 字以内）"
                            :maxlength="20"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="24">
                      <a-col :span="12">
                        <a-form-item
                          label="开户行地址"
                          name="bankName"
                        >
                          <a-input
                            v-model:value="form.bankName"
                            placeholder="请输入开户行地址（50 字以内）"
                            :maxlength="50"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          label="开户行账号"
                          name="bankAccount"
                        >
                          <a-input
                            v-model:value="form.bankAccount"
                            placeholder="请输入开户行账号（50 字以内）"
                            :maxlength="50"
                            allow-clear
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </FormSection>
                </a-tab-pane>
              </a-tabs>
            </a-form>
          </a-spin>
        </div>

        <!-- 底部操作区：两个 Tab 共用一个保存动作（对标 ql361） -->
        <div class="form-footer">
          <span class="footer-tip">带 <em>*</em> 为必填项；保存后自动重新读取服务端数据</span>
          <a-space>
            <a-tooltip
              placement="bottom"
              title="放弃未保存的修改，恢复到最近一次加载的值"
            >
              <a-button @click="handleReset">
                重置
              </a-button>
            </a-tooltip>
            <a-button
              v-permission="'set:company-info:save'"
              type="primary"
              :loading="saving"
              @click="handleSave"
            >
              <template #icon>
                <SaveOutlined />
              </template>
              保存
            </a-button>
          </a-space>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SaveOutlined, UploadOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import request from '@/utils/request'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FormSection from '@/components/FormSection/index.vue'
import { emailRule, requiredRule } from '@/utils/formRules'
import { companyProfileApi, type CompanyProfile } from '@/api/tenant'

defineOptions({ name: 'SetCompanyInfo' })

/** 企业类型选项（与历史页面逐字一致，值即字面量） */
const COMPANY_TYPE_OPTIONS = ['有限责任公司', '股份有限公司', '合伙企业', '个体工商户']

/** 所属行业选项 */
const INDUSTRY_OPTIONS = ['IT/互联网', '制造业', '批发零售', '物流运输', '金融', '其他']

/** 企业规模选项 */
const COMPANY_SCALE_OPTIONS = ['小型（<50人）', '中型（50-500人）', '大型（>500人）']

/** 租户等级展示映射（lookup 一律用 Map，避免普通对象命中原型链键） */
const LEVEL_MAP = new Map<string, string>([
  ['basic', '基础版'],
  ['professional', '专业版'],
  ['enterprise', '企业版']
])

const activeTab = ref('company')
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

/** 可编辑字段（= 后端企业档案列 + 纳税人信息列 + LOGO） */
const form = reactive({
  tenantName: '',
  creditCode: '',
  companyType: undefined as string | undefined,
  legalPerson: '',
  registeredCapital: undefined as number | undefined,
  industry: undefined as string | undefined,
  companyScale: undefined as string | undefined,
  establishDate: undefined as string | undefined,
  businessScope: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  contactPersonPhone: '',
  contactPersonEmail: '',
  taxNumber: '',
  taxpayerAddress: '',
  taxpayerPhone: '',
  bankName: '',
  bankAccount: '',
  /** 企业 LOGO 地址（上传端点返回的 url；仅保存后落库） */
  logoUrl: ''
})

/** 只读展示字段（平台维护，不参与提交） */
const info = reactive({
  id: '',
  tenantCode: '',
  createTime: '',
  expireTime: '',
  status: undefined as number | undefined,
  level: undefined as string | undefined
})

const statusText = computed(() => {
  if (info.status === 0) return '正常'
  if (info.status === 1) return '停用'
  return '—'
})

const levelText = computed(() => (info.level ? LEVEL_MAP.get(info.level) || info.level : '—'))

/**
 * 行内校验规则。
 * 说明：电话**不**复用 `@/utils/formRules` 的 `phoneRule` —— 那是 `1[3-9]\d{9}` 手机号专用规则，
 * 企业座机（如 0931-1234567）会被误判为非法；这里用「数字 + 常见分隔符」的宽松口径。
 */
const rules: Record<string, Rule[]> = {
  tenantName: [
    requiredRule('企业名称'),
    { max: 50, message: '企业名称最长 50 个字符', trigger: 'blur' }
  ],
  creditCode: [
    { pattern: /^[0-9A-Z]{18}$/, message: '统一社会信用代码应为 18 位大写字母或数字', trigger: 'blur' }
  ],
  contactPhone: [
    { pattern: /^[\d\s()+-]{6,32}$/, message: '请输入有效的联系电话', trigger: 'blur' }
  ],
  contactEmail: [emailRule],
  contactPersonPhone: [
    { pattern: /^[\d\s()+-]{6,32}$/, message: '请输入有效的联系人电话', trigger: 'blur' }
  ],
  contactPersonEmail: [emailRule],
  taxpayerPhone: [
    { pattern: /^[\d\s()+-]{6,32}$/, message: '请输入有效的电话', trigger: 'blur' }
  ]
}

/** 统一日期展示：兼容 ISO（2026-09-18T10:00:00）与空格分隔两种后端格式 */
function formatDateTime(value?: string | null): string {
  if (!value) return ''
  return String(value).replace('T', ' ').split('.')[0]
}

// ── 企业 LOGO 上传 ────────────────────────────────────────────────────────────
/** 上传中标记（a-button loading） */
const logoUploading = ref(false)

/** LOGO 大小上限 2MB（对标 ql361「建议 225×225 < 2M」，见开发文档 §4-★3） */
const LOGO_MAX_SIZE = 2 * 1024 * 1024

/**
 * 上传 LOGO：复用平台通用上传契约 `POST /api/file/upload`（认证后可用，返回 `{ url }`）。
 * 只把返回的 url 存进表单，**不在这里落库** —— 用户还需点「保存」才会写进企业档案。
 * 一律返回 false 阻止 a-upload 自身的上传行为（否则会重复提交到 antd 默认地址）。
 */
async function beforeLogoUpload(file: File) {
  if (!file.type.startsWith('image/')) {
    message.warning('企业 LOGO 仅支持图片文件')
    return false
  }
  if (file.size > LOGO_MAX_SIZE) {
    message.warning('企业 LOGO 图片不能超过 2MB')
    return false
  }
  logoUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res: any = await request.post('/file/upload', formData)
    // 响应拦截器可能已拆包（直接给 data），两种形态都兼容
    const url = typeof res === 'string' ? res : (res?.url || res?.data?.url)
    if (!url) {
      message.error('企业 LOGO 上传失败')
      return false
    }
    form.logoUrl = url
    message.success('上传成功，点击「保存」后生效')
  } catch (error: any) {
    message.error(error?.message || '企业 LOGO 上传失败')
  } finally {
    logoUploading.value = false
  }
  return false
}

/** 服务端值 → 表单（空值一律归一为空串 / undefined，避免 a-input 显示 "null"） */
function applyProfile(data: CompanyProfile) {
  Object.assign(form, {
    tenantName: data.tenantName ?? '',
    creditCode: data.creditCode ?? '',
    companyType: data.companyType || undefined,
    legalPerson: data.legalPerson ?? '',
    registeredCapital: data.registeredCapital ?? undefined,
    industry: data.industry || undefined,
    companyScale: data.companyScale || undefined,
    establishDate: data.establishDate || undefined,
    businessScope: data.businessScope ?? '',
    contactPerson: data.contactPerson ?? '',
    contactPhone: data.contactPhone ?? '',
    contactEmail: data.contactEmail ?? '',
    address: data.address ?? '',
    contactPersonPhone: data.contactPersonPhone ?? '',
    contactPersonEmail: data.contactPersonEmail ?? '',
    taxNumber: data.taxNumber ?? '',
    taxpayerAddress: data.taxpayerAddress ?? '',
    taxpayerPhone: data.taxpayerPhone ?? '',
    bankName: data.bankName ?? '',
    bankAccount: data.bankAccount ?? '',
    logoUrl: data.logoUrl ?? ''
  })
  Object.assign(info, {
    // 雪花 ID 是 BIGINT：只做字符串展示，禁止 Number(id)
    id: data.id != null ? String(data.id) : '',
    tenantCode: data.tenantCode ?? '',
    createTime: formatDateTime(data.createTime),
    expireTime: formatDateTime(data.expireTime),
    status: data.status,
    level: data.level
  })
}

/** 加载当前租户的企业档案（会话租户，路径不带 id） */
async function loadProfile() {
  loading.value = true
  try {
    // 响应拦截器已把 Result 拆包，运行时拿到的就是企业档案本体（故此处转成 CompanyProfile）
    const data = (await companyProfileApi.getCurrent()) as unknown as CompanyProfile
    if (data) {
      applyProfile(data)
    }
    // 回读后清掉校验态，避免上一轮的标红残留
    formRef.value?.clearValidate()
  } catch (error: any) {
    // 拉取失败必须提示：历史实现是静默展示空表单，用户分不清「没数据」与「加载失败」
    message.error(error?.response?.data?.message || error?.message || '企业信息加载失败')
  } finally {
    loading.value = false
  }
}

/** 表单 → 提交体（只提交企业档案字段，平台列由后端忽略） */
function buildPayload(): Partial<CompanyProfile> {
  return {
    tenantName: form.tenantName.trim(),
    creditCode: form.creditCode.trim(),
    companyType: form.companyType,
    legalPerson: form.legalPerson.trim(),
    registeredCapital: form.registeredCapital,
    industry: form.industry,
    companyScale: form.companyScale,
    establishDate: form.establishDate,
    businessScope: form.businessScope.trim(),
    contactPerson: form.contactPerson.trim(),
    contactPhone: form.contactPhone.trim(),
    contactEmail: form.contactEmail.trim(),
    address: form.address.trim(),
    contactPersonPhone: form.contactPersonPhone.trim(),
    contactPersonEmail: form.contactPersonEmail.trim(),
    taxNumber: form.taxNumber.trim(),
    taxpayerAddress: form.taxpayerAddress.trim(),
    taxpayerPhone: form.taxpayerPhone.trim(),
    bankName: form.bankName.trim(),
    bankAccount: form.bankAccount.trim(),
    // 传空串即清空 LOGO（后端把空白归一为 NULL）
    logoUrl: form.logoUrl.trim()
  }
}

/** 保存：校验 → PUT /tenant/current → 重新 GET 回读（保证页面显示的就是库里的值） */
async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    message.warning('请先修正表单中标红的字段')
    return
  }

  saving.value = true
  try {
    await companyProfileApi.save(buildPayload())
    message.success('保存成功')
    await loadProfile()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

/** 重置：丢弃未保存的修改，重新拉取服务端值（不再沿用「重置=置空」的历史语义） */
function handleReset() {
  loadProfile()
}

onMounted(loadProfile)
</script>

<style scoped>
.company-info-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  background: #f5f5f5;
}

.form-scroll-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 12px 12px;
}

/* Tab 栏吸顶：滚动浏览长表单时两个 Tab 始终可见 */
.company-tabs :deep(.ant-tabs-nav) {
  position: sticky;
  top: 0;
  z-index: 10;
  margin-bottom: 8px;
  padding: 0 12px;
  background: #fff;
  border-radius: 6px;
}

.form-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.footer-tip {
  font-size: 12px;
  color: #8c8c8c;
}

.footer-tip em {
  color: #ff4d4f;
  font-style: normal;
}

/* 企业 LOGO：按钮 + 预览同一行，预览框固定 56×56（列表/详情处显示比例一致） */
.logo-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-preview {
  width: 56px;
  height: 56px;
  object-fit: contain;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  background: #fafafa;
}

.logo-empty {
  font-size: 12px;
  color: #bfbfbf;
}
</style>
