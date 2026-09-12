<template>
  <div class="supplier-form-page">
    <!-- ═══ 顶部选项行（对标：既是供应商又是客户 / 启用价格跟踪 / 名称重复配置） ═══ -->
    <div class="top-options">
      <a-checkbox v-model:checked="form.isAlsoCustomer">
        既是供应商又是客户
      </a-checkbox>
      <a-checkbox v-model:checked="form.priceTrackEnabled">
        启用价格跟踪
      </a-checkbox>
      <a class="dup-config-link" @click="openDupConfig">名称重复配置</a>
      <span class="dup-config-hint">{{ dupConfigLabel }}</span>
    </div>

    <div class="form-body">
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        :colon="false"
      >
        <!-- ═══ 基础信息 ═══ -->
        <section class="section">
          <h4 class="section-title">
            基础信息
          </h4>
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item
                label="供应商名称"
                name="partnerName"
              >
                <a-input
                  v-model:value="form.partnerName"
                  placeholder="请输入供应商名称"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="供应商编号"
                name="partnerCode"
              >
                <a-input
                  v-model:value="form.partnerCode"
                  placeholder="由系统自动生成，可修改"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="所属分类"
                name="partnerCategoryId"
              >
                <div class="category-picker">
                  <a-tree-select
                    v-model:value="form.partnerCategoryId"
                    :tree-data="categoryTreeData"
                    :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
                    placeholder="请选择所属分类"
                    size="small"
                    allow-clear
                    tree-default-expand-all
                    style="flex: 1"
                  />
                  <a-button
                    type="text"
                    size="small"
                    title="新增分类"
                    @click="openCategoryModal"
                  >
                    <PlusOutlined />
                  </a-button>
                </div>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="助记码">
                <a-input
                  v-model:value="form.mnemonicCode"
                  placeholder="请输入助记码"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="16">
              <a-form-item label="备注">
                <a-input
                  v-model:value="form.remark"
                  placeholder="请输入备注"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
        </section>

        <!-- ═══ 联系人 ═══ -->
        <section class="section">
          <h4 class="section-title">
            联系人
          </h4>
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="联系人">
                <a-input
                  v-model:value="form.contactPerson"
                  placeholder="请输入联系人"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="联系电话">
                <a-input
                  v-model:value="form.contactPhone"
                  placeholder="请输入联系电话"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="联系地址">
                <a-input
                  v-model:value="form.contactAddress"
                  placeholder="请输入联系地址"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
        </section>

        <!-- ═══ 纳税人信息 ═══ -->
        <section class="section">
          <h4 class="section-title">
            纳税人信息
          </h4>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="公司全称">
                <a-input
                  v-model:value="form.companyFullName"
                  placeholder="请输入公司全称"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="税号">
                <a-input
                  v-model:value="form.taxNumber"
                  placeholder="请输入纳税人识别号"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="地址">
                <a-input
                  v-model:value="form.taxAddress"
                  placeholder="请输入纳税人地址"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="电话">
                <a-input
                  v-model:value="form.phone"
                  placeholder="请输入电话"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="开户行">
                <a-input
                  v-model:value="form.bankName"
                  placeholder="请输入开户行"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="银行账号">
                <a-input
                  v-model:value="form.bankAccount"
                  placeholder="请输入银行账号"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
        </section>

        <!-- ═══ 期初信息（付款/预付余额由财务记账维护，此处只读展示） ═══ -->
        <section class="section">
          <h4 class="section-title">
            期初信息
          </h4>
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="期初应付金额">
                <a-input-number
                  v-model:value="form.openingPayable"
                  :precision="2"
                  disabled
                  style="width: 100%"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="期初预付金额">
                <a-input-number
                  v-model:value="form.openingPrepaid"
                  :precision="2"
                  disabled
                  style="width: 100%"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </section>

        <!-- ═══ 其他信息 ═══ -->
        <section class="section">
          <h4 class="section-title">
            其他信息
          </h4>
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="付款期限">
                <div class="payment-term">
                  <a-radio-group
                    v-model:value="form.paymentTermType"
                    size="small"
                  >
                    <a-radio value="DYNAMIC">
                      动态付款期限(天)
                    </a-radio>
                  </a-radio-group>
                  <a-input-number
                    v-model:value="form.paymentDays"
                    :min="0"
                    :max="365"
                    :disabled="form.paymentTermType !== 'DYNAMIC'"
                    size="small"
                    style="width: 120px"
                  />
                </div>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label=" ">
                <div class="payment-term">
                  <a-radio-group
                    v-model:value="form.paymentTermType"
                    size="small"
                  >
                    <a-radio value="FIXED">
                      固定账期
                    </a-radio>
                  </a-radio-group>
                  <a-select
                    v-model:value="form.fixedPaymentDay"
                    :disabled="form.paymentTermType !== 'FIXED'"
                    size="small"
                    style="width: 120px"
                  >
                    <a-select-option
                      v-for="d in 31"
                      :key="d"
                      :value="d"
                    >
                      {{ d }}号
                    </a-select-option>
                  </a-select>
                </div>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="结算期">
                <a-select
                  v-model:value="form.settlementDay"
                  size="small"
                  style="width: 160px"
                >
                  <a-select-option
                    v-for="d in 31"
                    :key="d"
                    :value="d"
                  >
                    {{ d }}号
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="经营系列">
                <a-input
                  v-model:value="form.operatingSeries"
                  placeholder="请输入经营系列"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="经营面积">
                <a-input-number
                  v-model:value="form.operatingArea"
                  :precision="2"
                  :min="0"
                  placeholder="0"
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </section>

        <!-- ═══ 证件信息（通用组件：营业执照固定，其余可选/自定义，始终保留一个待输入位） ═══ -->
        <section class="section">
          <CertUploadList
            ref="certRef"
            :partner-id="partyId"
          />
        </section>

        <!-- ═══ 附件 ═══ -->
        <section class="section">
          <AttachmentUpload
            ref="attachmentRef"
            :partner-id="partyId"
          />
        </section>
      </a-form>
    </div>

    <!-- ═══ 底部保存 ═══ -->
    <div class="form-footer">
      <a-space>
        <a-button @click="handleCancel">
          取消
        </a-button>
        <a-button
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          保存(Enter)
        </a-button>
      </a-space>
    </div>

    <!-- 新增分类 -->
    <a-modal
      v-model:open="categoryModalVisible"
      title="新增分类"
      :confirm-loading="categorySaving"
      width="420px"
      @ok="handleCategorySave"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="分类名称">
          <a-input
            v-model:value="categoryName"
            placeholder="请输入分类名称"
            size="small"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 名称重复配置 -->
    <a-modal
      v-model:open="dupConfigVisible"
      title="名称重复配置"
      :confirm-loading="dupConfigSaving"
      width="420px"
      @ok="saveDupConfig"
    >
      <a-radio-group v-model:value="dupConfigValue">
        <a-radio value="ALLOW">
          允许重复（不校验）
        </a-radio>
        <a-space />
        <a-radio value="WARN">
          提示重复（可继续保存）
        </a-radio>
        <a-space />
        <a-radio value="FORBID">
          禁止重复（同名不可保存）
        </a-radio>
      </a-radio-group>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import AttachmentUpload from '../components/AttachmentUpload.vue'
import CertUploadList from '@/components/CertUploadList/CertUploadList.vue'
import {
  partnerApi,
  partnerCategoryApi,
  type PartnerCategory,
} from '@/api/erp/partner'
import configApi from '@/api/config'
import { generatePartnerCodeAsync } from '../utils/generateCode'

defineOptions({ name: 'MdSupplierForm' })

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const attachmentRef = ref<any>(null)

const partyId = ref<number | undefined>(undefined)
const certRef = ref<any>(null)
const saving = ref(false)

const form = reactive({
  partnerName: '',
  partnerCode: '',
  mnemonicCode: '',
  partnerCategoryId: undefined as any,
  remark: '',
  isAlsoCustomer: false,
  priceTrackEnabled: false,
  contactPerson: '',
  contactPhone: '',
  contactAddress: '',
  companyFullName: '',
  taxNumber: '',
  taxAddress: '',
  phone: '',
  bankName: '',
  bankAccount: '',
  openingPayable: 0,
  openingPrepaid: 0,
  paymentTermType: 'DYNAMIC' as 'DYNAMIC' | 'FIXED',
  paymentDays: 30,
  fixedPaymentDay: 1,
  settlementDay: 1,
  operatingSeries: '',
  operatingArea: undefined as number | undefined,
})

const rules = {
  partnerName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  partnerCode: [{ required: true, message: '请填写供应商编号', trigger: 'blur' }],
  partnerCategoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
}

// ═══ 分类树 ═══
const categoryList = ref<PartnerCategory[]>([])
const categoryTreeData = computed<any[]>(() => [
  { id: 0, categoryName: '全部供应商', children: categoryList.value },
])

async function loadCategories() {
  try {
    const tree = await partnerCategoryApi.getTree('SUPPLIER')
    categoryList.value = Array.isArray(tree) ? tree : []
  } catch {
    categoryList.value = []
  }
}

const categoryModalVisible = ref(false)
const categorySaving = ref(false)
const categoryName = ref('')
function openCategoryModal() {
  categoryName.value = ''
  categoryModalVisible.value = true
}
async function handleCategorySave() {
  if (!categoryName.value.trim()) {
    message.warning('请输入分类名称')
    return
  }
  categorySaving.value = true
  try {
    await partnerCategoryApi.create({
      categoryName: categoryName.value.trim(),
      categoryType: 'SUPPLIER',
      sortOrder: 0,
    } as any)
    message.success('分类新增成功')
    categoryModalVisible.value = false
    await loadCategories()
  } catch (e: any) {
    message.error(e?.message || '分类新增失败')
  } finally {
    categorySaving.value = false
  }
}

// ═══ 名称重复配置（系统配置持久化：md:supplier:name-duplicate） ═══
const DUP_CONFIG_KEY = 'md:supplier:name-duplicate'
const DUP_LABELS: Record<string, string> = {
  ALLOW: '当前：允许重复',
  WARN: '当前：提示重复',
  FORBID: '当前：禁止重复',
}
const dupConfigVisible = ref(false)
const dupConfigSaving = ref(false)
const dupConfigValue = ref('ALLOW')
const dupConfigLabel = computed(() => DUP_LABELS[dupConfigValue.value] || '')

async function loadDupConfig() {
  try {
    const value = await configApi.getValue(DUP_CONFIG_KEY)
    if (typeof value === 'string' && DUP_LABELS[value]) {
      dupConfigValue.value = value
    }
  } catch {
    // 未配置时保持默认「允许重复」
  }
}
function openDupConfig() {
  dupConfigVisible.value = true
}
async function saveDupConfig() {
  dupConfigSaving.value = true
  try {
    await configApi.saveValue(DUP_CONFIG_KEY, dupConfigValue.value)
    message.success('名称重复配置已保存')
    dupConfigVisible.value = false
  } catch {
    message.error('配置保存失败')
  } finally {
    dupConfigSaving.value = false
  }
}

/** 按配置校验同名供应商；返回 true 表示可继续保存 */
async function checkDuplicateName(): Promise<boolean> {
  if (dupConfigValue.value === 'ALLOW') return true
  try {
    const res: any = await partnerApi.page({
      partnerType: 'supplier',
      keyword: form.partnerName.trim(),
      pageNum: 1,
      pageSize: 50,
    } as any)
    const same = (res?.records || []).filter(
      (r: any) => r.partnerName === form.partnerName.trim() && String(r.id) !== String(partyId.value ?? ''),
    )
    if (same.length === 0) return true
    if (dupConfigValue.value === 'FORBID') {
      message.error(`已存在同名供应商「${form.partnerName.trim()}」，当前配置为禁止重复`)
      return false
    }
    return await new Promise<boolean>(resolve => {
      Modal.confirm({
        title: '名称重复提示',
        content: `已存在 ${same.length} 个同名供应商，是否继续保存？`,
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      })
    })
  } catch {
    return true
  }
}

// ═══ 编辑回填 ═══
async function loadDetail(id: number) {
  const data: any = await partnerApi.getById(id)
  if (!data) {
    message.error('供应商不存在或已删除')
    return
  }
  partyId.value = id
  Object.assign(form, {
    partnerName: data.partnerName || '',
    partnerCode: data.partnerCode || '',
    mnemonicCode: data.mnemonicCode || '',
    partnerCategoryId: data.categoryId ?? undefined,
    remark: data.remark || '',
    isAlsoCustomer: String(data.roles || '').includes('CUSTOMER'),
    priceTrackEnabled: data.priceTrackEnabled === 1,
    contactPerson: data.contactPerson || '',
    contactPhone: data.contactPhone || '',
    contactAddress: data.address || '',
    companyFullName: data.companyFullName || '',
    taxNumber: data.taxNumber || '',
    taxAddress: data.taxAddress || '',
    phone: data.phone || '',
    bankName: data.bankName || '',
    bankAccount: data.bankAccount || '',
    openingPayable: Number(data.openingPayable || 0),
    openingPrepaid: Number(data.openingPrepaid || 0),
    paymentTermType: (data.paymentTermType || 'DYNAMIC') as 'DYNAMIC' | 'FIXED',
    paymentDays: data.paymentDays ?? 30,
    fixedPaymentDay: data.fixedPaymentDay ?? 1,
    settlementDay: data.settlementDay ?? 1,
    operatingSeries: data.operatingSeries || '',
    operatingArea: data.operatingArea === null || data.operatingArea === undefined
      ? undefined
      : Number(data.operatingArea),
  })
}

// ═══ 保存 ═══
async function buildPayload() {
  const roles = form.isAlsoCustomer ? 'SUPPLIER,CUSTOMER' : 'SUPPLIER'
  return {
    partnerType: 'SUPPLIER',
    status: 'ENABLED',
    roles,
    partnerName: form.partnerName.trim(),
    partnerCode: form.partnerCode.trim(),
    mnemonicCode: form.mnemonicCode || undefined,
    partnerCategoryId: form.partnerCategoryId || undefined,
    remark: form.remark || undefined,
    companyFullName: form.companyFullName || undefined,
    taxNumber: form.taxNumber || undefined,
    address: form.taxAddress || undefined,
    phone: form.phone || undefined,
    bankName: form.bankName || undefined,
    bankAccount: form.bankAccount || undefined,
    openingPayable: Number(form.openingPayable || 0),
    openingPrepaid: Number(form.openingPrepaid || 0),
    paymentTermType: form.paymentTermType,
    paymentDays: Number(form.paymentDays || 0),
    fixedPaymentDay: Number(form.fixedPaymentDay || 1),
    settlementDay: Number(form.settlementDay || 1),
    operatingSeries: form.operatingSeries || undefined,
    operatingArea: form.operatingArea === undefined ? undefined : Number(form.operatingArea),
    priceTrackEnabled: form.priceTrackEnabled ? 1 : 0,
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!(await checkDuplicateName())) return

  saving.value = true
  try {
    const payload = await buildPayload()
    let id = partyId.value
    if (id) {
      await partnerApi.update(id, payload)
    } else {
      const created: any = await partnerApi.create(payload)
      id = Number(created?.id || 0)
      if (!id) {
        message.error('保存失败：未获取到新建供应商 ID')
        return
      }
      partyId.value = id
    }
    await certRef.value?.sync(id!)
    // 联系人分区（联系人 / 联系电话 / 联系地址）落到主联系人记录
    await partnerApi.savePrimaryContact(id!, {
      contactPerson: form.contactPerson || undefined,
      contactPhone: form.contactPhone || undefined,
      address: form.contactAddress || undefined,
    })
    message.success(partyId.value === id ? '保存成功' : '新增成功')
    router.push('/md/supplier/index')
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  router.push('/md/supplier/index')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.ctrlKey && !e.metaKey) {
    const tag = (e.target as HTMLElement)?.tagName
    if (tag === 'TEXTAREA') return
    e.preventDefault()
    handleSave()
  }
}

onMounted(async () => {
  await loadCategories()
  await loadDupConfig()
  const id = Number(route.query.id || route.params.id || 0)
  if (id) {
    await loadDetail(id)
  } else {
    form.partnerCode = await generatePartnerCodeAsync('supplier')
  }
  document.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.supplier-form-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: #f5f7fa;
}
.top-options {
  display: flex;
  align-items: center;
  gap: 24px;
  background: #fff;
  padding: 12px 20px;
  flex-shrink: 0;
  border-bottom: 1px solid #f0f0f0;
}
.dup-config-link {
  color: #ff6b35;
  font-size: 13px;
}
.dup-config-hint {
  color: #999;
  font-size: 12px;
}
.form-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 16px;
}
.section {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 16px 20px 0;
  margin-bottom: 12px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 14px;
}
.category-picker {
  display: flex;
  align-items: center;
  gap: 4px;
}
.payment-term {
  display: flex;
  align-items: center;
  gap: 8px;
}
.form-footer {
  background: #fff;
  border-top: 1px solid #e8e8e8;
  padding: 12px 24px;
  text-align: right;
  flex-shrink: 0;
}
:deep(.ant-form-item) {
  margin-bottom: 14px;
}
:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #606266;
}
</style>
