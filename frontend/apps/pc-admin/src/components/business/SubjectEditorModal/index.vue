<template>
  <!--
    会计科目编辑器（共享组件）
    使用方：《资料 → 财务账户 → 会计科目》《费用类型》《其他收入》
    （对标 ql361：费用类型/其他收入页行内「修改」打开的就是本「会计科目」编辑器）
    对标 ql361 编辑弹窗字段：科目编号* / 科目名称* / 助记码 / 科目全名 / 核算项 / 借-贷（余额方向）
    按钮：保存(Enter) / 关闭(Esc)
    · api 缺省走 finance/subject（会计科目页）；费用类型等视图页可传入自己的菜单路径 API
  -->
  <a-modal
    :open="open"
    :title="isEditing ? '会计科目' : (title || '新增会计科目')"
    :confirm-loading="submitting"
    :mask-closable="false"
    width="560px"
    ok-text="保存(Enter)"
    cancel-text="关闭(Esc)"
    @ok="handleSubmit"
    @cancel="handleClose"
    @update:open="(v: boolean) => emit('update:open', v)"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="formRules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <a-form-item
        label="科目编号"
        name="subjectCode"
      >
        <a-input
          v-model:value="formState.subjectCode"
          placeholder="如 1001"
          size="small"
          @press-enter="handleSubmit"
        />
      </a-form-item>
      <a-form-item
        label="科目名称"
        name="subjectName"
      >
        <a-input
          v-model:value="formState.subjectName"
          placeholder="如 库存现金"
          size="small"
          @press-enter="handleSubmit"
        />
      </a-form-item>
      <a-form-item label="助记码">
        <a-input
          v-model:value="formState.mnemonicCode"
          placeholder="科目名称拼音首字母，如 KCXJ"
          size="small"
          @press-enter="handleSubmit"
        />
      </a-form-item>
      <a-form-item label="科目全名">
        <a-input
          v-model:value="formState.fullName"
          :placeholder="autoFullName || '留空则按上级科目自动拼装'"
          size="small"
          @press-enter="handleSubmit"
        />
      </a-form-item>
      <a-form-item label="核算项">
        <a-select
          v-model:value="formState.auxiliaryTypeId"
          placeholder="不核算"
          size="small"
          allow-clear
          :options="auxTypeOptions"
        />
      </a-form-item>
      <a-form-item
        label="借/贷"
        name="direction"
      >
        <a-radio-group v-model:value="formState.direction">
          <a-radio :value="1">
            借方
          </a-radio>
          <a-radio :value="2">
            贷方
          </a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="状态">
        <a-switch
          v-model:checked="formState.isEnabled"
          checked-children="启用"
          un-checked-children="停用"
          size="small"
        />
      </a-form-item>
      <a-form-item
        v-if="formState.parentName"
        label="上级科目"
      >
        <a-input
          :value="formState.parentName"
          disabled
          size="small"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { accountSubjectApi } from '@/api/finance'

defineOptions({ name: 'SubjectEditorModal' })

const props = withDefaults(defineProps<{
  /** 弹窗显隐（v-model:open） */
  open: boolean
  /** 编辑对象（含 id 即为编辑态） */
  record?: any | null
  /** 新增时的上级科目 */
  parent?: any | null
  /** 新增时的预置科目分类（1-资产 2-负债 3-权益 4-成本 5-损益） */
  defaultSubjectType?: number
  /** 新增时的预置余额方向（1-借 2-贷） */
  defaultDirection?: number
  /** 新增态弹窗标题 */
  title?: string
  /** 科目 API（缺省 accountSubjectApi；费用类型等视图页传入各自菜单路径 API） */
  api?: any
}>(), {
  record: null,
  parent: null,
  defaultSubjectType: undefined,
  defaultDirection: 1,
  title: '新增会计科目',
  api: undefined,
})

/** 实际使用的科目 API：视图页可覆盖，缺省为会计科目接口 */
const subjectApi = computed(() => props.api || accountSubjectApi)

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'saved'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)

const formState = reactive({
  subjectCode: '',
  subjectName: '',
  mnemonicCode: '',
  fullName: '',
  auxiliaryTypeId: undefined as number | undefined,
  direction: 1,
  isEnabled: true,
  subjectType: undefined as number | undefined,
  parentId: undefined as number | undefined,
  parentName: '',
})

const formRules = {
  subjectCode: [{ required: true, message: '请输入科目编号', trigger: 'blur' }],
  subjectName: [{ required: true, message: '请输入科目名称', trigger: 'blur' }],
  direction: [{ required: true, message: '请选择借/贷方向', trigger: 'change' }],
} as any

/** 科目全名自动拼装预览（有上级科目时） */
const autoFullName = computed(() => {
  if (!formState.subjectName) return ''
  return formState.parentName ? `${formState.parentName}/${formState.subjectName}` : formState.subjectName
})

// ── 核算项（辅助核算类型，复用 finance_auxiliary_type 主数据，不另建字典） ──
const auxTypeOptions = ref<{ label: string; value: number }[]>([])

async function fetchAuxTypes() {
  try {
    const res: any = await subjectApi.value.getAuxTypes()
    const list = res?.data ?? res ?? []
    auxTypeOptions.value = (Array.isArray(list) ? list : []).map((t: any) => ({
      label: t.typeName, value: t.id,
    }))
  } catch (e) {
    console.warn('[会计科目] 核算项加载失败', e)
    auxTypeOptions.value = []
  }
}

function resetForm() {
  Object.assign(formState, {
    subjectCode: '', subjectName: '', mnemonicCode: '', fullName: '',
    auxiliaryTypeId: undefined, direction: props.defaultDirection ?? 1, isEnabled: true,
    subjectType: undefined, parentId: undefined, parentName: '',
  })
  editingId.value = null
  isEditing.value = false
  formRef.value?.clearValidate()
}

function initForm() {
  resetForm()
  const record = props.record
  if (record && record.id) {
    isEditing.value = true
    editingId.value = record.id
    Object.assign(formState, {
      subjectCode: record.subjectCode,
      subjectName: record.subjectName,
      mnemonicCode: record.mnemonicCode || '',
      fullName: record.fullName || '',
      auxiliaryTypeId: record.auxiliaryTypeId ?? undefined,
      direction: record.direction ?? 1,
      isEnabled: record.isEnabled !== false,
      subjectType: record.subjectType,
      parentId: record.parentId ?? undefined,
      parentName: record.parentName ? `${record.parentCode || ''} ${record.parentName}`.trim() : '',
    })
  } else {
    formState.subjectType = props.defaultSubjectType
    if (props.parent) {
      formState.parentId = props.parent.id
      formState.parentName = `${props.parent.subjectCode} ${props.parent.subjectName}`
      formState.subjectType = props.parent.subjectType
      formState.direction = props.parent.direction ?? formState.direction
    }
  }
  fetchAuxTypes()
}

watch(() => props.open, (visible) => {
  if (visible) initForm()
})

function handleClose() {
  emit('update:open', false)
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload: any = {
      subjectCode: formState.subjectCode,
      subjectName: formState.subjectName,
      mnemonicCode: formState.mnemonicCode,
      fullName: formState.fullName,
      auxiliaryTypeId: formState.auxiliaryTypeId ?? null,
      direction: formState.direction,
      isEnabled: formState.isEnabled,
      subjectType: formState.subjectType,
    }
    if (formState.parentId !== undefined) payload.parentId = formState.parentId

    if (isEditing.value && editingId.value) {
      await subjectApi.value.update(editingId.value, payload)
      message.success('科目已更新')
    } else {
      await subjectApi.value.create(payload)
      message.success('科目已新增')
    }
    emit('update:open', false)
    emit('saved')
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>
