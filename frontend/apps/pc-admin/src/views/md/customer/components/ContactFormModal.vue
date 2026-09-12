<template>
  <a-modal
    :open="open"
    :title="isEdit ? '联系人（编辑）' : '联系人'"
    :confirm-loading="saving"
    width="880px"
    :mask-closable="false"
    @ok="handleSave"
    @cancel="handleClose"
  >
    <a-form
      ref="formRef"
      :model="form"
      :rules="rules"
      layout="horizontal"
      :label-col="{ flex: '78px' }"
    >
      <!-- 人（独立主数据） -->
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item
            label="联系人"
            name="contactName"
          >
            <a-input
              v-model:value="form.contactName"
              size="small"
              placeholder="请输入联系人姓名"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="性别">
            <a-select
              v-model:value="form.gender"
              size="small"
              allow-clear
              :options="[{ label: '男', value: '男' }, { label: '女', value: '女' }]"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item
            label="联系电话"
            name="mobile"
          >
            <a-input
              v-model:value="form.mobile"
              size="small"
              placeholder="请输入手机/电话"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="邮箱">
            <a-input
              v-model:value="form.email"
              size="small"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="微信">
            <a-input
              v-model:value="form.wechat"
              size="small"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="QQ">
            <a-input
              v-model:value="form.qq"
              size="small"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="生日">
            <a-date-picker
              v-model:value="form.birthday"
              size="small"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="16">
          <a-form-item label="备注">
            <a-input
              v-model:value="form.remark"
              size="small"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 服务的往来单位（多对多） -->
      <div class="card-title">
        服务的往来单位
        <a-button
          type="link"
          size="small"
          @click="addLink"
        >
          <PlusOutlined /> 添加
        </a-button>
      </div>
      <div
        v-for="(row, idx) in links"
        :key="row._uid"
        class="link-row"
      >
        <a-row :gutter="12">
          <a-col :span="8">
            <a-form-item :label="idx === 0 ? '往来单位' : ''">
              <a-select
                v-model:value="row.partyId"
                size="small"
                show-search
                :filter-option="false"
                placeholder="请选择往来单位"
                :options="partyOptions"
                :loading="partyLoading"
                @search="searchParty"
              />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item :label="idx === 0 ? '职务' : ''">
              <a-input
                v-model:value="row.position"
                size="small"
              />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item :label="idx === 0 ? '部门' : ''">
              <a-input
                v-model:value="row.department"
                size="small"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item :label="idx === 0 ? '主联系人' : ''">
              <a-checkbox
                :checked="row.isPrimary === 1"
                @change="(e: any) => row.isPrimary = e.target.checked ? 1 : 0"
              >
                设为主联系人
              </a-checkbox>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="8">
            <a-form-item :label="idx === 0 ? '联系地址' : ''">
              <a-input
                v-model:value="row.detailAddress"
                size="small"
              />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item :label="idx === 0 ? '配送方式' : ''">
              <a-select
                v-model:value="row.deliveryMethod"
                size="small"
                allow-clear
                :options="deliveryOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item :label="idx === 0 ? '配送线路' : ''">
              <a-input
                v-model:value="row.deliveryRoute"
                size="small"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6" class="link-actions">
            <a-button
              type="link"
              size="small"
              danger
              @click="removeLink(row)"
            >
              <DeleteOutlined /> 移除
            </a-button>
          </a-col>
        </a-row>
      </div>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
/**
 * 联系人弹窗（对标：全部联系人 → 新增 → 「联系人」）
 *
 * 业务模型：联系人是**与往来单位平行**的独立实体，通过 biz_party_contact 多对多关联；
 * 一个联系人可服务多个往来单位（每段关系有自己的主联系人标记 / 职务 / 配送信息）。
 */
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { partnerApi } from '@/api/erp/partner'
import request from '@/utils/request'

const props = defineProps<{
  open: boolean
  /** 编辑时传入行（含 contactId 或 id） */
  record?: Record<string, any> | null
  /** 从某个往来单位新增时预填 */
  presetPartyId?: number | string | null
}>()

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'saved'): void
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
let uid = 0

const form = reactive<Record<string, any>>({
  id: undefined,
  contactName: '',
  gender: undefined,
  mobile: '',
  email: '',
  wechat: '',
  qq: '',
  birthday: undefined,
  remark: '',
})

interface LinkRow {
  _uid: number
  relId?: number
  partyId?: any
  isPrimary: number
  position?: string
  department?: string
  detailAddress?: string
  deliveryMethod?: string
  deliveryRoute?: string
}

const links = ref<LinkRow[]>([])
const removedRelIds = ref<number[]>([])

const isEdit = computed(() => !!form.id)

const rules = {
  contactName: [{ required: true, message: '请输入联系人姓名' }],
  mobile: [{ required: true, message: '请输入联系电话' }],
}

const deliveryOptions = [
  { label: '自配', value: '自配' },
  { label: '物流', value: '物流' },
  { label: '自提', value: '自提' },
]

function emptyLink(partyId?: any): LinkRow {
  return { _uid: ++uid, partyId, isPrimary: 0 }
}

function addLink() {
  links.value.push(emptyLink())
}

function removeLink(row: LinkRow) {
  const i = links.value.findIndex(l => l._uid === row._uid)
  if (i >= 0) links.value.splice(i, 1)
  if (row.relId) removedRelIds.value.push(row.relId)
}

// ── 往来单位搜索 ──
const partyOptions = ref<Array<{ label: string; value: any }>>([])
const partyLoading = ref(false)
async function searchParty(kw: string) {
  partyLoading.value = true
  try {
    const list: any = await partnerApi.search(kw || '')
    const opts = (list || []).map((p: any) => ({ label: `${p.partnerName}${p.partnerCode ? ' (' + p.partnerCode + ')' : ''}`, value: p.id }))
    // 合并已选（避免回显丢失）
    for (const l of links.value) {
      if (l.partyId && !opts.some((o: any) => String(o.value) === String(l.partyId)) && l.partyName) {
        opts.push({ label: l.partyName, value: l.partyId })
      }
    }
    partyOptions.value = opts
  } catch {
    partyOptions.value = []
  } finally {
    partyLoading.value = false
  }
}

watch(() => props.open, async (v) => {
  if (!v) return
  removedRelIds.value = []
  const r = props.record || {}
  const cid = r.contactId || (r.isContactPage ? r.id : undefined)
  Object.assign(form, {
    id: cid || undefined,
    contactName: r.contactName || '',
    gender: r.gender || undefined,
    mobile: r.mobile || r.contactPhone || '',
    email: r.email || '',
    wechat: r.wechat || '',
    qq: r.qq || '',
    birthday: r.birthday || undefined,
    remark: r.remark || '',
  })
  links.value = []
  partyOptions.value = []

  if (cid) {
    // 编辑：补齐人的档案 + 已关联的往来单位
    try {
      const d: any = await request.get(`/erp/contact/${cid}`)
      if (d) Object.assign(form, { contactName: d.contactName || form.contactName, gender: d.gender || form.gender, mobile: d.mobile || form.mobile, email: d.email || '', wechat: d.wechat || '', qq: d.qq || '', birthday: d.birthday || undefined, remark: d.remark || '' })
    } catch { /* 兼容仅有 relId 的历史行 */ }
    try {
      const rels: any = await request.get(`/erp/contact/${cid}/parties`)
      links.value = (rels || []).map((x: any) => ({
        _uid: ++uid, relId: x.relId, partyId: x.partyId, partyName: x.partyName,
        isPrimary: x.isPrimary || 0, position: x.position, department: x.department,
        detailAddress: x.detailAddress, deliveryMethod: x.deliveryMethod, deliveryRoute: x.deliveryRoute,
      }))
      partyOptions.value = links.value.filter(l => l.partyId).map(l => ({ label: l.partyName || String(l.partyId), value: l.partyId }))
    } catch { /* ignore */ }
  } else if (props.presetPartyId) {
    links.value = [emptyLink(props.presetPartyId)]
    partyOptions.value = r.partnerName ? [{ label: r.partnerName, value: props.presetPartyId }] : []
  }
  if (links.value.length === 0) links.value = [emptyLink(props.presetPartyId || undefined)]
  searchParty('')
})

async function handleSave() {
  try { await formRef.value?.validate() } catch { return }
  const valid = links.value.filter(l => l.partyId)
  if (valid.length === 0) {
    message.warning('请至少关联一个往来单位')
    return
  }
  saving.value = true
  try {
    // 1) upsert 人
    const person = {
      contactName: form.contactName,
      gender: form.gender || undefined,
      mobile: form.mobile,
      email: form.email || undefined,
      wechat: form.wechat || undefined,
      qq: form.qq || undefined,
      birthday: form.birthday || undefined,
      remark: form.remark || undefined,
      status: 1,
    }
    let contactId = form.id
    if (contactId) {
      await request.put(`/erp/contact/${contactId}`, person)
    } else {
      const created: any = await request.post('/erp/contact', person)
      contactId = created?.id
      form.id = contactId
    }
    if (!contactId) throw new Error('联系人保存失败')

    // 2) 解除已移除的关联
    for (const relId of removedRelIds.value) {
      await request.delete(`/erp/contact/${contactId}/parties/${relId}`)
    }
    // 3) 绑定/更新关联
    for (const l of valid) {
      await request.post(`/erp/contact/${contactId}/parties`, {
        partyId: l.partyId,
        isPrimary: l.isPrimary,
        position: l.position ?? undefined,
        department: l.department ?? undefined,
        detailAddress: l.detailAddress ?? undefined,
        deliveryMethod: l.deliveryMethod ?? undefined,
        deliveryRoute: l.deliveryRoute ?? undefined,
      })
    }
    message.success('联系人已保存')
    emit('saved')
    emit('update:open', false)
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleClose() {
  emit('update:open', false)
}
</script>

<style scoped>
.card-title {
  font-size: 13px;
  font-weight: 600;
  color: #262626;
  margin: 4px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.link-row {
  padding: 8px 0 0;
  border-top: 1px dashed #f0f0f0;
}
.link-actions { text-align: right; }
</style>
