/**
 * useBillForm — BillFormPage 通用表单逻辑 composable
 *
 * 从 OrderFormPage/useFormLogic 提取，供所有使用 BillFormPage 的单据页面复用。
 * 提供：表单数据管理、下拉选项加载、明细行增删、验证、提交、快捷键。
 */
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import optionsApi from '@/api/options'
import { generateCodeDemo, generateCodeAsync } from '@/utils/codeGenerator'

// ── 类型 ──

export type BillField = {
  key: string
  label: string
  type: 'select' | 'input' | 'date' | 'number'
  required?: boolean
  optionsRef?: 'customers' | 'suppliers' | 'warehouses' | 'users' | 'products'
  options?: { label: string; value: string | number }[]
}

export interface BillFormApi {
  create: (data: any) => Promise<any>
  update?: (id: number, data: any) => Promise<any>
  getById?: (id: number) => Promise<any>
}

export interface UseBillFormOptions {
  /** 单据编号前缀（如 'XSDD-', 'PO', 'SH'） */
  billPrefix?: string
  /** CRUD API */
  api: BillFormApi
  /** 提交后跳转路径 */
  redirectPath?: string
  /** 基本信息字段配置（用于初始化和验证） */
  fields?: BillField[]
  /** 显式模式（不指定则从路由推断） */
  mode?: 'create' | 'edit' | 'view'
  /** 需要加载的下拉选项类型（不指定则全部加载） */
  optionTypes?: ('customers' | 'suppliers' | 'warehouses' | 'users' | 'products')[]
  /** 明细行默认字段模板 */
  productDefaults?: Record<string, any>
  /** 基本信息字段变更回调 */
  onFieldChange?: (fieldKey: string, value: any, formData: Record<string, any>) => void
  /** 自定义提交数据转换 */
  transformPayload?: (formData: Record<string, any>, status: number) => any
  /** 后端序号接口路径（传入则异步获取真实序号，否则用随机演示序号） */
  codeApiPath?: string
}

// ── Composable ──

export function useBillForm(options: UseBillFormOptions) {
  const {
    billPrefix = 'NO',
    api,
    redirectPath,
    fields = [],
    mode: explicitMode,
    optionTypes,
    productDefaults = {},
    onFieldChange,
    transformPayload,
    codeApiPath,
  } = options

  const router = useRouter()
  const route = useRoute()

  // ── 表单数据 ──
  const formData = reactive<Record<string, any>>({
    id: undefined,
    orderNo: '',
    date: '',
    products: [] as any[],
  })

  // 初始化字段默认值
  for (const field of fields) {
    if (!(field.key in formData)) {
      formData[field.key] = field.type === 'number' ? 0 : undefined
    }
  }

  const loadingOptions = ref(false)
  const saving = ref(false)

  // ── 下拉选项 ──
  const optionRefs = reactive<Record<string, any[]>>({
    customers: [],
    suppliers: [],
    warehouses: [],
    users: [],
    products: [],
  })

  const filterOption = (input: string, option: any) => {
    const text = option?.label || option?.name || option?.children?.[0]?.children || ''
    return text.toString().toLowerCase().includes(input.toLowerCase())
  }

  // ── 模式推断 ──
  const effectiveMode = computed(() => {
    if (explicitMode) return explicitMode
    const editId = route.params.id || route.query.id
    return editId ? 'edit' : 'create'
  })

  // ── 单据编号（使用系统级 codeGenerator）──
  async function generateBillNo() {
    if (codeApiPath) {
      formData.orderNo = await generateCodeAsync(billPrefix, codeApiPath)
    } else {
      formData.orderNo = generateCodeDemo(billPrefix)
    }
  }

  // ── 加载下拉选项 ──
  async function loadOptions() {
    loadingOptions.value = true
    const types = optionTypes || ['customers', 'warehouses', 'users', 'products']
    try {
      const loaders: Record<string, () => Promise<any>> = {
        customers: () => optionsApi.getCustomers().catch(() => []),
        suppliers: () => optionsApi.getSuppliers?.().catch(() => []),
        warehouses: () => optionsApi.getWarehouses().catch(() => []),
        users: () => optionsApi.getUsers().catch(() => []),
        products: () => optionsApi.getProducts().catch(() => []),
      }
      const results = await Promise.all(types.map(t => loaders[t]?.() ?? Promise.resolve([])))
      types.forEach((t, i) => { optionRefs[t] = results[i] || [] })
    } finally {
      loadingOptions.value = false
    }
  }

  // ── 编辑模式：加载详情 ──
  async function loadDetail(id: number) {
    if (!api.getById) return
    try {
      const data = await api.getById(id)
      if (data) {
        Object.assign(formData, data)
        // 兼容后端返回 items 或 details 字段
        const rawItems = data.items || data.details || []
        if (rawItems.length) {
          formData.products = rawItems.map((d: any, i: number) => ({
            ...d,
            // 标准化后端字段名为前端字段名
            specification: d.specification || d.productSpec || '',
            unit: d.unit || d.productUnit || '',
            quantity: d.quantity ?? d.outboundQuantity ?? d.returnQuantity ?? d.inboundQuantity ?? 0,
            id: d.id || `detail-${i}`,
          }))
        }
      }
    } catch (err: any) {
      message.error('加载详情失败: ' + (err?.message || ''))
    }
  }

  // ── 明细行操作 ──
  function handleAddProduct() {
    formData.products.push({
      id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
      productId: undefined,
      productCode: '',
      productName: '',
      specification: '',
      quantity: 0,
      unit: '',
      unitPrice: 0,
      taxRate: 13,
      remark: '',
      ...productDefaults,
    })
  }

  function handleRemoveProduct(index: number) {
    formData.products.splice(index, 1)
  }

  function handleProductChange(val: number, index: number) {
    const p = optionRefs.products.find((x: any) => x.id === val)
    if (p && formData.products[index]) {
      const row = formData.products[index]
      row.productCode = p.code || ''
      row.productName = p.name || ''
      row.specification = p.specification || ''
      row.unit = p.unit || ''
      row.unitPrice = p.salePrice || p.price || 0
    }
  }

  // ── 字段变更回调 ──
  function handleFieldChange(fieldKey: string, val: any) {
    onFieldChange?.(fieldKey, val, formData)
  }

  // ── 验证 ──
  function validate(): boolean {
    for (const field of fields) {
      if (field.required && !formData[field.key]) {
        message.warning(`请填写${field.label}`)
        return false
      }
    }
    if (formData.products.length === 0) {
      message.warning('请添加明细')
      return false
    }
    return true
  }

  // ── 构建提交数据 ──
  function buildPayload(status: number) {
    if (transformPayload) return transformPayload(formData, status)
    return {
      ...formData,
      status,
      details: formData.products.map((p: any) => ({
        productId: p.productId,
        productCode: p.productCode,
        productName: p.productName,
        specification: p.specification,
        quantity: p.quantity,
        unit: p.unit,
        unitPrice: p.unitPrice,
        amount: (p.quantity || 0) * (p.unitPrice || 0),
        taxRate: p.taxRate,
        taxAmount: (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100,
        totalAmount: (p.quantity || 0) * (p.unitPrice || 0) * (1 + (p.taxRate || 0) / 100),
        remark: p.remark,
      })),
    }
  }

  // ── 提交 ──
  async function doSubmit(status: number) {
    if (!validate()) return
    saving.value = true
    try {
      const payload = buildPayload(status)
      const editId = route.params.id || route.query.id
      if (effectiveMode.value === 'edit' && editId && api.update) {
        await api.update(Number(editId), payload)
      } else {
        await api.create(payload)
      }
      message.success(status === 0 ? '保存草稿成功' : '提交成功')
      if (redirectPath) router.push(redirectPath)
    } catch (err: any) {
      message.error(err?.message || '操作失败')
    } finally {
      saving.value = false
    }
  }

  function handleSaveDraft() { doSubmit(0) }
  function handleSubmit() { doSubmit(1) }

  // ── 快捷键 ──
  function handleKeydown(e: KeyboardEvent) {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); handleSaveDraft() }
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSubmit() }
  }

  // ── 计算属性 ──
  const totalQuantity = computed(() => formData.products.reduce((s: number, p: any) => s + (p.quantity || 0), 0))
  const totalAmount = computed(() => formData.products.reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0), 0))
  const totalTaxAmount = computed(() => formData.products.reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100, 0))
  const totalWithTax = computed(() => formData.products.reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * (1 + (p.taxRate || 0) / 100), 0))
  const totalWithTaxFormatted = computed(() => `¥${totalWithTax.value.toFixed(2)}`)

  // ── 生命周期 ──
  onMounted(() => {
    generateBillNo()  // async, fires and forgets
    loadOptions()
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
    loadingOptions,
    saving,
    optionRefs,
    filterOption,
    effectiveMode,
    handleAddProduct,
    handleRemoveProduct,
    handleProductChange,
    handleFieldChange,
    handleSaveDraft,
    handleSubmit,
    // 计算
    totalQuantity,
    totalAmount,
    totalTaxAmount,
    totalWithTax,
    totalWithTaxFormatted,
  }
}
