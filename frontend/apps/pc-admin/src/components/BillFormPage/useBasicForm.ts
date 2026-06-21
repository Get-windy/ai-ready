/**
 * useBasicForm — BillFormPage 基础信息表单 composable
 *
 * 与 useBillForm（单据类，含明细行）配套，用于 CRM/Finance/DMS 等纯基础信息表单。
 * 提供：表单数据管理、验证、保存/提交、快捷键、编辑模式数据加载。
 */
import { reactive, ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import type { BasicInfoField } from './types'

// ── 类型 ──

export interface BasicFormApi {
  create: (data: any) => Promise<any>
  update?: (id: number, data: any) => Promise<any>
  getById?: (id: number) => Promise<any>
}

export interface UseBasicFormOptions {
  /** CRUD API */
  api: BasicFormApi
  /** 提交后跳转路径 */
  redirectPath?: string
  /** 字段配置 */
  fields?: BasicInfoField[]
  /** 显式模式（不指定则从路由推断） */
  mode?: 'create' | 'edit' | 'view'
}

// ── Composable ──

export function useBasicForm(options: UseBasicFormOptions) {
  const {
    api,
    redirectPath,
    fields = [],
    mode: explicitMode,
  } = options

  const router = useRouter()
  const route = useRoute()

  const saving = ref(false)

  // ── 表单数据 ──
  const formData = reactive<Record<string, any>>({})

  // 初始化字段默认值
  for (const field of fields) {
    if (!(field.key in formData)) {
      if (field.type === 'number') formData[field.key] = 0
      else formData[field.key] = undefined
    }
  }

  // ── 模式推断 ──
  const effectiveMode = computed(() => {
    if (explicitMode) return explicitMode
    const editId = route.params.id || route.query.id
    return editId ? 'edit' : 'create'
  })

  // ── 编辑模式：加载详情 ──
  async function loadDetail(id: number) {
    if (!api.getById) return
    try {
      const data = await api.getById(id)
      if (data) Object.assign(formData, data)
    } catch (err: any) {
      message.error('加载详情失败: ' + (err?.message || ''))
    }
  }

  // ── 验证 ──
  function validate(): boolean {
    for (const field of fields) {
      if (field.required) {
        const val = formData[field.key]
        if (val === undefined || val === null || val === '') {
          message.warning(`请填写${field.label}`)
          return false
        }
      }
    }
    return true
  }

  // ── 保存（草稿） ──
  async function handleSave() {
    if (!validate()) return
    saving.value = true
    try {
      const editId = route.params.id || route.query.id
      if (effectiveMode.value === 'edit' && editId && api.update) {
        await api.update(Number(editId), { ...formData })
      } else {
        await api.create({ ...formData })
      }
      message.success('保存成功')
      if (redirectPath) router.push(redirectPath)
    } catch (err: any) {
      message.error(err?.message || '保存失败')
    } finally {
      saving.value = false
    }
  }

  // ── 提交 ──
  async function handleSubmit() {
    if (!validate()) return
    saving.value = true
    try {
      const payload = { ...formData, status: 1 }
      const editId = route.params.id || route.query.id
      if (effectiveMode.value === 'edit' && editId && api.update) {
        await api.update(Number(editId), payload)
      } else {
        await api.create(payload)
      }
      message.success('提交成功')
      if (redirectPath) router.push(redirectPath)
    } catch (err: any) {
      message.error(err?.message || '提交失败')
    } finally {
      saving.value = false
    }
  }

  // ── 快捷键 ──
  function handleKeydown(e: KeyboardEvent) {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); handleSave() }
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSubmit() }
  }

  // ── 生命周期 ──
  onMounted(() => {
    document.addEventListener('keydown', handleKeydown)
    const editId = route.params.id || route.query.id
    if (editId && effectiveMode.value === 'edit') {
      loadDetail(Number(editId))
    }
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown)
  })

  return {
    formData,
    saving,
    effectiveMode,
    fields: ref(fields),
    handleSave,
    handleSubmit,
  }
}
