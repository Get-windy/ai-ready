<template>
  <a-modal
    :open="open"
    :title="isEdit ? '会员卡（编辑）' : '会员卡'"
    :confirm-loading="saving"
    width="720px"
    :mask-closable="false"
    @ok="handleSave"
    @cancel="handleClose"
  >
    <a-form
      ref="formRef"
      :model="form"
      :rules="rules"
      layout="horizontal"
      :label-col="{ flex: '96px' }"
    >
      <div class="card-title">
        基本信息
      </div>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="会员名称">
            <a-input
              v-model:value="form.memberName"
              size="small"
              placeholder="请输入会员名称"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="会员卡号"
            name="memberCardNo"
          >
            <a-input
              v-model:value="form.memberCardNo"
              size="small"
              placeholder="请输入会员卡号"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系电话">
            <a-input
              v-model:value="form.phone"
              size="small"
              placeholder="请输入联系电话"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="客户"
            name="partyId"
          >
            <a-select
              v-model:value="form.partyId"
              size="small"
              show-search
              :filter-option="false"
              placeholder="请选择客户"
              :options="partyOptions"
              :loading="partyLoading"
              @search="searchParty"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="有效时间">
            <a-range-picker
              v-model:value="validRange"
              size="small"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="初始积分">
            <a-input-number
              v-model:value="form.memberInitialPoints"
              size="small"
              :min="0"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="会员级别"
            name="memberLevel"
          >
            <a-select
              v-model:value="form.memberLevel"
              size="small"
              placeholder="请选择会员级别"
              :options="levelOptions"
              :loading="levelLoading"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="会员卡状态">
            <a-select
              v-model:value="form.memberCardStatus"
              size="small"
              :options="cardStatusOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="会员生日">
            <a-date-picker
              v-model:value="form.birthday"
              size="small"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="备注">
            <a-input
              v-model:value="form.remark"
              size="small"
              placeholder="请输入备注"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="card-title">
        卡信息（只读）
      </div>
      <a-row :gutter="16">
        <a-col :span="6">
          <span class="ro-label">当前积分：</span><span class="ro-val">{{ form.points ?? 0 }}</span>
        </a-col>
        <a-col :span="6">
          <span class="ro-label">累计消费额：</span><span class="ro-val">{{ form.memberTotalConsume ?? 0 }}</span>
        </a-col>
        <a-col :span="6">
          <span class="ro-label">发卡时间：</span><span class="ro-val">{{ form.memberIssueTime || '-' }}</span>
        </a-col>
        <a-col :span="6">
          <span class="ro-label">最近交易：</span><span class="ro-val">{{ form.lastTradeTime || '-' }}</span>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
/**
 * 会员卡弹窗（对标：会员管理 → 新增 → 「会员卡」）
 *
 * 会员卡挂在**往来单位**上（"客户*"必选）：会员名称/卡号/级别/有效期/积分/生日，
 * 数据落在 biz_party 的 member_* 字段；会员级别来自独立的 erp_member_level（≠ 客户级别/价格等级）。
 */
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { partnerApi } from '@/api/erp/partner'
import request from '@/utils/request'

const props = defineProps<{
  open: boolean
  /** 编辑时传入会员行（含 partyId） */
  record?: Record<string, any> | null
}>()

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'saved'): void
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const isEdit = computed(() => !!(props.record && (props.record.partyId || props.record.id)))
const validRange = ref<[string, string] | undefined>(undefined)

const form = reactive<Record<string, any>>({
  partyId: undefined,
  memberName: '',
  memberCardNo: '',
  memberLevel: undefined,
  memberCardStatus: 'NORMAL',
  memberInitialPoints: 0,
  birthday: undefined,
  phone: '',
  remark: '',
  points: 0,
  memberTotalConsume: 0,
  memberIssueTime: '',
  lastTradeTime: '',
})

const rules = {
  partyId: [{ required: true, message: '请选择客户' }],
  memberCardNo: [{ required: true, message: '请输入会员卡号' }],
  memberLevel: [{ required: true, message: '请选择会员级别' }],
}

const cardStatusOptions = [
  { label: '正常', value: 'NORMAL' },
  { label: '停用', value: 'STOPPED' },
  { label: '已过期', value: 'EXPIRED' },
]

// ── 会员级别（独立字典） ──
const levelOptions = ref<Array<{ label: string; value: string }>>([])
const levelLoading = ref(false)
async function loadLevels() {
  levelLoading.value = true
  try {
    const list: any = await request.get('/erp/member-level/list')
    levelOptions.value = (list || []).map((l: any) => ({ label: l.levelName, value: l.levelName }))
  } catch {
    levelOptions.value = []
  } finally {
    levelLoading.value = false
  }
}

// ── 客户选择（往来单位，客户类型） ──
const partyOptions = ref<Array<{ label: string; value: any }>>([])
const partyLoading = ref(false)
async function searchParty(kw: string) {
  partyLoading.value = true
  try {
    const list: any = await partnerApi.search(kw || '', 'customer')
    partyOptions.value = (list || []).map((p: any) => ({ label: `${p.partnerName}${p.partnerCode ? ' (' + p.partnerCode + ')' : ''}`, value: p.id }))
  } catch {
    partyOptions.value = []
  } finally {
    partyLoading.value = false
  }
}

watch(() => props.open, async (v) => {
  if (!v) return
  loadLevels()
  partyOptions.value = []
  const r = props.record || {}
  const partyId = r.partyId || r.id
  if (partyId) {
    try {
      const d: any = await partnerApi.getById(partyId)
      Object.assign(form, {
        partyId: d.id,
        memberName: d.memberName || d.partnerName || '',
        memberCardNo: d.memberCardNo || '',
        memberLevel: d.memberLevel || undefined,
        memberCardStatus: d.memberCardStatus || 'NORMAL',
        memberInitialPoints: d.memberInitialPoints ?? 0,
        birthday: d.birthday || undefined,
        phone: d.phone || d.contactPhone || '',
        remark: d.remark || '',
        points: d.points ?? 0,
        memberTotalConsume: d.memberTotalConsume ?? 0,
        memberIssueTime: d.memberIssueTime || '',
        lastTradeTime: d.lastTradeTime || '',
      })
      validRange.value = (d.memberValidStart || d.memberValidEnd)
        ? [d.memberValidStart || '', d.memberValidEnd || '']
        : undefined
      partyOptions.value = [{ label: `${d.partnerName}${d.partnerCode ? ' (' + d.partnerCode + ')' : ''}`, value: d.id }]
    } catch {
      message.error('加载会员信息失败')
    }
  } else {
    Object.assign(form, {
      partyId: undefined, memberName: '', memberCardNo: '', memberLevel: undefined,
      memberCardStatus: 'NORMAL', memberInitialPoints: 0, birthday: undefined,
      phone: '', remark: '', points: 0, memberTotalConsume: 0, memberIssueTime: '', lastTradeTime: '',
    })
    validRange.value = undefined
  }
})

async function handleSave() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await partnerApi.update(form.partyId, {
      memberName: form.memberName || undefined,
      memberCardNo: form.memberCardNo,
      memberLevel: form.memberLevel,
      memberCardStatus: form.memberCardStatus,
      memberInitialPoints: form.memberInitialPoints ?? 0,
      memberValidStart: validRange.value?.[0] || undefined,
      memberValidEnd: validRange.value?.[1] || undefined,
      birthday: form.birthday || undefined,
      phone: form.phone || undefined,
      remark: form.remark || undefined,
      memberIssueTime: form.memberIssueTime || new Date().toISOString().slice(0, 10),
    })
    message.success('会员卡已保存')
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
}
.ro-label { color: #8c8c8c; font-size: 13px; }
.ro-val { color: #262626; font-size: 13px; font-weight: 500; }
</style>
