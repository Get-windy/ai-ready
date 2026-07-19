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
            <a-radio-button value="remote">
              远程打印
            </a-radio-button>
          </a-radio-group>
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
            :disabled="!previewHtml"
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
import { ref, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined,
  EyeOutlined,
  EditOutlined,
  FileTextOutlined,
} from '@ant-design/icons-vue'
import { printingApi, type PrintTemplateVO } from '@/api/printing'
import { useRouter } from 'vue-router'

defineOptions({ name: 'PrintDialog' })

const props = defineProps<{
  /** 页面编码，用于筛选对应页面的模板，如 'sale', 'purchase', 'receipt' */
  pageCode: string
  /** 打印数据，会传递给模板渲染接口 */
  printData: Record<string, any>
}>()

const emit = defineEmits<{
  'print-success': []
}>()

const router = useRouter()

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

/** 打开弹窗 */
async function open() {
  visible.value = true
  await loadTemplates()
}

/** 关闭弹窗 */
function handleClose() {
  visible.value = false
  previewHtml.value = ''
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
    // 默认选中 isDefault 或第一个
    const defaultTpl = records.find((t: PrintTemplateVO) => t.isDefault)
    selectedTemplateId.value = defaultTpl?.templateId || records[0]?.templateId || null
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
    const res = await printingApi.renderTemplate({
      templateJson: tpl.templateJson,
      dataJson: JSON.stringify(props.printData),
    })
    previewHtml.value = res?.data?.html || res?.html || '<p>渲染结果为空</p>'
  } catch (e: any) {
    message.error('模板渲染失败：' + (e?.message || '未知错误'))
    previewHtml.value = ''
  } finally {
    rendering.value = false
  }
}

/** 执行打印 */
async function handlePrint() {
  if (!previewHtml.value) {
    message.warning('请先预览模板')
    return
  }
  printing.value = true
  try {
    if (printMode.value === 'local') {
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
    } else {
      // 远程打印：调用打印链执行
      message.info('远程打印功能配置中，请在打印链管理中添加客户端')
    }
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

defineExpose({ open })
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
