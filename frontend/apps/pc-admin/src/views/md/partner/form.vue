<template>
  <div class="partner-form-page">
    <!-- ═══ 顶部选项行：该单位的其他业务关系（多重身份） ═══ -->
    <div class="top-options">
      <span class="top-options-label">该单位还有其他业务关系：</span>
      <a-checkbox
        v-for="opt in roleOptions"
        :key="opt.value"
        v-model:checked="opt.checked"
      >
        {{ opt.label }}
      </a-checkbox>
      <a-checkbox
        v-if="hasCustomerRole"
        v-model:checked="form.priceTrackEnabled"
        style="margin-left: 12px"
      >
        启用销售价格跟踪
      </a-checkbox>
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
                label="单位名称"
                name="partnerName"
              >
                <a-input
                  v-model:value="form.partnerName"
                  placeholder="请输入单位名称"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="单位编号"
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
                label="单位类别"
                name="partnerCategoryId"
              >
                <div class="category-picker">
                  <a-tree-select
                    v-model:value="form.partnerCategoryId"
                    :tree-data="categoryTreeData"
                    :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
                    placeholder="如：银行 / 政府机构 / 劳务公司"
                    size="small"
                    allow-clear
                    tree-default-expand-all
                    style="flex: 1"
                  />
                  <a-button
                    type="text"
                    size="small"
                    title="新增类别"
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
            <a-col :span="8">
              <a-form-item label="状态">
                <a-switch
                  v-model:checked="statusChecked"
                  checked-children="启用"
                  un-checked-children="停用"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
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
              <a-form-item label="开户行">
                <a-input
                  v-model:value="form.bankName"
                  placeholder="请输入开户行"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
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

        <!-- ═══ 期初信息 ═══ -->
        <section class="section">
          <h4 class="section-title">
            期初信息
          </h4>
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="期初应收金额">
                <a-input-number
                  v-model:value="form.openingReceivable"
                  :precision="2"
                  :min="0"
                  style="width: 100%"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="期初应付金额">
                <a-input-number
                  v-model:value="form.openingPayable"
                  :precision="2"
                  :min="0"
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
              <a-form-item label="默认经手人">
                <a-select
                  v-model:value="form.defaultHandlerId"
                  placeholder="请选择经手人"
                  allow-clear
                  show-search
                  size="small"
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
            <a-col :span="8">
              <a-form-item label="结款方式">
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
          v-if="!partyId"
          :loading="saving"
          @click="handleSave(true)"
        >
          保存并新增
        </a-button>
        <a-button
          type="primary"
          :loading="saving"
          @click="handleSave(false)"
        >
          保存并返回列表(Ctrl+Enter)
        </a-button>
      </a-space>
    </div>

    <!-- ═══ 新增类别 ═══ -->
    <a-modal
      v-model:open="categoryModalVisible"
      title="新增单位类别"
      :confirm-loading="categorySaving"
      width="420px"
      @ok="handleCategorySave"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="类别名称">
          <a-input
            v-model:value="categoryName"
            placeholder="如：银行 / 政府机构 / 劳务公司"
            size="small"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import AttachmentUpload from '../components/AttachmentUpload.vue'
import CertUploadList from '@/components/CertUploadList/CertUploadList.vue'
import {
  partnerApi,
  partnerCategoryApi,
  type PartnerCategory,
} from '@/api/erp/partner'
import { userApi, type UserInfo } from '@/api/user'
import { generatePartnerCodeAsync } from '../utils/generateCode'

defineOptions({ name: 'MdPartnerForm' })

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const attachmentRef = ref<any>(null)

/** 编辑态主键：有值=编辑，无值=新增 */
const partyId = ref<number | undefined>(undefined)
const certRef = ref<any>(null)
const saving = ref(false)
const statusChecked = ref(true)

/** 多重身份：本页固定 OTHER，可叠加客户/供应商/物流 */
const roleOptions = ref([
  { value: 'CUSTOMER', label: '客户', checked: false },
  { value: 'SUPPLIER', label: '供应商', checked: false },
  { value: 'LOGISTICS', label: '物流', checked: false },
])
const hasCustomerRole = computed(() => roleOptions.value.some(o => o.value === 'CUSTOMER' && o.checked))

const users = ref<UserInfo[]>([])

const form = reactive({
  partnerName: '',
  partnerCode: '',
  mnemonicCode: '',
  partnerCategoryId: undefined as any,
  remark: '',
  priceTrackEnabled: false,
  contactPerson: '',
  contactPhone: '',
  contactAddress: '',
  companyFullName: '',
  taxNumber: '',
  taxAddress: '',
  bankName: '',
  bankAccount: '',
  openingReceivable: 0,
  openingPayable: 0,
  defaultHandlerId: undefined as any,
  settleType: '挂账',
})

const rules = {
  partnerName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
  partnerCode: [{ required: true, message: '请填写单位编号', trigger: 'blur' }],
  // 其他往来单位的核心标识就是「单位类别」（银行/政府机构/劳务公司…），与供应商页口径一致强制选择
  partnerCategoryId: [{ required: true, message: '请选择单位类别', trigger: 'change' }],
}

// ═══ 类别树（其他往来单位口径 OTHER） ═══
const categoryList = ref<PartnerCategory[]>([])
const categoryTreeData = computed<any[]>(() => [
  { id: 0, categoryName: '全部单位', children: categoryList.value },
])

async function loadCategories() {
  try {
    const tree = await partnerCategoryApi.getTree('OTHER')
    categoryList.value = Array.isArray(tree) ? tree : []
  } catch {
    categoryList.value = []
  }
}

const categoryModalVisible = ref(false)
const categorySaving = ref(false)
const categoryName = ref('')

/** 按名称在类别树中查找（新增类别后自动选中用） */
function findCategoryByName(nodes: PartnerCategory[], name: string): PartnerCategory | undefined {
  for (const n of nodes) {
    if (n.categoryName === name) return n
    if (n.children?.length) {
      const hit = findCategoryByName(n.children, name)
      if (hit) return hit
    }
  }
  return undefined
}

function openCategoryModal() {
  categoryName.value = ''
  categoryModalVisible.value = true
}
async function handleCategorySave() {
  if (!categoryName.value.trim()) {
    message.warning('请输入类别名称')
    return
  }
  categorySaving.value = true
  try {
    const name = categoryName.value.trim()
    await partnerCategoryApi.create({
      categoryName: name,
      categoryType: 'OTHER',
      sortOrder: 0,
    } as any)
    message.success('类别新增成功')
    categoryModalVisible.value = false
    await loadCategories()
    // 新建后自动选中（按名称回查，兼容 create 的多种返回形态）
    const created = findCategoryByName(categoryList.value, name)
    if (created) {
      form.partnerCategoryId = created.id
    }
  } catch (e: any) {
    message.error(e?.message || '类别新增失败')
  } finally {
    categorySaving.value = false
  }
}

async function loadUsers() {
  try {
    const res: any = await userApi.getList()
    users.value = res?.data || res || []
  } catch {
    users.value = []
  }
}
function filterUser(input: string, option: any) {
  return String(option?.label || '').toLowerCase().includes(String(input).toLowerCase())
}

// ═══ 编辑回填 ═══
async function loadDetail(id: number) {
  const data: any = await partnerApi.getById(id)
  if (!data) {
    message.error('其他往来单位不存在或已删除')
    return
  }
  partyId.value = id
  const roles = String(data.roles || '')
  statusChecked.value = data.status !== 'DISABLED'
  roleOptions.value.forEach(opt => {
    opt.checked = roles.includes(opt.value)
  })
  Object.assign(form, {
    partnerName: data.partnerName || '',
    partnerCode: data.partnerCode || '',
    mnemonicCode: data.mnemonicCode || '',
    partnerCategoryId: data.categoryId ?? undefined,
    remark: data.remark || '',
    priceTrackEnabled: data.priceTrackEnabled === 1,
    contactPerson: data.contactPerson || '',
    contactPhone: data.contactPhone || '',
    contactAddress: data.address || '',
    companyFullName: data.companyFullName || '',
    taxNumber: data.taxNumber || '',
    taxAddress: data.taxAddress || '',
    bankName: data.bankName || '',
    bankAccount: data.bankAccount || '',
    openingReceivable: Number(data.openingReceivable || 0),
    openingPayable: Number(data.openingPayable || 0),
    defaultHandlerId: data.defaultHandlerId ?? undefined,
    settleType: data.settleType === '现结' ? '现结' : '挂账',
  })
}

// ═══ 保存 ═══
function buildRoles(): string {
  const extra = roleOptions.value.filter(o => o.checked).map(o => o.value)
  return ['OTHER', ...extra].join(',')
}

function buildPayload() {
  const handlerId = form.defaultHandlerId ? Number(form.defaultHandlerId) : undefined
  const handler = users.value.find(u => Number(u.id) === handlerId)
  return {
    partnerType: 'OTHER',
    status: statusChecked.value ? 'ENABLED' : 'DISABLED',
    roles: buildRoles(),
    partnerName: form.partnerName.trim(),
    partnerCode: form.partnerCode.trim(),
    mnemonicCode: form.mnemonicCode || undefined,
    partnerCategoryId: form.partnerCategoryId || undefined,
    remark: form.remark || undefined,
    companyFullName: form.companyFullName || undefined,
    taxNumber: form.taxNumber || undefined,
    address: form.taxAddress || undefined,
    bankName: form.bankName || undefined,
    bankAccount: form.bankAccount || undefined,
    openingReceivable: Number(form.openingReceivable || 0),
    openingPayable: Number(form.openingPayable || 0),
    defaultHandlerId: handlerId,
    defaultHandlerName: handler?.nickname || handler?.username || undefined,
    settleType: form.settleType || '挂账',
    priceTrackEnabled: form.priceTrackEnabled ? 1 : 0,
  }
}

/**
 * 保存
 * @param andNew true=保存并新增（保存后清空表单继续录入）
 */
async function handleSave(andNew: boolean) {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    let id = partyId.value
    if (id) {
      await partnerApi.update(id, payload as any)
    } else {
      const created: any = await partnerApi.create(payload as any)
      id = Number(created?.id || 0)
      if (!id) {
        message.error('保存失败：未获取到新建单位 ID')
        return
      }
      partyId.value = id
    }
    await certRef.value?.sync(id!)
    // 联系人分区落到主联系人记录（biz_party_contact，is_primary=1）
    await partnerApi.savePrimaryContact(id!, {
      contactPerson: form.contactPerson || undefined,
      contactPhone: form.contactPhone || undefined,
      address: form.contactAddress || undefined,
    })

    if (andNew) {
      message.success('保存成功，可继续新增')
      resetForm()
      await loadCategories()
    } else {
      message.success(partyId.value === id && !andNew ? '保存成功' : '新增成功')
      router.push('/md/partner/index')
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function resetForm() {
  partyId.value = undefined
  statusChecked.value = true
  roleOptions.value.forEach(o => { o.checked = false })
  Object.assign(form, {
    partnerName: '',
    partnerCode: '',
    mnemonicCode: '',
    partnerCategoryId: undefined,
    remark: '',
    priceTrackEnabled: false,
    contactPerson: '',
    contactPhone: '',
    contactAddress: '',
    companyFullName: '',
    taxNumber: '',
    taxAddress: '',
    bankName: '',
    bankAccount: '',
    openingReceivable: 0,
    openingPayable: 0,
    defaultHandlerId: undefined,
    settleType: '挂账',
  })
  form.partnerCode = await generatePartnerCodeAsync('partner')
}

function handleCancel() {
  router.push('/md/partner/index')
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSave(false)
  }
}

onMounted(async () => {
  await loadCategories()
  await loadUsers()
  const id = Number(route.query.id || route.params.id || 0)
  if (id) {
    await loadDetail(id)
  } else {
    form.partnerCode = await generatePartnerCodeAsync('partner')
  }
  document.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.partner-form-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: #f5f7fa;
}
.top-options {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  padding: 12px 20px;
  flex-shrink: 0;
  border-bottom: 1px solid #f0f0f0;
}
.top-options-label {
  font-size: 13px;
  color: #595959;
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
