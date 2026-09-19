<template>
  <a-modal
    v-model:open="visible"
    title="打印"
    :width="820"
    :footer="null"
    :closable="true"
    :mask-closable="false"
    wrap-class-name="print-dialog-wrap"
    @cancel="handleClose"
  >
    <div class="print-dialog-body">
      <!-- 左侧：选项区 -->
      <div class="print-options">
        <div class="option-group">
          <label class="option-label">打印模板</label>
          <a-select
            v-model:value="selectedTemplateId"
            placeholder="选择打印模板"
            style="width: 100%"
            :loading="loadingTemplates"
            :disabled="props.alwaysLastTemplate && !!readLastTemplateId()"
            @change="handleTemplateChange"
          >
            <a-select-option
              v-for="tpl in templates"
              :key="tpl.templateId"
              :value="tpl.templateId"
            >
              {{ tpl.templateName }}
              <a-tag
                v-if="tpl.isDefault"
                color="blue"
                size="small"
              >
                默认
              </a-tag>
            </a-select-option>
          </a-select>
        </div>

        <div class="option-group">
          <label class="option-label">打印模式</label>
          <a-radio-group
            v-model:value="printMode"
            button-style="solid"
            size="small"
          >
            <a-radio-button value="local">
              本地打印
            </a-radio-button>
            <!-- 「打印设置 → 远程打印」关闭时禁用该模式（配置真实生效，不是装饰） -->
            <a-radio-button
              value="remote"
              :disabled="remoteDisabled"
            >
              远程打印
            </a-radio-button>
          </a-radio-group>
          <div
            v-if="remoteDisabled"
            class="option-hint"
          >
            远程打印已在「打印设置」中关闭
          </div>
        </div>

        <div class="option-group">
          <label class="option-label">打印份数</label>
          <a-input-number
            v-model:value="copies"
            :min="1"
            :max="99"
            size="small"
            style="width: 100%"
          />
        </div>

        <div class="option-group">
          <label class="option-label">页面范围</label>
          <a-radio-group
            v-model:value="pageRangeType"
            size="small"
          >
            <a-radio value="all">
              全部
            </a-radio>
            <a-radio value="current">
              当前页
            </a-radio>
          </a-radio-group>
        </div>

        <!-- 本页已生效的打印设置（读不到配置时整块不渲染，界面与改造前一致） -->
        <div
          v-if="appliedHints.length"
          class="option-hint"
        >
          <div
            v-for="hint in appliedHints"
            :key="hint"
          >
            {{ hint }}
          </div>
        </div>

        <div class="option-actions">
          <a-button
            type="primary"
            :loading="rendering"
            @click="handlePreview"
          >
            <template #icon>
              <EyeOutlined />
            </template>
            预览
          </a-button>
          <a-button
            type="primary"
            ghost
            :loading="printing"
            :disabled="!canSubmitPrint"
            @click="handlePrint"
          >
            <template #icon>
              <PrinterOutlined />
            </template>
            打印
          </a-button>
        </div>

        <div class="option-footer">
          <a-button
            type="link"
            size="small"
            @click="handleOpenDesigner"
          >
            <template #icon>
              <EditOutlined />
            </template>
            模板设计器
          </a-button>
        </div>
      </div>

      <!-- 右侧：预览区 -->
      <div class="print-preview">
        <div
          v-if="!previewHtml"
          class="preview-placeholder"
        >
          <FileTextOutlined style="font-size: 48px; color: #d9d9d9;" />
          <p>点击"预览"查看打印效果</p>
        </div>
        <div
          v-else
          class="preview-content"
          v-html="previewHtml"
        />
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined,
  EyeOutlined,
  EditOutlined,
  FileTextOutlined,
} from '@ant-design/icons-vue'
import { printingApi, type PrintTemplateVO } from '@/api/printing'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import type { PrintBehaviorConfig } from '@/api/set/print-config'
import {
  loadPrintBehaviorConfig,
  applyPrintBehavior,
  isDraftDocument,
  pickActiveChain,
  resolveDocumentNo,
} from './printBehavior'

defineOptions({ name: 'PrintDialog' })

const props = defineProps<{
  /** 页面编码，用于筛选对应页面的模板，如 'sale', 'purchase', 'receipt' */
  pageCode: string
  /** 打印数据，会传递给模板渲染接口 */
  printData: Record<string, any>
  /** 页面打印设置：默认打印模板ID（未指定时用模板自身的默认标记） */
  defaultTemplateId?: number | null
  /** 页面打印设置：默认打印份数 */
  defaultCopies?: number
  /** 页面打印设置：始终使用最后一次打印的模板，打印时不再选择 */
  alwaysLastTemplate?: boolean
  /**
   * 单据是否草稿（可选，**显式声明**「允许打印草稿」门控所需的单据状态）。
   * 不传时由 `printBehavior.isDraftDocument` 从打印数据自动推断（见该函数注释里的判定顺序与兜底）。
   */
  isDraft?: boolean
  /** 单据类型（可选，仅用于远程打印任务留痕；不传时退化为 pageCode） */
  documentType?: string
}>()

const emit = defineEmits<{
  'print-success': []
}>()

const router = useRouter()
const userStore = useUserStore()

const visible = ref(false)
const templates = ref<PrintTemplateVO[]>([])
const selectedTemplateId = ref<number | null>(null)
const printMode = ref<'local' | 'remote'>('local')
const copies = ref(1)
const pageRangeType = ref<'all' | 'current'>('all')
const previewHtml = ref('')
const loadingTemplates = ref(false)
const rendering = ref(false)
const printing = ref(false)

/**
 * 打印设置（设置 → 打印管理 → 打印设置）。
 * **null = 没读到（无权限/接口失败）** → 组件完全按改造前的原有行为工作（见 printBehavior.ts 纪律 ①）。
 */
const printBehavior = ref<PrintBehaviorConfig | null>(null)

/** 「打印设置 → 远程打印」关闭 → 远程模式不可选 */
const remoteDisabled = computed(() => printBehavior.value?.remoteEnabled === false)

/**
 * 「打印」按钮是否可点。
 * · 预览过 → 可点（改造前的唯一条件）；
 * · 选了「远程打印」→ 可点（内容由打印链上的模板渲染，不依赖本弹窗的预览）；
 * · 「助手打印」开启 → 可点（点击时先自动渲染再打印）。
 * 配置读不到时只剩「预览过」一个条件，即改造前的行为。
 */
const canSubmitPrint = computed(() =>
  !!previewHtml.value
  || printMode.value === 'remote'
  || printBehavior.value?.assistantEnabled === true,
)

/**
 * 弹窗内如实展示「正在改变打印行为的设置项」（读不到配置时为空 → 不渲染，界面与改造前一致）。
 * 只列**会改变操作结果**的项，避免把默认值也当提示刷屏。
 */
const appliedHints = computed(() => {
  const cfg = printBehavior.value
  if (!cfg) return []
  const hints: string[] = []
  if (cfg.assistantEnabled) hints.push('助手打印已开启：点击「打印」将跳过预览直接打印')
  if (cfg.decimalEnabled) {
    hints.push(`单据打印小数位数已开启：数量 ${cfg.qtyDecimal} 位、单价 ${cfg.priceDecimal} 位`)
  }
  return hints
})

/**
 * 打开弹窗。
 *
 * 两道与打印设置相关的关卡（读不到配置时两道都不生效，即改造前的行为）：
 * ① 「允许打印草稿」关闭时，草稿单据**不允许打印**并给出明确提示（ql361 原文：关闭后不可打印草稿）；
 * ② 「远程打印」关闭时把模式收敛回本地打印。
 */
async function open() {
  printBehavior.value = await loadPrintBehaviorConfig()

  if (printBehavior.value?.allowDraftPrint === false && isDraftDocument(props.printData, props.isDraft)) {
    message.warning(
      '当前单据为草稿状态。「打印设置」中已关闭「允许打印草稿」，不允许打印；'
      + '请先保存/审核生效，或由管理员在「设置 → 打印管理 → 打印设置」中开启该选项。',
    )
    return
  }

  if (remoteDisabled.value) printMode.value = 'local'

  visible.value = true
  await loadTemplates()
}

/** 关闭弹窗 */
function handleClose() {
  visible.value = false
  previewHtml.value = ''
}

/** 「始终使用最后一次打印的模板」所需的本地记录（按页面编码隔离） */
const lastTemplateKey = computed(() => `print-last-template-${props.pageCode}`)
function readLastTemplateId(): number | null {
  try {
    const v = localStorage.getItem(lastTemplateKey.value)
    return v ? Number(v) : null
  } catch { return null }
}
function writeLastTemplateId(id: number | null) {
  if (id == null) return
  try { localStorage.setItem(lastTemplateKey.value, String(id)) } catch { /* 忽略写入失败 */ }
}

/** 加载当前 pageCode 的模板列表 */
async function loadTemplates() {
  loadingTemplates.value = true
  try {
    const res = await printingApi.getTemplates({
      page: 1,
      size: 50,
      pageCode: props.pageCode,
      status: 1, // 已发布
    })
    const records = res?.data?.records || res?.records || []
    templates.value = records
    // 优先级：页面打印设置指定模板 > 「始终使用最后一次打印的模板」 > 模板默认标记 > 第一个
    const lastUsedId = readLastTemplateId()
    const bySetting = props.defaultTemplateId
      ? records.find((t: PrintTemplateVO) => t.templateId === props.defaultTemplateId)
      : undefined
    const byLastUsed = (props.alwaysLastTemplate && lastUsedId)
      ? records.find((t: PrintTemplateVO) => t.templateId === lastUsedId)
      : undefined
    const byDefault = records.find((t: PrintTemplateVO) => t.isDefault)
    const picked = bySetting || byLastUsed || byDefault || records[0]
    selectedTemplateId.value = picked?.templateId || null
    copies.value = props.defaultCopies && props.defaultCopies > 0 ? props.defaultCopies : 1
  } catch (e) {
    templates.value = []
    selectedTemplateId.value = null
  } finally {
    loadingTemplates.value = false
  }
}

/** 模板切换时清空预览 */
function handleTemplateChange() {
  previewHtml.value = ''
}

/** 预览模板 */
async function handlePreview() {
  if (!selectedTemplateId.value) {
    message.warning('请先选择打印模板')
    return
  }
  const tpl = templates.value.find(t => t.templateId === selectedTemplateId.value)
  if (!tpl) {
    message.error('模板不存在')
    return
  }
  rendering.value = true
  try {
    // 打印设置在此处**真正作用于打印内容**：小数位格式化 + 「打印内容」派生的 batchEffectiveText
    // （配置读不到时 applyPrintBehavior 原样返回，即改造前的行为）
    const renderData = applyPrintBehavior(props.printData, printBehavior.value)
    const res = await printingApi.renderTemplate({
      templateJson: tpl.templateJson,
      dataJson: JSON.stringify(renderData),
    })
    previewHtml.value = res?.data?.html || res?.html || '<p>渲染结果为空</p>'
  } catch (e: any) {
    message.error('模板渲染失败：' + (e?.message || '未知错误'))
    previewHtml.value = ''
  } finally {
    rendering.value = false
  }
}

/**
 * 提交远程打印（复用既有打印链能力，不重复实现打印执行）。
 *
 * 三道前置检查都在**提交前**完成并给出可读原因 —— 宁可当场说清，也不留一个永远不会被执行的
 * pending 任务让人误以为「已经远程打印了」：
 * ① 「打印设置 → 远程打印」关闭 → 不提交；
 * ② 本页没有已启用（ACTIVE）的打印链 → 不提交；
 * ③ 没有已注册的打印客户端（打印助手）→ 不提交（任务会永久 pending）。
 */
async function submitRemotePrint(): Promise<boolean> {
  if (remoteDisabled.value) {
    message.warning('远程打印已在「打印设置 → 远程打印」中关闭，请改用本地打印')
    return false
  }
  printing.value = true
  try {
    const chainRes: any = await printingApi.getChains({ page: 1, size: 100, pageCode: props.pageCode })
    const chainRecords = chainRes?.data?.records || chainRes?.records || []
    const chain = pickActiveChain(chainRecords)
    if (!chain) {
      message.warning(`本页（${props.pageCode}）没有已启用的打印链路，请先在「打印链路」中配置并启用`)
      return false
    }

    const clientRes: any = await printingApi.getClients({ page: 1, size: 1 })
    const clientRecords = clientRes?.data?.records || clientRes?.records || []
    if (!clientRecords.length) {
      message.warning(
        '未检测到已注册的打印客户端（打印助手），远程打印无法送达；'
        + '请在作为远程点的计算机上安装并注册打印助手后重试',
      )
      return false
    }

    // dataJson 是**对象**（后端 ChainTaskExecuteRequest.dataJson 为 Map）、userId 走请求头
    // （打印任务接口要求 tenantId/userId 两个请求头；tenantId 由 request 拦截器统一注入）
    const renderData = applyPrintBehavior(props.printData, printBehavior.value)
    await request.post(
      '/v2/print/tasks/by-chain',
      {
        chainId: chain.chainId,
        dataJson: renderData,
        pageCode: props.pageCode,
        documentType: props.documentType || props.pageCode,
        documentNo: resolveDocumentNo(renderData),
      },
      { headers: { userId: String(userStore.userId || 0) } },
    )
    message.success(`已提交远程打印任务（链路：${chain.chainName || chain.chainId}）`)
    return true
  } catch (e: any) {
    message.error('远程打印提交失败：' + (e?.message || '未知错误'))
    return false
  } finally {
    printing.value = false
  }
}

/** 执行打印 */
async function handlePrint() {
  // 「打印设置 → 助手打印」开启后跳过预览直接打印（ql361 原文：开启助手打印，将跳过预览直接打印）
  if (!previewHtml.value && printMode.value === 'local' && printBehavior.value?.assistantEnabled === true) {
    if (!selectedTemplateId.value) {
      message.warning('请先选择打印模板')
      return
    }
    await handlePreview()
    if (!previewHtml.value) return // 渲染失败：handlePreview 已给出可读提示
  }

  if (printMode.value === 'remote') {
    const ok = await submitRemotePrint()
    if (ok) {
      writeLastTemplateId(selectedTemplateId.value)
      emit('print-success')
    }
    return
  }

  if (!previewHtml.value) {
    message.warning('请先预览模板')
    return
  }
  printing.value = true
  try {
    // 本地打印：创建 iframe 调用浏览器打印
    const iframe = document.createElement('iframe')
    iframe.style.position = 'fixed'
    iframe.style.right = '0'
    iframe.style.bottom = '0'
    iframe.style.width = '0'
    iframe.style.height = '0'
    iframe.style.border = '0'
    document.body.appendChild(iframe)

    const doc = iframe.contentDocument || iframe.contentWindow?.document
    if (!doc) {
      message.error('无法创建打印窗口')
      return
    }
    doc.open()
    doc.write(`<!DOCTYPE html><html><head><title>打印</title>
        <style>
          body { margin: 0; padding: 8mm; font-family: SimSun, serif; font-size: 12px; }
          table { border-collapse: collapse; width: 100%; }
          td, th { border: 1px solid #333; padding: 4px 6px; }
          @media print { @page { size: auto; margin: 8mm; } }
        </style>
      </head><body>${previewHtml.value}</body></html>`)
    doc.close()

    // 等待内容加载
    await new Promise(resolve => setTimeout(resolve, 300))

    if (iframe.contentWindow) {
      iframe.contentWindow.focus()
      iframe.contentWindow.print()
    }

    // 清理 iframe
    setTimeout(() => {
      document.body.removeChild(iframe)
    }, 1000)

    message.success('已发送到本地打印机')
    writeLastTemplateId(selectedTemplateId.value)
    emit('print-success')
  } catch (e: any) {
    message.error('打印失败：' + (e?.message || '未知错误'))
  } finally {
    printing.value = false
  }
}

/** 打开模板设计器 */
function handleOpenDesigner() {
  const tplId = selectedTemplateId.value || ''
  router.push({
    path: '/printing/designer',
    query: { pageCode: props.pageCode, templateId: tplId ? String(tplId) : '' },
  })
  visible.value = false
}

// 监听 visible 变化来加载模板
watch(visible, (val) => {
  if (val) {
    loadTemplates()
  }
})

// appliedBehavior 供 E2E/调试断言「本次打印读到的配置」（返回值可能为 null = 未读到，走原有行为）
defineExpose({ open, appliedBehavior: () => printBehavior.value })
</script>

<style scoped>
.print-dialog-body {
  display: flex;
  gap: 16px;
  min-height: 480px;
}

.print-options {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.option-label {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
}

.option-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

/* 打印设置生效提示（读不到配置时不渲染） */
.option-hint {
  padding: 6px 8px;
  background: #f6ffed;
  border: 1px solid #d9f7be;
  border-radius: 4px;
  font-size: 12px;
  color: #389e0d;
  line-height: 1.7;
}

.option-footer {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.print-preview {
  flex: 1;
  min-width: 0;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: #fafafa;
  overflow: auto;
  max-height: 520px;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
  color: #bfbfbf;
  gap: 12px;
}

.preview-placeholder p {
  font-size: 13px;
  margin: 0;
}

.preview-content {
  padding: 16px;
  background: #fff;
  min-height: 400px;
}

.preview-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
}

.preview-content :deep(td),
.preview-content :deep(th) {
  border: 1px solid #333;
  padding: 4px 6px;
}
</style>

<style>
.print-dialog-wrap .ant-modal-header {
  background: #303030 !important;
  border-radius: 8px 8px 0 0;
  padding: 14px 24px;
  margin-bottom: 0;
}

.print-dialog-wrap .ant-modal-title {
  color: #fff !important;
  font-size: 16px;
  font-weight: 500;
}

.print-dialog-wrap .ant-modal-close {
  color: #fff !important;
  top: 14px;
}

.print-dialog-wrap .ant-modal-body {
  padding: 16px;
}
</style>
