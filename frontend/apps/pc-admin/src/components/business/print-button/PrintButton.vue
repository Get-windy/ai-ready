<template>
  <a-tooltip :title="tooltip">
    <a-button
      :type="buttonType"
      :size="buttonSize"
      :loading="printing"
      :disabled="disabled || printing"
      @click="handlePrint"
    >
      <template #icon>
        <PrinterOutlined />
      </template>
      {{ buttonText }}
    </a-button>
  </a-tooltip>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { PrinterOutlined } from '@ant-design/icons-vue'
import { useRoute } from 'vue-router'
import { printingApi } from '@/api/printing'

interface Props {
  /** 打印链 ID（优先级最高，直接提交任务） */
  chainId?: number
  /** 页面编码，用于查找打印链 */
  pageCode?: string
  /** 业务记录 ID */
  businessId?: number | string
  /** 业务类型 */
  businessType?: string
  /** 模板类型（兼容旧的 prop 名） */
  templateType?: string
  /** 业务数据对象（优先级高于 businessId，避免额外请求） */
  record?: Record<string, any>
  /** 按钮文字 */
  buttonText?: string
  /** 按钮大小 */
  buttonSize?: 'small' | 'middle' | 'large'
  /** 按钮类型 */
  buttonType?: 'primary' | 'default' | 'link'
  /** 是否禁用 */
  disabled?: boolean
  /** 悬浮提示 */
  tooltip?: string
}

const props = withDefaults(defineProps<Props>(), {
  buttonText: '打印',
  buttonSize: 'middle',
  buttonType: 'primary',
  disabled: false,
  tooltip: '',
})

const emit = defineEmits<{
  'print-success': [result: any]
  'print-error': [error: Error]
}>()

const route = useRoute()
const printing = ref(false)

/** 获取页面编码 */
function getPageCode(): string {
  if (props.pageCode) return props.pageCode
  // 从当前路由路径推断 pageCode
  const path = route.path.replace(/^\/+/, '')
  // 移除动态参数段（如 /:id）
  return path.replace(/\/\d+/g, '').replace(/\/:[^/]+/g, '')
}

/** 获取业务数据（优先使用 record prop，其次通过 businessId 查找） */
async function getBusinessData(): Promise<Record<string, any>> {
  // 如果已有完整业务数据，直接使用
  if (props.record && typeof props.record === 'object' && Object.keys(props.record).length > 0) {
    return props.record
  }

  // 尝试从路由参数获取业务数据（如果是详情页，data 可能已通过 props/attrs 传入）
  return {}
}

/** 查找可用的打印链 */
async function findChainId(pageCode: string): Promise<number | null> {
  try {
    const res = await printingApi.getChains({ page: 1, size: 10, pageCode })
    const chains = (res as any)?.data?.records || (res as any)?.data || []
    if (chains.length === 0) return null
    // 取第一个启用的打印链
    const active = chains.find((c: any) => c.status === 'ACTIVE' || c.status === 'ENABLED' || !c.status)
    return active?.chainId || chains[0]?.chainId || null
  } catch {
    return null
  }
}

/** 获取单据编号 */
function getDocumentNo(): string {
  if (props.record?.orderNo) return props.record.orderNo
  if (props.record?.invoiceNo) return props.record.invoiceNo
  if (props.record?.inboundNo) return props.record.inboundNo
  if (props.record?.outboundNo) return props.record.outboundNo
  if (props.record?.stocktakeNo) return props.record.stocktakeNo
  if (props.record?.returnNo) return props.record.returnNo
  if (props.record?.documentNo) return props.record.documentNo
  if (props.record?.applicationNo) return props.record.applicationNo
  if (props.record?.reimbursementNo) return props.record.reimbursementNo
  if (props.record?.paymentNo) return props.record.paymentNo
  if (props.record?.voucherNo) return props.record.voucherNo
  if (props.record?.contractNo) return props.record.contractNo
  if (props.record?.assetCode) return props.record.assetCode
  if (props.record?.disposalNo) return props.record.disposalNo
  if (props.record?.transferNo) return props.record.transferNo
  if (props.record?.quotationNo) return props.record.quotationNo
  if (props.record?.shipmentNo) return props.record.shipmentNo
  return ''
}

/** 获取单据类型 */
function getDocumentType(): string {
  if (props.businessType) return props.businessType
  if (props.templateType) {
    const map: Record<string, string> = {
      order: 'ORDER',
      invoice: 'INVOICE',
      inbound: 'INBOUND',
      inquiry: 'INQUIRY',
      stock: 'STOCK',
      customer: 'CUSTOMER',
    }
    return map[props.templateType] || props.templateType.toUpperCase()
  }
  return ''
}

/** 主打印逻辑 */
async function handlePrint() {
  if (printing.value) return
  printing.value = true

  try {
    // 1. 确定打印链 ID
    let targetChainId = props.chainId
    if (!targetChainId) {
      const pageCode = getPageCode()
      targetChainId = await findChainId(pageCode)
      if (!targetChainId) {
        message.warning('未找到对应的打印配置，请先在打印管理模块配置打印链')
        printing.value = false
        return
      }
    }

    // 2. 构建业务数据
    const businessData = await getBusinessData()
    const documentNo = getDocumentNo()
    const documentType = getDocumentType()
    const documentId = props.businessId ? Number(props.businessId) : undefined

    // 3. 执行打印链
    const result = await printingApi.executeChain({
      chainId: targetChainId,
      dataJson: JSON.stringify(businessData),
      pageCode: getPageCode(),
      documentType: documentType || undefined,
      documentId: documentId || undefined,
      documentNo: documentNo || undefined,
    })

    message.success('打印任务已提交')
    emit('print-success', result)
  } catch (err: any) {
    const error = err instanceof Error ? err : new Error(err?.message || '打印失败')
    message.error(error.message)
    emit('print-error', error)
  } finally {
    printing.value = false
  }
}
</script>
