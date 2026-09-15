<template>
  <div class="form-page-wrapper">
    <!-- ═══ 页头 ═══ -->
    <div class="page-header">
      <a-space :size="8">
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
        <span class="page-title">{{ isEdit ? '物流公司 - 修改' : '物流公司 - 新增' }}</span>
      </a-space>
    </div>

    <div class="form-scroll-area">
      <a-form
        ref="formRef"
        layout="horizontal"
        :label-col="{ span: 8 }"
        :wrapper-col="{ span: 16 }"
        :colon="false"
      >
        <!-- ═══ 基础信息（对标：编号/名称、助记码/简称、备注） ═══ -->
        <FormSection>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item
                label="物流公司编号"
                required
              >
                <a-input
                  v-model:value="form.partnerCode"
                  placeholder="由系统自动生成"
                  size="small"
                  disabled
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="物流公司名称"
                required
              >
                <a-input
                  v-model:value="form.partnerName"
                  placeholder="请输入物流公司名称"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="助记码">
                <a-input
                  v-model:value="form.mnemonicCode"
                  placeholder="请输入助记码"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="简称">
                <a-input
                  v-model:value="form.shortName"
                  placeholder="请输入简称"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="24">
            <a-col :span="24">
              <a-form-item
                label="备注"
                :label-col="{ span: 4 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-input
                  v-model:value="form.remark"
                  placeholder="请输入备注"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>

        <!-- ═══ 网点（可多行，第 1 行为主网点） ═══ -->
        <FormSection title="网点">
          <template #extra>
            <a-button
              type="link"
              size="small"
              class="btn-add-branch"
              @click="addBranch"
            >
              <PlusOutlined /> 新增
            </a-button>
          </template>

          <div
            v-for="(branch, index) in branches"
            :key="branch._key"
            class="branch-block"
          >
            <div class="branch-header">
              <span class="branch-title">网点{{ index + 1 }}</span>
              <a-button
                v-if="branches.length > 1"
                type="link"
                size="small"
                danger
                @click="removeBranch(index)"
              >
                <DeleteOutlined /> 删除
              </a-button>
            </div>
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="网点名称"
                  :label-col="{ span: 8 }"
                  :wrapper-col="{ span: 16 }"
                >
                  <a-input
                    v-model:value="branch.contactName"
                    placeholder="请输入网点名称"
                    size="small"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="联系人"
                  :label-col="{ span: 8 }"
                  :wrapper-col="{ span: 16 }"
                >
                  <a-input
                    v-model:value="branch.linkman"
                    placeholder="请输入联系人"
                    size="small"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="联系电话"
                  :label-col="{ span: 8 }"
                  :wrapper-col="{ span: 16 }"
                >
                  <a-input
                    v-model:value="branch.phone"
                    placeholder="请输入联系电话"
                    size="small"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="联系地址"
                  :label-col="{ span: 8 }"
                  :wrapper-col="{ span: 16 }"
                >
                  <a-input
                    v-model:value="branch.detailAddress"
                    placeholder="请输入联系地址"
                    size="small"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </FormSection>

        <!-- ═══ 纳税人信息 ═══ -->
        <FormSection title="纳税人信息">
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
              <a-form-item label="纳税人识别号">
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
                  v-model:value="form.address"
                  placeholder="请输入地址"
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
              <a-form-item label="开户行地址">
                <a-input
                  v-model:value="form.bankAddress"
                  placeholder="请输入开户行地址"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="开户行账号">
                <a-input
                  v-model:value="form.bankAccount"
                  placeholder="请输入开户行账号"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>
        <!-- ═══ 证件信息（通用组件：营业执照固定，其余可选/自定义，始终保留一个待输入位） ═══ -->
        <FormSection>
          <CertUploadList
            ref="certRef"
            :partner-id="certPartnerId"
          />
        </FormSection>

      </a-form>
    </div>

    <!-- ═══ 底部按钮栏（对标：保存(Enter) / 取消(Esc)） ═══ -->
    <div class="form-footer">
      <a-space :size="12">
        <a-button
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          保存(Enter)
        </a-button>
        <a-button @click="handleCancel">
          取消(Esc)
        </a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { partnerApi, partnerContactApi } from '@/api/erp/partner'
import request from '@/utils/request'
import CertUploadList from '@/components/CertUploadList/CertUploadList.vue'
import FormSection from '@/components/FormSection/index.vue'

const route = useRoute()
const router = useRouter()

/** 物流公司编号前缀（对标 ql361 实测：WuLiu + 3 位序号，如 WuLiu001） */
const CODE_PREFIX = 'WuLiu'

const saving = ref(false)
let branchKeySeed = 0

interface BranchRow {
  _key: number
  id?: number
  contactName: string
  linkman: string
  phone: string
  detailAddress: string
}

const form = reactive({
  partnerCode: '',
  partnerName: '',
  mnemonicCode: '',
  shortName: '',
  remark: '',
  // 纳税人信息
  companyFullName: '',
  taxNumber: '',
  address: '',
  phone: '',
  bankAddress: '',
  bankAccount: '',
})

const branches = ref<BranchRow[]>([createBranch()])

const routeId = computed(() => {
  const id = route.params.id
  return id ? String(id) : ''
})
const isEdit = computed(() => !!routeId.value)
/** 证件组件绑定的往来单位 id（编辑态取路由 id；新增态保存后回填） */
const certPartnerId = ref<number | undefined>(undefined)
const certRef = ref<any>(null)
watch(routeId, (v) => { certPartnerId.value = v ? Number(v) : undefined }, { immediate: true })

function createBranch(data?: Partial<BranchRow>): BranchRow {
  branchKeySeed += 1
  return {
    _key: branchKeySeed,
    id: data?.id,
    contactName: data?.contactName || '',
    linkman: data?.linkman || '',
    phone: data?.phone || '',
    detailAddress: data?.detailAddress || '',
  }
}

function addBranch() {
  branches.value.push(createBranch())
}

function removeBranch(index: number) {
  branches.value.splice(index, 1)
}

// ═══ 编号生成（对标 WuLiu001 口径） ═══
async function generateCode() {
  try {
    const res: any = await request.get('/erp/md/customer/next-seq', { params: { prefix: CODE_PREFIX } })
    const seq = Number(res?.seq ?? res?.data?.seq ?? 1) || 1
    form.partnerCode = CODE_PREFIX + String(seq).padStart(3, '0')
  } catch {
    form.partnerCode = CODE_PREFIX + '001'
  }
}

// ═══ 编辑回填 ═══
async function loadDetail(id: string) {
  try {
    const detail: any = await partnerApi.getById(Number(id))
    if (!detail) {
      message.error('物流公司不存在')
      return
    }
    form.partnerCode = detail.partnerCode || ''
    form.partnerName = detail.partnerName || ''
    form.mnemonicCode = detail.mnemonicCode || ''
    form.shortName = detail.partnerShortName || ''
    form.remark = detail.remark || ''
    form.companyFullName = detail.companyFullName || ''
    form.taxNumber = detail.taxNumber || ''
    form.address = detail.address || ''
    form.phone = detail.phone || ''
    form.bankAddress = detail.bankAddress || ''
    form.bankAccount = detail.bankAccount || ''

    const contacts: any = await partnerContactApi.getByPartner(Number(id))
    const list: any[] = Array.isArray(contacts) ? contacts : []
    // 网点 = biz_party_contact 中该往来单位的记录（物流公司场景下联系人即网点）
    branches.value = list.length
      ? list.map(c => createBranch({
        id: c.id,
        contactName: c.contactName || '',
        linkman: c.linkman || '',
        phone: c.phone || '',
        detailAddress: c.detailAddress || '',
      }))
      : [createBranch()]
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载详情失败')
  }
}

// ═══ 保存 ═══
async function saveBranches(partyId: number, previousIds: number[]) {
  const validBranches = branches.value.filter(b =>
    (b.contactName || '').trim() || (b.linkman || '').trim() || (b.phone || '').trim() || (b.detailAddress || '').trim(),
  )
  const keepIds = new Set(validBranches.map(b => b.id).filter(Boolean) as number[])

  // 编辑场景：删除已被移除的网点
  for (const oldId of previousIds) {
    if (!keepIds.has(oldId)) {
      await partnerContactApi.delete(oldId).catch(() => {})
    }
  }

  for (let i = 0; i < validBranches.length; i++) {
    const branch = validBranches[i]
    const payload: any = {
      partyId,
      contactName: branch.contactName || '',
      linkman: branch.linkman || '',
      phone: branch.phone || '',
      detailAddress: branch.detailAddress || '',
      isPrimary: i === 0 ? 1 : 0,
      status: 1,
    }
    try {
      if (branch.id) {
        await partnerContactApi.update(branch.id, payload)
      } else {
        await partnerContactApi.create(payload)
      }
    } catch (error: any) {
      // 网点保存失败不阻断主档保存，但必须让用户知道
      message.warning(`网点「${branch.contactName || i + 1}」保存失败：${error?.response?.data?.message || error?.message || '未知错误'}`)
    }
  }
}

async function handleSave() {
  if (!(form.partnerName || '').trim()) {
    message.warning('请输入物流公司名称')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, any> = {
      partnerCode: form.partnerCode,
      partnerName: form.partnerName,
      partnerShortName: form.shortName,
      mnemonicCode: form.mnemonicCode,
      remark: form.remark,
      partnerType: 'LOGISTICS',
      status: 'ENABLED',
      companyFullName: form.companyFullName,
      taxNumber: form.taxNumber,
      address: form.address,
      phone: form.phone,
      bankAddress: form.bankAddress,
      bankAccount: form.bankAccount,
    }

    let previousIds: number[] = []
    let partyId: number

    if (isEdit.value) {
      partyId = Number(routeId.value)
      const contacts: any = await partnerContactApi.getByPartner(partyId).catch(() => [])
      previousIds = (Array.isArray(contacts) ? contacts : []).map((c: any) => c.id).filter(Boolean)
      await partnerApi.update(partyId, payload)
    } else {
      const created: any = await partnerApi.create(payload)
      partyId = Number(created?.id || 0)
      if (!partyId) {
        message.error('保存失败：未获取到新建记录 ID')
        return
      }
    }

    certPartnerId.value = partyId
    await certRef.value?.sync(partyId)
    await saveBranches(partyId, previousIds)
    message.success(isEdit.value ? '保存成功' : '新增成功')
    router.push('/md/logistics/index')
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  router.push('/md/logistics/index')
}

// ═══ 快捷键：保存(Enter) / 取消(Esc) ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    e.preventDefault()
    handleCancel()
    return
  }
  if (e.key !== 'Enter') return
  // 对标：保存(Enter) / 取消(Esc)。多行输入控件内回车仍是换行，不触发保存。
  const tag = (e.target as HTMLElement | null)?.tagName?.toLowerCase()
  const isMultiline = tag === 'textarea'
  if (isMultiline && !(e.ctrlKey || e.metaKey)) return
  e.preventDefault()
  handleSave()
}

onMounted(async () => {
  document.addEventListener('keydown', handleKeydown)
  if (isEdit.value) {
    await loadDetail(routeId.value)
  } else {
    await generateCode()
  }
})
onBeforeUnmount(() => document.removeEventListener('keydown', handleKeydown))
</script>

<style scoped>
.form-page-wrapper { display: flex; flex-direction: column; height: 100%; overflow: hidden; background: #f5f7fa; }
.page-header { padding: 10px 16px; flex-shrink: 0; background: #fff; border-bottom: 1px solid #e8e8e8; }
.page-title { font-size: 15px; font-weight: 600; color: #262626; }

.form-scroll-area { flex: 1; overflow-y: auto; padding: 12px 16px; min-height: 0; }

.btn-add-branch { padding: 0; }

.branch-block { border-bottom: 1px dashed #f0f0f0; margin-bottom: 8px; }
.branch-block:last-child { border-bottom: none; }
.branch-header { display: flex; align-items: center; justify-content: space-between; }
.branch-title { font-size: 13px; color: #8c8c8c; }

:deep(.ant-form-item) { margin-bottom: 12px; }
:deep(.ant-form-item-label > label) { font-size: 13px; color: #595959; }
:deep(.ant-form-item-required::before) { margin-right: 2px; }

.form-footer {
  background: #fff;
  padding: 12px 24px;
  text-align: right;
  flex-shrink: 0;
  border-top: 1px solid #e8e8e8;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
