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
        <span class="page-title">{{ isEdit ? '客户编辑' : '客户新增' }}</span>
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
        v-model:checked="enablePriceTrack"
        style="margin-left:24px"
      >
        启用销售价格跟踪
      </a-checkbox>
    </div>

    <!-- 可滚动内容区 -->
    <div class="form-scroll-area">
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        layout="horizontal"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 18 }"
      >
        <!-- 基本信息 -->
        <FormSection title="基本信息">
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="20">
              <a-row
                :gutter="24"
                class="basic-info-row"
              >
                <a-col :span="6">
                  <a-form-item
                    :label-col="{ flex: '78px' }"
                    label="客户名称"
                    name="partnerName"
                  >
                    <a-input
                      v-model:value="form.partnerName"
                      placeholder="请输入客户名称"
                      size="small"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="6">
                  <a-form-item
                    :label-col="{ flex: '78px' }"
                    label="客户编号"
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
                <a-col :span="6">
                  <a-form-item
                    :label-col="{ flex: '78px' }"
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
                <a-col :span="6">
                  <a-form-item
                    :label-col="{ flex: '78px' }"
                    label="客户级别"
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
                <a-col :span="6">
                  <a-form-item
                    :label-col="{ flex: '78px' }"
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
                      @change="onDefaultHandlerChange"
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
                <a-col :span="6">
                  <a-form-item
                    :label-col="{ flex: '78px' }"
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
                <!-- 对标：所属区域在基本信息区（与所属仓库同排），带放大镜下拉 -->
                <a-col :span="6">
                  <a-form-item
                    label="所属区域"
                    :label-col="{ flex: '78px' }"
                  >
                    <a-select
                      v-model:value="region"
                      placeholder="请选择所属区域"
                      allow-clear
                      show-search
                      option-filter-prop="label"
                      size="small"
                      :options="regionOptions"
                    />
                  </a-form-item>
                </a-col>
              <!-- 省/市/区已移到「联系人信息 → 所在地区」三级联动，单位所在地区默认取常用联系人（对标无此三项，避免冗余） -->
                <a-col :span="6">
                  <a-form-item
                    label="助记码"
                    :label-col="{ flex: '78px' }"
                  >
                    <a-input
                      v-model:value="form.partnerShortName"
                      placeholder="输入拼音首字母等"
                      size="small"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="6">
                  <a-form-item
                    :label-col="{ flex: '78px' }"
                    label="结款方式"
                    name="settleType"
                  >
                    <a-select
                      v-model:value="form.settleType"
                      size="small"
                    >
                      <a-select-option value="挂账">
                        挂账
                      </a-select-option>
                      <a-select-option value="现结">
                        现结
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="6">
                  <a-form-item
                    label="状态"
                    :label-col="{ flex: '78px' }"
                  >
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
              :span="4"
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
        </FormSection>

        <!-- 联系人信息（对标：位于基本信息之后、会员信息之前） -->
        <FormSection>
          <ContactList
            :contacts="contacts"
            show-mall-account
            @update="contacts = $event"
          />
        </FormSection>

        <!-- 会员信息 -->
        <FormSection title="会员信息">
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="会员卡号">
                <a-input
                  v-model:value="memberCardNo"
                  placeholder="请输入会员卡号"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="会员名称">
                <a-input
                  v-model:value="memberName"
                  placeholder="请输入会员名称"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="初始积分">
                <a-input-number
                  v-model:value="initialPoints"
                  :min="0"
                  style="width:100%"
                  size="small"
                  placeholder="0"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="会员级别">
                <a-select
                  v-model:value="memberLevel"
                  placeholder="请选择会员级别"
                  size="small"
                  allow-clear
                  :options="grades.map(g => ({ label: g.gradeName, value: g.gradeName }))"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="会员卡状态">
                <a-select
                  v-model:value="memberCardStatus"
                  placeholder="请选择会员卡状态"
                  size="small"
                  allow-clear
                  :options="memberCardStatusOptions"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="会员卡有效期">
                <a-range-picker
                  v-model:value="memberValidRange"
                  size="small"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="累计消费额">
                <a-input-number
                  v-model:value="memberTotalConsume"
                  :precision="2"
                  :min="0"
                  style="width:100%"
                  size="small"
                  placeholder="0"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="发卡时间">
                <a-date-picker
                  v-model:value="memberIssueTime"
                  size="small"
                  show-time
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="当前积分">
                <a-input-number
                  v-model:value="currentPoints"
                  :min="0"
                  style="width:100%"
                  size="small"
                  placeholder="0"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>

        <!-- 纳税人信息 -->
        <FormSection title="纳税人信息">
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
              <a-form-item
                label="税号"
                name="taxId"
              >
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
        </FormSection>

        <!-- 期初信息 -->
        <FormSection title="期初信息">
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="期初应收金额">
                <a-input-number
                  v-model:value="openingBalance"
                  :precision="2"
                  :min="0"
                  style="width: 100%"
                  size="small"
                  placeholder="0"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="期初预收金额">
                <a-input-number
                  v-model:value="preReceivedAmount"
                  :precision="2"
                  :min="0"
                  style="width: 100%"
                  size="small"
                  placeholder="0"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>

        <!-- 其他信息 -->
        <FormSection
          title="其他信息"
          collapsible
          v-model:collapsed="otherInfoCollapsed"
        >
          
          <a-row
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
                  placeholder="如：线上推广/转介绍"
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
              <a-form-item label="信用额度">
                <a-input-number
                  v-model:value="form.creditLimit"
                  :precision="2"
                  :min="0"
                  style="width: 100%"
                  size="small"
                  placeholder="0"
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
          <!-- 账期（对标：结款方式=挂账时配套 动态收款期限/固定账期/结算期） -->
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="动态收款期限（天）">
                <a-input-number
                  v-model:value="creditDays"
                  :min="0"
                  style="width:100%"
                  size="small"
                  placeholder="0"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="固定账期">
                <a-input-number
                  v-model:value="fixedCreditDay"
                  :min="0"
                  :max="31"
                  style="width:100%"
                  size="small"
                  placeholder="号"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="结算期">
                <a-input-number
                  v-model:value="statementDay"
                  :min="0"
                  :max="31"
                  style="width:100%"
                  size="small"
                  placeholder="号"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="推广人">
                <a-input
                  v-model:value="promoterName"
                  placeholder="请输入推广人"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="客户来源">
                <a-select
                  v-model:value="customerSource"
                  placeholder="请选择客户来源"
                  size="small"
                  allow-clear
                  :options="customerSourceOptions"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row
            :gutter="24"
            class="section-row"
          >
            <a-col :span="8">
              <a-form-item label="买家账号">
                <a-input
                  v-model:value="buyerAccount"
                  placeholder="商城买家账号（开通商城账号后可用）"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="客户一票通">
                <a-input
                  v-model:value="customerOnePass"
                  placeholder="请输入客户一票通"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="营业执照有效期">
                <a-date-picker
                  v-model:value="businessLicenseExpiry"
                  size="small"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row
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
        </FormSection>

        <!-- 证件信息（通用组件：营业执照固定，其余类型可选/自定义，始终保留一个待输入位） -->
        <FormSection>
          <CertUploadList
            ref="certRef"
            :partner-id="savedPartnerId || loadedPartnerId"
          />
        </FormSection>

        <!-- 附件 -->
        <FormSection>
          <AttachmentUpload :partner-id="savedPartnerId || loadedPartnerId" />
        </FormSection>
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
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { ArrowLeftOutlined, PictureOutlined, UserOutlined } from '@ant-design/icons-vue'
import { partnerApi, partnerCategoryApi, partnerGradeApi, partnerContactApi, partnerBankAccountApi, partnerRoleApi, customerRegionApi, type PartyContact } from '@/api/erp/partner'
import type { PartnerCategory, PartnerGrade } from '@/api/erp/partner'
import ContactList, { type ContactRowData } from '../components/ContactList.vue'
import AttachmentUpload from '../components/AttachmentUpload.vue'
import CertUploadList from '@/components/CertUploadList/CertUploadList.vue'
import FormSection from '@/components/FormSection/index.vue'
import { userApi, type UserInfo } from '@/api/user'
import { warehouseApi, type WmsWarehouse } from '@/api/wms/warehouse'
import request from '@/utils/request'
import { generatePartnerCodeAsync } from '../utils/generateCode'

async function generateCode() { form.partnerCode = await generatePartnerCodeAsync('customer') }

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()

/** 编辑模式：?id= 命中时为编辑，否则为新增（对标双入口：列表/新增同页） */
const editingId = computed<number>(() => Number(route.query.id || (route.params as any).id || 0))
const isEdit = computed(() => editingId.value > 0)
const saving = ref(false)
const statusChecked = ref(true)
const enablePriceTrack = ref(true)
const otherRoleOptions = ref([
  { value: 'SUPPLIER', label: '供应商', checked: false },
  { value: 'LOGISTICS', label: '物流', checked: false },
  { value: 'OTHER', label: '其他', checked: false },
])
const openingBalance = ref(0)
const preReceivedAmount = ref(0)
const companyWebsite = ref('')
const detailAddress = ref('')
const bankName = ref('')
const bankAccount = ref('')
const otherInfoCollapsed = ref(true)
const categories = ref<PartnerCategory[]>([])
const grades = ref<PartnerGrade[]>([])
const users = ref<UserInfo[]>([])
const warehouses = ref<WmsWarehouse[]>([])
const contacts = ref<ContactRowData[]>([])
const savedPartnerId = ref<number>()
/** 编辑模式已加载的客户 id（附件组件依赖） */
const loadedPartnerId = ref<number>()
const avatarUrl = ref('')
const avatarFileInput = ref<HTMLInputElement>()
const memberCardNo = ref('')
const memberName = ref('')
const initialPoints = ref(0)
const certRef = ref<any>(null)

// ── 会员信息（对标「会员信息」分区 + 会员管理子标签） ──
const memberLevel = ref<string | undefined>(undefined)
const memberCardStatus = ref<string | undefined>(undefined)
const memberValidRange = ref<[string, string] | undefined>(undefined)
const memberTotalConsume = ref<number | undefined>(undefined)
const memberIssueTime = ref<string | undefined>(undefined)
const currentPoints = ref<number | undefined>(undefined)
const memberCardStatusOptions = [
  { label: '正常', value: 'NORMAL' },
  { label: '停用', value: 'STOPPED' },
  { label: '已过期', value: 'EXPIRED' },
]

// ── 其他信息（对标「其他信息」分区：账期 / 推广人 / 来源 / 商城账号 / 证件） ──
const creditDays = ref<number | undefined>(undefined)
const fixedCreditDay = ref<number | undefined>(undefined)
const statementDay = ref<number | undefined>(undefined)
const promoterName = ref('')
const customerSource = ref<string | undefined>(undefined)
const region = ref('')
/** 所属区域下拉（对标：基本信息区「所属区域」带放大镜可选择区域档案） */
const regionOptions = ref<Array<{ label: string; value: string }>>([])
const buyerAccount = ref('')
const customerOnePass = ref('')
const businessLicenseExpiry = ref<string | undefined>(undefined)
const customerSourceOptions = [
  { label: '门店拜访', value: '门店拜访' },
  { label: '电话营销', value: '电话营销' },
  { label: '老客户介绍', value: '老客户介绍' },
  { label: '线上推广', value: '线上推广' },
  { label: '其他', value: '其他' },
]

const form = reactive({
  partnerName: '',
  partnerShortName: '',
  partnerCode: '',
  partnerCategoryId: undefined as number | undefined,
  partnerGradeId: undefined as number | undefined,
  defaultHandlerId: undefined as number | undefined,
  defaultHandlerName: undefined as string | undefined,
  defaultWarehouseId: undefined as number | undefined,
  province: '',
  city: '',
  district: '',
  companyFullName: '',
  unifiedSocialCode: '',
  taxId: '',
  legalPerson: '',
  companyPhone: '',
  companyEmail: '',
  industry: '',
  sourceChannel: '',
  settleType: '挂账',
  taxRate: 13,
  creditLimit: undefined as number | undefined,
  remark: '',
})

const formRules: Record<string, any> = {
  partnerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
}

async function loadCategories() { try { categories.value = await partnerCategoryApi.getTree('CUSTOMER') } catch { categories.value = [] } }
async function loadGrades() { try { grades.value = await partnerGradeApi.list('CUSTOMER') } catch { grades.value = [] } }
async function loadUsers() { try { const res = await userApi.getList(); users.value = (res as any)?.data || (res as any) || [] } catch { users.value = [] } }
async function loadWarehouses() { try { const res = await warehouseApi.listAll(); warehouses.value = (res as any)?.data || (res as any) || [] } catch { warehouses.value = [] } }
/** 所属区域选项（来源：客户区域管理 erp_customer_region） */
async function loadRegions() {
  try {
    const list = await customerRegionApi.list()
    regionOptions.value = (list || []).map((r: any) => ({ label: r.regionName, value: r.regionName }))
  } catch {
    regionOptions.value = []
  }
}
function filterUser(input: string, option: any) { return (option.label || '').toLowerCase().includes(input.toLowerCase()) }
/** 默认经手人：同步落库姓名，供销售单据/明细查询按名称展示与筛选 */
function onDefaultHandlerChange(value: any) {
  const u = users.value.find((x: any) => x.id === value)
  form.defaultHandlerName = u ? (u.nickname || u.username) : undefined
}
function handleCancel() { router.push('/md/customer/index') }

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

/** 汇总表单 → 后端 payload（新增/编辑共用；字段口径与列表 26 列一致） */
function buildPayload(): Record<string, any> {
  const roles = ['CUSTOMER', ...otherRoleOptions.value.filter(o => o.checked).map(o => o.value)].join(',')
  const warehouse = warehouses.value.find((w: any) => w.id === form.defaultWarehouseId)
  const grade = grades.value.find(g => g.id === form.partnerGradeId)
  // 单位所在地区 = 常用联系人所在地区（对标：基本信息区不再单列省/市/区）
  const primaryContact = contacts.value.find((c: any) => c.isDefault) || contacts.value[0]
  const areaPath = [primaryContact?.province, primaryContact?.city, primaryContact?.district].filter(Boolean) as string[]
  return {
    ...form,
    partnerType: 'CUSTOMER',
    roles,
    gradeName: grade ? grade.gradeName : (form as any).gradeName,
    status: statusChecked.value ? 'ENABLED' : 'DISABLED',
    province: areaPath[0] || undefined,
    city: areaPath[1] || undefined,
    district: areaPath[2] || undefined,
    // 期初信息
    openingReceivable: openingBalance.value,
    openingPreReceived: preReceivedAmount.value,
    // 纳税人信息 / 联系地址
    companyWebsite: companyWebsite.value,
    address: detailAddress.value,
    bankName: bankName.value,
    bankAccount: bankAccount.value,
    // 客户列表专属列
    warehouseName: warehouse ? ((warehouse as any).warehouseName || (warehouse as any).name) : undefined,
    region: region.value || undefined,
    buyerAccount: buyerAccount.value || undefined,
    customerOnePass: customerOnePass.value || undefined,
    customerSource: customerSource.value || undefined,
    businessLicenseExpiry: businessLicenseExpiry.value || undefined,
    creditDays: creditDays.value,
    fixedCreditDay: fixedCreditDay.value,
    statementDay: statementDay.value,
    promoterName: promoterName.value || undefined,
    partnerAvatar: avatarUrl.value || undefined,
    // 会员信息
    memberCardNo: memberCardNo.value || undefined,
    memberName: memberName.value || undefined,
    memberInitialPoints: initialPoints.value,
    points: currentPoints.value,
    memberLevel: memberLevel.value || undefined,
    memberCardStatus: memberCardStatus.value || undefined,
    memberValidStart: memberValidRange.value?.[0] || undefined,
    memberValidEnd: memberValidRange.value?.[1] || undefined,
    memberTotalConsume: memberTotalConsume.value,
    memberIssueTime: memberIssueTime.value || undefined,
  }
}

async function doSubmit(): Promise<number | null> {
  try { await formRef.value?.validate() } catch { return null }
  try {
    if (isEdit.value) {
      await partnerApi.update(editingId.value, buildPayload())
      return editingId.value
    }
    const result = await partnerApi.create(buildPayload())
    return (result as any)?.id || 0
  } catch (err: any) { message.error(err?.response?.data?.message || err?.message || (isEdit.value ? '保存失败' : '创建失败')); return null }
}

/** 编辑模式：回填客户档案 */
async function loadDetail(id: number) {
  try {
    const d: any = await partnerApi.getById(id)
    if (!d) { message.warning('客户不存在'); return }
    Object.assign(form, {
      partnerName: d.partnerName || '',
      partnerShortName: d.partnerShortName || '',
      partnerCode: d.partnerCode || '',
      partnerCategoryId: d.categoryId || undefined,
      partnerGradeId: undefined,
      defaultHandlerId: d.defaultHandlerId || undefined,
      defaultHandlerName: d.defaultHandlerName || undefined,
      companyFullName: d.companyFullName || '',
      taxId: d.taxNumber || '',
      legalPerson: d.legalPerson || '',
      companyPhone: d.phone || '',
      companyEmail: d.email || '',
      // 结款方式与列表/对标同口径：直接使用「挂账 / 现结」中文值，避免 MONTHLY/CASH 混用
      settleType: d.settleType || '挂账',
      // 单位所在地区（表单不再单独设输入项，仅用于回填常用联系人的所在地区）
      province: d.province || '',
      city: d.city || '',
      district: d.district || '',
      creditLimit: d.creditLimit ?? undefined,
      remark: d.remark || '',
    })
    // 客户级别（下拉以等级 id 为值，按名称反查）
    if (d.gradeName) {
      const hit = grades.value.find(g => g.gradeName === d.gradeName)
      form.partnerGradeId = hit ? hit.id : undefined
      if (!hit) (form as any).gradeName = d.gradeName
    }
    statusChecked.value = d.status !== 'DISABLED'
    openingBalance.value = d.openingReceivable ?? d.openingPayable ?? 0
    preReceivedAmount.value = d.openingPreReceived ?? d.openingPrepaid ?? 0
    companyWebsite.value = d.website || ''
    detailAddress.value = d.address || ''
    bankName.value = d.bankName || ''
    bankAccount.value = d.bankAccount || ''
    memberCardNo.value = d.memberCardNo || ''
    memberName.value = d.memberName || ''
    initialPoints.value = d.memberInitialPoints ?? 0
    currentPoints.value = d.points ?? undefined
    memberLevel.value = d.memberLevel || undefined
    memberCardStatus.value = d.memberCardStatus || undefined
    memberValidRange.value = d.memberValidStart && d.memberValidEnd ? [d.memberValidStart, d.memberValidEnd] : undefined
    memberTotalConsume.value = d.memberTotalConsume ?? undefined
    memberIssueTime.value = d.memberIssueTime ? String(d.memberIssueTime).replace('T', ' ').slice(0, 19) : undefined
    creditDays.value = d.creditDays ?? undefined
    fixedCreditDay.value = d.fixedCreditDay ?? undefined
    statementDay.value = d.statementDay ?? undefined
    promoterName.value = d.promoterName || ''
    customerSource.value = d.customerSource || undefined
    region.value = d.region || ''
    buyerAccount.value = d.buyerAccount || ''
    customerOnePass.value = d.customerOnePass || ''
    businessLicenseExpiry.value = d.businessLicenseExpiry ? String(d.businessLicenseExpiry).slice(0, 10) : undefined
    avatarUrl.value = d.partnerAvatar || ''
    // 其他业务关系（多重身份）
    const roles = String(d.roles || '').split(',').map((r: string) => r.trim())
    otherRoleOptions.value.forEach(o => { o.checked = roles.includes(o.value) })
    savedPartnerId.value = id
    loadedPartnerId.value = id
    // 联系人
    try {
      const list: any = await partnerContactApi.getByPartner(id)
      contacts.value = (list || []).map((c: any, i: number) => ({
        ...c,
        _uid: i + 1,
        isDefault: c.isPrimary,
        contactPhone: c.mobile || c.phone,
        contactEmail: c.email,
        region: c.region,
        detailAddress: c.detailAddress,
        deliveryMethod: c.deliveryMethod,
        deliveryRoute: c.deliveryRoute,
        openMallAccount: c.openMallAccount,
      }))
      // 历史数据：单位已有所在地区、联系人未填时，回填到常用联系人（单位地区 = 常用联系人地区）
      const first = contacts.value[0] as any
      if (first && !first.province && !first.city && !first.district && (form.province || form.city || form.district)) {
        first.province = form.province
        first.city = form.city
        first.district = form.district
        first.region = [form.province, form.city, form.district].filter(Boolean).join('/')
        first._areaPath = [form.province, form.city, form.district].filter(Boolean)
      }
    } catch { contacts.value = [] }
  } catch (e: any) {
    message.error(e?.message || '加载客户失败')
  }
}

/** 编辑模式：联系人增量同步（更新已有 / 新增 / 删除被移除的） */
/** 联系人分区 → 独立联系人(人) + 关联(单位)：统一走 /erp/contact/link（按手机/姓名归并同一个人，幂等） */
function contactLinkPayload(partnerId: number, c: any, isPrimary: number) {
  return {
    partyId: partnerId,
    contactId: c.contactId || undefined,
    contactName: c.contactName || '',
    mobile: c.contactPhone || c.mobile || c.phone || undefined,
    phone: c.phone || undefined,
    gender: c.gender || undefined,
    email: c.contactEmail || c.email || undefined,
    position: c.position || undefined,
    department: c.department || undefined,
    region: c.region || undefined,
    detailAddress: c.detailAddress || undefined,
    deliveryMethod: c.deliveryMethod || undefined,
    deliveryRoute: c.deliveryRoute || undefined,
    logisticsCompany: c.logisticsCompany || undefined,
    isPrimary,
  }
}

async function submitContacts(partnerId: number) {
  const ordered = [...contacts.value].sort((a, b) => (b.isDefault || 0) - (a.isDefault || 0))
  for (const c of ordered) {
    if (!c.contactName && !(c.contactPhone || c.mobile)) continue
    await request.post('/erp/contact/link', contactLinkPayload(partnerId, c, c.isDefault || 0))
  }
}

/** 编辑模式：先 upsert 全部（link 幂等），再解除被移除联系人与本单位的关联 */
async function syncContactsOnEdit(partnerId: number) {
  await submitContacts(partnerId)
  const existing: any = await partnerContactApi.getByPartner(partnerId).catch(() => [])
  const keep = new Set(contacts.value.filter(c => c.contactId || c.id).map(c => String(c.contactId || c.id)))
  for (const rel of (existing || [])) {
    if (!rel.contactId) continue
    if (!keep.has(String(rel.contactId))) {
      await request.delete(`/erp/contact/${rel.contactId}/parties/${rel.id}`).catch(() => {})
    }
  }
}

async function submitRoles(partnerId: number) {
  const allRoles = ['CUSTOMER', ...otherRoleOptions.value.filter(o => o.checked).map(o => o.value)]
  for (const roleType of allRoles) {
    try { await partnerRoleApi.addRole(partnerId, roleType, roleType === 'CUSTOMER') } catch {}
  }
}

async function submitBankAccount(partnerId: number) {
  if (!bankName.value && !bankAccount.value) return
  try { await partnerBankAccountApi.create({ partnerId, accountName: form.partnerName, bankName: bankName.value, accountNo: bankAccount.value, isDefault: 1 }) } catch {}
}

async function handleSubmit() {
  saving.value = true
  try {
    const pid = await doSubmit()
    if (!pid) return
    await certRef.value?.sync(pid)
    if (isEdit.value) {
      await syncContactsOnEdit(pid)
      message.success('客户保存成功')
    } else {
      await submitRoles(pid)
      await submitContacts(pid)
      await submitBankAccount(pid)
      savedPartnerId.value = pid
      message.success('客户创建成功')
    }
    router.push('/md/customer/index')
  } finally { saving.value = false }
}

async function handleSaveAndNew() {
  saving.value = true
  try {
    const pid = await doSubmit()
    if (!pid) return
    await certRef.value?.sync(pid)
    await submitRoles(pid)
    await submitContacts(pid)
    await submitBankAccount(pid)
    savedPartnerId.value = pid
    message.success('客户创建成功')
    Object.assign(form, { partnerName:'', partnerShortName:'', partnerCode:'', partnerCategoryId:undefined, partnerGradeId:undefined, defaultHandlerId:undefined, defaultHandlerName:undefined, defaultWarehouseId:undefined, province:'', city:'', district:'', companyFullName:'', unifiedSocialCode:'', taxId:'', legalPerson:'', companyPhone:'', companyEmail:'', industry:'', sourceChannel:'', settleType:'挂账', taxRate:13, creditLimit:undefined, remark:'' })
    contacts.value = []; bankName.value = ''; bankAccount.value = ''; detailAddress.value = ''
    openingBalance.value = 0; preReceivedAmount.value = 0; companyWebsite.value = ''
    avatarUrl.value = ''; memberCardNo.value = ''; memberName.value = ''; initialPoints.value = 0
    await generateCode()
  } finally { saving.value = false }
}

function handleKeydown(e: KeyboardEvent) { if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSubmit() } }

onMounted(async () => {
  await Promise.all([loadCategories(), loadGrades(), loadUsers(), loadWarehouses(), loadRegions()])
  if (isEdit.value) {
    await loadDetail(editingId.value)
  } else {
    await generateCode()
  }
  document.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => { document.removeEventListener('keydown', handleKeydown) })
</script>

<style scoped>
.form-page-wrapper {
  display: flex; flex-direction: column; height: 100%; overflow: hidden;
}
.page-header { padding: 12px 16px; flex-shrink: 0; }
.page-title { font-size: 16px; font-weight: 600; color: #262626; }
.top-options { background: #fff; padding: 12px 20px; margin: 0 16px 8px; border-radius: 6px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); flex-shrink: 0; }

/* 可滚动内容区 */
.form-scroll-area {
  flex: 1; overflow-y: auto; padding: 0 16px 8px; min-height: 0;
}

.section-row :deep(.ant-form-item) { margin-bottom: 10px; }
.section-row :deep(.ant-form-item-label > label) { font-size: 13px; color: #595959; }

/* 固定底部按钮栏 */
.form-footer {
  background: #fff; border-radius: 6px; padding: 16px 24px;
  text-align: right; box-shadow: 0 -1px 4px rgba(0,0,0,0.05);
  flex-shrink: 0; margin: 0 16px 16px;
}

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
