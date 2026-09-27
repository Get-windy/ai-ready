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
          <!-- 空态说明：此前列表是空的、打印按钮是灰的，用户看不出「为什么不能打印」 -->
          <div v-if="!loadingTemplates && !templates.length" class="option-hint">
            该页面还没有已发布的打印模板，请到「设置 → 打印管理 → 打印模板」配置并发布后再打印。
          </div>
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
        <!--
          用 iframe(srcdoc) 而不是 v-html：渲染结果是**完整 HTML 文档**，v-html 会把其中的
          <style> 提到当前页面里生效 —— 模板的 body/table 样式会污染整个后台界面，
          而预览本身又丢了 <head> 里的设置，所见与所打不一致。iframe 天然隔离样式，且保真。
        -->
        <iframe
          v-else
          class="preview-content"
          :srcdoc="previewHtml"
          title="打印预览"
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
import { printingApi, type DocumentTemplatesVO } from '@/api/printing'
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
  /**
   * 单据主键。**给了它就走后端装配数据的系统级路径**：取数、挑模板、打印设置加工全在服务端，
   * 页面不必准备 printData。该页面在后端注册了 PrintDataProvider 时才生效
   * （以 `GET /v2/print/documents/{pageCode}/templates` 返回的 supported 为准）。
   */
  documentId?: number | string | null
  /**
   * 打印数据（兼容路径：页面自己准备数据，前端调渲染接口）。
   * 与 documentId 二选一；两者都给时优先 documentId。
   */
  printData?: Record<string, any>
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
const templates = ref<DocumentTemplatesVO['templates']>([])

/** 后端是否已为该页面注册数据装配器（true 才能只给单据主键就打印） */
const backendAssemble = ref(false)
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

  // 走后端装配（只给 documentId）时前端拿不到单据字段，草稿判定只能靠调用方显式传 :is-draft，
  // 否则这一关不生效（与改造前「配置读不到就不拦」同一降级口径）
  if (printBehavior.value?.allowDraftPrint === false
    && isDraftDocument(props.printData || {}, props.isDraft)) {
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

/**
 * 加载当前 pageCode 的**已发布**模板。
 *
 * 「只有已发布才能打印」这条规则由服务端定（`GET /v2/print/documents/{pageCode}/templates`），
 * 前端不再自己拼 status —— 早先前端发 `status: 1`（数字）、后端列是 varchar 存 'PUBLISHED'，
 * 查询恒不命中，模板下拉永远空白。
 */
async function loadTemplates() {
  loadingTemplates.value = true
  try {
    const res: any = await printingApi.getDocumentTemplates(props.pageCode)
    const body = res?.data || res || {}
    templates.value = body.templates || []
    backendAssemble.value = body.supported === true
    // 优先级：页面打印设置指定模板 > 「始终使用最后一次打印的模板」 > 模板默认标记 > 第一个
    const lastUsedId = readLastTemplateId()
    const bySetting = props.defaultTemplateId
      ? templates.value.find(t => t.templateId === props.defaultTemplateId)
      : undefined
    const byLastUsed = (props.alwaysLastTemplate && lastUsedId)
      ? templates.value.find(t => t.templateId === lastUsedId)
      : undefined
    const byDefault = templates.value.find(t => t.isDefault)
    const picked = bySetting || byLastUsed || byDefault || templates.value[0]
    selectedTemplateId.value = picked?.templateId || null
    copies.value = props.defaultCopies && props.defaultCopies > 0 ? props.defaultCopies : 1
  } catch (e) {
    templates.value = []
    selectedTemplateId.value = null
    backendAssemble.value = false
  } finally {
    loadingTemplates.value = false
  }
}

/** 模板切换时清空预览 */
function handleTemplateChange() {
  previewHtml.value = ''
}

/**
 * 渲染预览。
 *
 * 两条路径：
 * ① **后端装配**（给了 documentId 且该页面已注册 PrintDataProvider）——
 *    服务端自己取数、挑模板、应用打印设置，页面什么都不用准备；
 * ② **兼容路径**（页面自己给 printData）—— 模板正文按需单独取（列表接口不再回传 templateJson），
 *    打印设置仍由前端加工（这份口径已在服务端另有一份实现，供路径 ① 使用）。
 */
async function handlePreview() {
  if (!selectedTemplateId.value) {
    message.warning('请先选择打印模板')
    return
  }
  rendering.value = true
  try {
    const docId = props.documentId
    if (docId !== null && docId !== undefined && docId !== '' && backendAssemble.value) {
      const res: any = await printingApi.renderDocument(
        props.pageCode, docId, selectedTemplateId.value,
      )
      previewHtml.value = res?.data?.html || res?.html || '<p>渲染结果为空</p>'
      return
    }

    const tpl = templates.value.find(t => t.templateId === selectedTemplateId.value)
    if (!tpl?.templateJson) {
      message.error('模板不存在')
      return
    }
    // 模板正文可能是 JSON 串（实体列）也可能是对象，两种都兜住
    const templateJson = typeof tpl.templateJson === 'string'
      ? tpl.templateJson
      : JSON.stringify(tpl.templateJson)
    // 打印设置在此处**真正作用于打印内容**：小数位格式化 + 「打印内容」派生的 batchEffectiveText
    // （配置读不到时 applyPrintBehavior 原样返回）
    const renderData = applyPrintBehavior(props.printData || {}, printBehavior.value)
    const res = await printingApi.renderTemplate({
      templateJson,
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

    // 两条取数路径与「预览」一致（见 handlePreview 的说明）：
    //  · 后端装配（只给 documentId）→ dataJson 留空，服务端按 pageCode + documentId 自己取；
    //  · 兼容路径（页面给 printData）→ 前端加工后随请求发出。
    const docId = props.documentId
    const useBackend = docId !== null && docId !== undefined && docId !== '' && backendAssemble.value
    const renderData = useBackend
      ? {}
      : applyPrintBehavior(props.printData || {}, printBehavior.value)
    await request.post(
      '/v2/print/tasks/by-chain',
      {
        chainId: chain.chainId,
        // dataJson 是**对象**（后端 ChainTaskExecuteRequest.dataJson 为 Map）
        dataJson: renderData,
        pageCode: props.pageCode,
        documentType: props.documentType || props.pageCode,
        documentId: useBackend ? docId : undefined,
        // 后端装配模式下 renderData 是空的，单号仍从页面给的 printData 里取（拿不到就留空）
        documentNo: resolveDocumentNo(useBackend ? (props.printData || {}) : renderData),
      },
      // userId 走请求头（打印任务接口要求 tenantId/userId 两个请求头；tenantId 由 request 拦截器统一注入）
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
/**
 * 打印兜底样式：只补模板自身通常不声明的部分。
 *
 * ⚠️ 必须放在模板自己的 `<style>` **之前** —— 模板要覆盖就能覆盖。
 * 尤其**不要**在这里写 `td,th{border:…}`：无边框模板会被强行加上边框，
 * 打出来和设计器里看到的不是一个东西。
 */
const PRINT_FALLBACK_STYLE =
  '<style>body{margin:0;font-family:SimSun,serif;font-size:12px;}</style>'

/**
 * 把渲染结果包成可直接 `doc.write` 的完整文档。
 *
 * 渲染接口返回的本来就是完整 HTML（含模板的 `<style>` 与 `@page`），
 * 旧实现把它塞进 `<body>` 再外套一层 `<html>` —— 文档套文档，内层 `<head>`/`<style>`
 * 会被浏览器丢弃或错位，模板的字体与纸张尺寸设置随之失效。已是完整文档就直接写。
 */
function buildPrintDocument(html: string): string {
  if (/<html[\s>]/i.test(html)) {
    return html.includes('<head>')
      ? html.replace('<head>', `<head>${PRINT_FALLBACK_STYLE}`)
      : PRINT_FALLBACK_STYLE + html
  }
  return `<!DOCTYPE html><html><head><meta charset="utf-8"><title>打印</title>`
    + `${PRINT_FALLBACK_STYLE}</head><body>${html}</body></html>`
}

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
    doc.write(buildPrintDocument(previewHtml.value))
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

.option-hint {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #d46b08;
  background: #fff7e6;
  border: 1px solid #ffe7ba;
  border-radius: 4px;
  padding: 6px 8px;
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

/*
 * 预览是一个 srcdoc iframe：样式由模板自己带（渲染结果里含完整 <style>）。
 * 这里**不要**再补 table/td 的边框样式 —— 那会盖掉模板自己的选择（如无边框模板），
 * 让预览看到的边框和真正打出来的不一样。之前用 v-html 时那几条 :deep 规则就是干这个的。
 */
.preview-content {
  display: block;
  width: 100%;
  height: 60vh;
  min-height: 400px;
  border: 0;
  background: #fff;
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
