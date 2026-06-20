/**
 * OrderFormPage 通用单据全屏表单 - 表单逻辑 composable
 */
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import optionsApi from '@/api/options'
import type { FormApi, HeaderField, FormMode } from './types'

export interface UseFormLogicOptions {
  /** 单据编号前缀 */
  billPrefix?: string
  /** CRUD API */
  api: FormApi
  /** 提交后跳转路径 */
  redirectPath?: string
  /** 表单模式 */
  mode?: FormMode
  /** 头部字段配置（用于构建 formData 初始值） */
  headerFields: HeaderField[]
}

export function useFormLogic(options: UseFormLogicOptions) {
  const { billPrefix = 'NO', api, redirectPath, mode = 'create', headerFields } = options
  const router = useRouter()
  const route = useRoute()

  // 表单数据
  const formData = reactive<Record<string, any>>({
    id: undefined,
    orderNo: '',
    date: '',
    products: [] as any[],
  })

  // 初始化字段默认值
  for (const field of headerFields) {
    if (!(field.name in formData)) {
      formData[field.name] = field.type === 'number' ? 0 : undefined
    }
  }

  const activeTab = ref('')
  const loadingOptions = ref(false)
  const saving = ref(false)

  // 下拉选项
  const optionRefs = reactive<Record<string, any[]>>({
    customers: [],
    suppliers: [],
    warehouses: [],
    users: [],
    products: [],
  })

  // 过滤函数
  const filterOption = (input: string, option: any) => {
    const text = option?.label || option?.name || option?.children?.[0]?.children || ''
    return text.toString().toLowerCase().includes(input.toLowerCase())
  }

  // 生成单据编号
  function generateBillNo() {
    const date = new Date()
    const dateStr = date.toISOString().slice(0, 10).replace(/-/g, '')
    const random = Math.random().toString(36).substring(2, 8).toUpperCase()
    formData.orderNo = `${billPrefix}${dateStr}${random}`
  }

  // 加载下拉选项
  async function loadOptions() {
    loadingOptions.value = true
    try {
      const [customers, warehouses, users, products] = await Promise.all([
        optionsApi.getCustomers().catch(() => []),
        optionsApi.getWarehouses().catch(() => []),
        optionsApi.getUsers().catch(() => []),
        optionsApi.getProducts().catch(() => []),
      ])
      optionRefs.customers = customers || []
      optionRefs.warehouses = warehouses || []
      optionRefs.users = users || []
      optionRefs.products = products || []
    } catch (err) {
      console.error('加载选项失败:', err)
    } finally {
      loadingOptions.value = false
    }
  }

  // 编辑模式：加载详情数据
  async function loadDetail(id: number) {
    if (!api.getById) return
    try {
      const data = await api.getById(id)
      if (data) {
        Object.assign(formData, data)
        if (data.details) {
          formData.products = data.details.map((d: any, i: number) => ({
            ...d,
            id: d.id || `detail-${i}`,
          }))
        }
      }
    } catch (err: any) {
      message.error('加载详情失败: ' + (err?.message || ''))
    }
  }

  // 添加明细行
  function handleAddProduct() {
    formData.products.push({
      id: Date.now().toString(),
      productId: undefined,
      productCode: '',
      productName: '',
      specification: '',
      quantity: 0,
      unit: '',
      unitPrice: 0,
      taxRate: 13,
      remark: '',
    })
  }

  // 删除明细行
  function handleRemoveProduct(index: number) {
    formData.products.splice(index, 1)
  }

  // 商品选择变更
  function handleProductChange(val: number, index: number) {
    const product = optionRefs.products.find((p: any) => p.id === val)
    if (product && formData.products[index]) {
      formData.products[index].productCode = product.code || ''
      formData.products[index].productName = product.name || ''
      formData.products[index].specification = product.specification || ''
      formData.products[index].unit = product.unit || ''
      formData.products[index].unitPrice = product.salePrice || product.price || 0
    }
  }

  // 表单验证
  function validate(): boolean {
    for (const field of headerFields) {
      if (field.required && !formData[field.name]) {
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

  // 构建提交数据
  function buildPayload(status: number) {
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

  // 保存草稿
  async function handleSaveDraft() {
    if (!validate()) return
    saving.value = true
    try {
      const payload = buildPayload(0)
      if (mode === 'edit' && formData.id && api.update) {
        await api.update(formData.id, payload)
      } else {
        await api.create(payload)
      }
      message.success('保存草稿成功')
      if (redirectPath) router.push(redirectPath)
    } catch (err: any) {
      message.error(err?.message || '保存失败')
    } finally {
      saving.value = false
    }
  }

  // 提交
  async function handleSubmit() {
    if (!validate()) return
    saving.value = true
    try {
      const payload = buildPayload(1)
      if (mode === 'edit' && formData.id && api.update) {
        await api.update(formData.id, payload)
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

  // 键盘快捷键
  function handleKeydown(e: KeyboardEvent) {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault()
      handleSaveDraft()
    }
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault()
      handleSubmit()
    }
  }

  onMounted(() => {
    generateBillNo()
    loadOptions()
    document.addEventListener('keydown', handleKeydown)

    // 编辑模式：加载详情
    if (mode === 'edit' && route.params.id) {
      loadDetail(Number(route.params.id))
    }
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown)
  })

  return {
    formData,
    activeTab,
    loadingOptions,
    saving,
    optionRefs,
    filterOption,
    handleAddProduct,
    handleRemoveProduct,
    handleProductChange,
    handleSaveDraft,
    handleSubmit,
  }
}
