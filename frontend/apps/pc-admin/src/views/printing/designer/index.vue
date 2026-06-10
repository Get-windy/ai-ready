<template>
  <div class="template-designer">
    <!-- 顶部工具栏 -->
    <div class="designer-toolbar">
      <div class="toolbar-left">
        <a-breadcrumb>
          <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
          <a-breadcrumb-item><router-link to="/printing/template">打印模板</router-link></a-breadcrumb-item>
          <a-breadcrumb-item>{{ isEdit ? '编辑模板' : '新建模板' }}</a-breadcrumb-item>
        </a-breadcrumb>
      </div>
      <div class="toolbar-center">
        <a-space>
          <a-button-group>
            <a-button :type="zoomLevel === 100 ? 'primary' : 'default'" @click="setZoom(100)">
              100%
            </a-button>
            <a-button :type="zoomLevel === 75 ? 'primary' : 'default'" @click="setZoom(75)">
              75%
            </a-button>
            <a-button :type="zoomLevel === 50 ? 'primary' : 'default'" @click="setZoom(50)">
              50%
            </a-button>
          </a-button-group>
          <a-divider type="vertical" />
          <a-tooltip title="撤销">
            <a-button :disabled="undoStack.length === 0" @click="undo">
              <template #icon><UndoOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="重做">
            <a-button :disabled="redoStack.length === 0" @click="redo">
              <template #icon><RedoOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-divider type="vertical" />
          <a-tooltip title="显示网格">
            <a-button :type="showGrid ? 'primary' : 'default'" @click="showGrid = !showGrid">
              <template #icon><AppstoreOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-space>
      </div>
      <div class="toolbar-right">
        <a-space>
          <a-button @click="handlePreview">
            <template #icon><EyeOutlined /></template>
            预览
          </a-button>
          <a-button @click="handleSaveDraft">
            保存草稿
          </a-button>
          <a-button type="primary" @click="handlePublish">
            发布
          </a-button>
        </a-space>
      </div>
    </div>

    <div class="designer-body">
      <!-- 左侧：组件面板 -->
      <div class="component-panel">
        <div class="panel-header">组件</div>
        <div class="panel-body">
          <div
            v-for="comp in componentTypes"
            :key="comp.type"
            class="component-item"
            draggable="true"
            @dragstart="onDragStart($event, comp)"
          >
            <component :is="comp.icon" class="comp-icon" />
            <span class="comp-label">{{ comp.label }}</span>
          </div>
        </div>
        <a-divider />
        <div class="panel-header">模板列表</div>
        <div class="panel-body">
          <a-select
            v-model:value="activeLayerIndex"
            style="width: 100%"
            @change="onLayerSelect"
          >
            <a-select-option
              v-for="(comp, idx) in templateJson.components"
              :key="idx"
              :value="idx"
            >
              {{ comp.type }} #{{ idx + 1 }}
              <template v-if="comp.field"> - {{ comp.field }}</template>
            </a-select-option>
          </a-select>
          <a-button
            type="link"
            danger
            size="small"
            style="margin-top: 8px; width: 100%"
            :disabled="activeLayerIndex < 0"
            @click="deleteComponent(activeLayerIndex)"
          >
            <template #icon><DeleteOutlined /></template>
            删除选中组件
          </a-button>
        </div>
      </div>

      <!-- 中间：画布 -->
      <div class="canvas-area" ref="canvasAreaRef">
        <div
          class="designer-canvas"
          :class="{ 'show-grid': showGrid }"
          :style="canvasStyle"
          @dragover.prevent
          @drop="onDrop"
          @click="deselectAll"
        >
          <!-- 纸张尺寸指示 -->
          <div class="paper-size-label">
            {{ templateJson.paperSize }}
            <template v-if="templateJson.paperSize === 'CUSTOM'">
              {{ templateJson.paperWidth }}mm × {{ templateJson.paperHeight }}mm
            </template>
          </div>

          <!-- 组件渲染 -->
          <template v-for="(comp, idx) in templateJson.components" :key="idx">
            <div
              v-if="comp.type === 'label'"
              class="designer-component"
              :class="{ selected: selectedIndex === idx }"
              :style="getComponentStyle(comp)"
              @mousedown.stop="onComponentMouseDown(idx, $event)"
              @contextmenu.stop.prevent="onContextMenu($event, idx)"
            >
              <div class="comp-content label-content">{{ comp.content || '标签' }}</div>
              <div v-if="selectedIndex === idx" class="resize-handle" @mousedown.stop="startResize($event, idx)" />
            </div>

            <div
              v-else-if="comp.type === 'field'"
              class="designer-component"
              :class="{ selected: selectedIndex === idx }"
              :style="getComponentStyle(comp)"
              @mousedown.stop="onComponentMouseDown(idx, $event)"
              @contextmenu.stop.prevent="onContextMenu($event, idx)"
            >
              <div class="comp-content field-content">{{ comp.field || '字段名' }}</div>
              <div v-if="selectedIndex === idx" class="resize-handle" @mousedown.stop="startResize($event, idx)" />
            </div>

            <div
              v-else-if="comp.type === 'table'"
              class="designer-component"
              :class="{ selected: selectedIndex === idx }"
              :style="getComponentStyle(comp)"
              @mousedown.stop="onComponentMouseDown(idx, $event)"
              @contextmenu.stop.prevent="onContextMenu($event, idx)"
            >
              <div class="comp-content table-content">
                <table style="width: 100%; height: 100%; border-collapse: collapse;">
                  <thead>
                    <tr>
                      <th
                        v-for="col in (comp.columns || [])"
                        :key="col.field"
                        style="border: 1px dashed #999; padding: 2px; font-size: 10px;"
                      >
                        {{ col.header }}
                      </th>
                    </tr>
                  </thead>
                </table>
              </div>
              <div v-if="selectedIndex === idx" class="resize-handle" @mousedown.stop="startResize($event, idx)" />
            </div>

            <div
              v-else-if="comp.type === 'barcode' || comp.type === 'qrcode'"
              class="designer-component"
              :class="{ selected: selectedIndex === idx }"
              :style="getComponentStyle(comp)"
              @mousedown.stop="onComponentMouseDown(idx, $event)"
              @contextmenu.stop.prevent="onContextMenu($event, idx)"
            >
              <div class="comp-content barcode-content">
                <svg :width="comp.w" :height="comp.h" viewBox="0 0 100 40">
                  <rect v-for="i in 20" :key="i" :x="i * 4" y="0" :width="i % 3 === 0 ? 4 : 2" height="30" fill="#333" />
                  <text x="50" y="38" text-anchor="middle" font-size="8" fill="#666">
                    {{ comp.type === 'barcode' ? '条码' : '二维码' }}
                  </text>
                </svg>
              </div>
              <div v-if="selectedIndex === idx" class="resize-handle" @mousedown.stop="startResize($event, idx)" />
            </div>

            <div
              v-else-if="comp.type === 'image'"
              class="designer-component"
              :class="{ selected: selectedIndex === idx }"
              :style="getComponentStyle(comp)"
              @mousedown.stop="onComponentMouseDown(idx, $event)"
              @contextmenu.stop.prevent="onContextMenu($event, idx)"
            >
              <div class="comp-content image-content">
                <img v-if="comp.src" :src="comp.src" style="width: 100%; height: 100%; object-fit: contain;" alt="" />
                <span v-else>图片占位</span>
              </div>
              <div v-if="selectedIndex === idx" class="resize-handle" @mousedown.stop="startResize($event, idx)" />
            </div>

            <div
              v-else-if="comp.type === 'line'"
              class="designer-component"
              :class="{ selected: selectedIndex === idx }"
              :style="getComponentStyle(comp)"
              @mousedown.stop="onComponentMouseDown(idx, $event)"
              @contextmenu.stop.prevent="onContextMenu($event, idx)"
            >
              <hr style="margin: 0; border-top: 1px solid #333;" />
              <div v-if="selectedIndex === idx" class="resize-handle" @mousedown.stop="startResize($event, idx)" />
            </div>
          </template>

          <!-- 空状态 -->
          <div
            v-if="templateJson.components.length === 0"
            class="empty-canvas-hint"
          >
            从左侧拖拽组件到此处，或点击画布后按 Ctrl+V 粘贴
          </div>
        </div>
      </div>

      <!-- 右侧：属性面板 -->
      <div class="property-panel" v-if="selectedComponent">
        <div class="panel-header">属性</div>
        <div class="panel-body">
          <a-form :model="selectedComponent" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" size="small">
            <a-divider>基本属性</a-divider>

            <a-form-item label="类型">
              <a-tag>{{ selectedComponent.type }}</a-tag>
            </a-form-item>

            <a-form-item label="X (mm)">
              <a-input-number
                v-model:value="selectedComponent.x"
                :min="0"
                :max="300"
                style="width: 100%"
                @change="onPropChange"
              />
            </a-form-item>

            <a-form-item label="Y (mm)">
              <a-input-number
                v-model:value="selectedComponent.y"
                :min="0"
                :max="500"
                style="width: 100%"
                @change="onPropChange"
              />
            </a-form-item>

            <a-form-item label="宽度 (mm)">
              <a-input-number
                v-model:value="selectedComponent.w"
                :min="5"
                :max="300"
                style="width: 100%"
                @change="onPropChange"
              />
            </a-form-item>

            <a-form-item label="高度 (mm)">
              <a-input-number
                v-model:value="selectedComponent.h"
                :min="5"
                :max="500"
                style="width: 100%"
                @change="onPropChange"
              />
            </a-form-item>

            <a-divider>内容属性</a-divider>

            <template v-if="selectedComponent.type === 'label'">
              <a-form-item label="内容">
                <a-input v-model:value="selectedComponent.content" @change="onPropChange" />
              </a-form-item>
            </template>

            <template v-if="selectedComponent.type === 'field'">
              <a-form-item label="字段">
                <a-input v-model:value="selectedComponent.field" @change="onPropChange" />
              </a-form-item>
              <a-form-item label="格式">
                <a-input
                  v-model:value="selectedComponent.formatConfig"
                  placeholder="如: value + '元'"
                  @change="onPropChange"
                />
              </a-form-item>
            </template>

            <template v-if="selectedComponent.type === 'table'">
              <a-form-item label="数据字段">
                <a-input v-model:value="selectedComponent.field" @change="onPropChange" />
              </a-form-item>
              <a-form-item label="列配置">
                <a-button type="dashed" block @click="showColumnEditor = !showColumnEditor">
                  {{ (selectedComponent.columns || []).length }} 列
                </a-button>
              </a-form-item>
              <div v-if="showColumnEditor" class="column-editor">
                <div v-for="(col, ci) in (selectedComponent.columns || [])" :key="ci" class="column-item">
                  <a-input
                    v-model:value="col.header"
                    placeholder="表头"
                    size="small"
                    style="width: 45%; margin-right: 4px"
                    @change="onPropChange"
                  />
                  <a-input
                    v-model:value="col.field"
                    placeholder="字段"
                    size="small"
                    style="width: 40%; margin-right: 4px"
                    @change="onPropChange"
                  />
                  <a-button type="link" danger size="small" @click="removeColumn(ci)">
                    <DeleteOutlined />
                  </a-button>
                </div>
                <a-button type="dashed" block size="small" @click="addColumn">+ 添加列</a-button>
              </div>
            </template>

            <template v-if="selectedComponent.type === 'barcode' || selectedComponent.type === 'qrcode'">
              <a-form-item label="数据字段">
                <a-input v-model:value="selectedComponent.field" @change="onPropChange" />
              </a-form-item>
            </template>

            <template v-if="selectedComponent.type === 'image'">
              <a-form-item label="图片地址">
                <a-input v-model:value="selectedComponent.src" @change="onPropChange" placeholder="http://..." />
              </a-form-item>
            </template>

            <a-divider v-if="selectedComponent.type !== 'line'">样式</a-divider>

            <template v-if="selectedComponent.type !== 'line'">
              <a-form-item label="字体大小">
                <a-input-number
                  v-model:value="selectedComponent.fontSize"
                  :min="8"
                  :max="72"
                  style="width: 100%"
                  @change="onPropChange"
                />
              </a-form-item>
              <a-form-item label="字体粗细">
                <a-select v-model:value="selectedComponent.fontWeight" @change="onPropChange">
                  <a-select-option value="normal">正常</a-select-option>
                  <a-select-option value="bold">粗体</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="文本对齐">
                <a-radio-group v-model:value="selectedComponent.textAlign" button-style="solid" @change="onPropChange">
                  <a-radio-button value="left">左</a-radio-button>
                  <a-radio-button value="center">中</a-radio-button>
                  <a-radio-button value="right">右</a-radio-button>
                </a-radio-group>
              </a-form-item>
            </template>
          </a-form>
        </div>
      </div>
    </div>

    <!-- 预览模态框 -->
    <a-modal
      v-model:open="previewVisible"
      title="打印预览"
      width="800px"
      :footer="null"
      :body-style="{ padding: 0 }"
    >
      <iframe
        ref="previewFrame"
        :srcdoc="previewHtml"
        style="width: 100%; height: 600px; border: none;"
      />
    </a-modal>

    <!-- 右键菜单 -->
    <a-menu
      v-if="contextMenuVisible"
      :style="{ position: 'fixed', left: contextMenuPos.x + 'px', top: contextMenuPos.y + 'px', zIndex: 1000 }"
      @click="onContextMenuClick"
    >
      <a-menu-item key="delete">
        <DeleteOutlined /> 删除
      </a-menu-item>
      <a-menu-item key="duplicate">
        <CopyOutlined /> 复制
      </a-menu-item>
      <a-menu-item key="bringFront">
        <VerticalAlignTopOutlined /> 置顶
      </a-menu-item>
      <a-menu-item key="sendBack">
        <VerticalAlignBottomOutlined /> 置底
      </a-menu-item>
    </a-menu>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  UndoOutlined,
  RedoOutlined,
  AppstoreOutlined,
  EyeOutlined,
  DeleteOutlined,
  CopyOutlined,
  VerticalAlignTopOutlined,
  VerticalAlignBottomOutlined,
  FontSizeOutlined,
  TableOutlined,
  PictureOutlined,
  MinusOutlined,
  BarcodeOutlined,
  QrcodeOutlined,
  FontColorsOutlined,
} from '@ant-design/icons-vue'
import { printingApi } from '@/api/printing'

const route = useRoute()
const router = useRouter()

// ── 组件类型定义 ────────────────────────────────────────

interface DesignerComponent {
  type: string
  x: number
  y: number
  w: number
  h: number
  field?: string
  content?: string
  formatConfig?: string
  columns?: Array<{ header: string; field: string; width?: number }>
  src?: string
  fontSize?: number
  fontWeight?: string
  textAlign?: string
  [key: string]: any
}

interface TemplateJson {
  paperSize: string
  paperWidth: number
  paperHeight: number
  marginTop: number
  marginBottom: number
  marginLeft: number
  marginRight: number
  components: DesignerComponent[]
}

const componentTypes = [
  { type: 'label', label: '标签', icon: 'FontColorsOutlined' },
  { type: 'field', label: '字段', icon: 'FontSizeOutlined' },
  { type: 'table', label: '表格', icon: 'TableOutlined' },
  { type: 'barcode', label: '条码', icon: 'BarcodeOutlined' },
  { type: 'qrcode', label: '二维码', icon: 'QrcodeOutlined' },
  { type: 'image', label: '图片', icon: 'PictureOutlined' },
  { type: 'line', label: '分割线', icon: 'MinusOutlined' },
]

// ── 状态 ────────────────────────────────────────────────

const isEdit = ref(false)
const templateId = ref<number | null>(null)
const templateName = ref('')
const pageCode = ref('')

const zoomLevel = ref(100)
const showGrid = ref(true)
const selectedIndex = ref(-1)
const activeLayerIndex = ref(-1)
const showColumnEditor = ref(false)
const previewVisible = ref(false)
const previewHtml = ref('')
const contextMenuVisible = ref(false)
const contextMenuPos = ref({ x: 0, y: 0 })
const contextMenuIdx = ref(-1)

const undoStack = ref<string[]>([])
const redoStack = ref<string[]>([])

const canvasAreaRef = ref<HTMLElement | null>(null)

const defaultTemplateJson: TemplateJson = {
  paperSize: 'A4',
  paperWidth: 210,
  paperHeight: 297,
  marginTop: 10,
  marginBottom: 10,
  marginLeft: 10,
  marginRight: 10,
  components: [],
}

const templateJson = reactive<TemplateJson>({
  paperSize: 'A4',
  paperWidth: 210,
  paperHeight: 297,
  marginTop: 10,
  marginBottom: 10,
  marginLeft: 10,
  marginRight: 10,
  components: [],
})

// ── 计算属性 ────────────────────────────────────────────

const canvasStyle = computed(() => {
  const scale = zoomLevel.value / 100
  // 1mm ≈ 3.78px at 96dpi, use 3.75 for simpler math
  const pxPerMm = 3.75 * scale
  const w = templateJson.paperSize === 'CUSTOM'
    ? templateJson.paperWidth * pxPerMm
    : templateJson.paperSize === 'A5'
      ? 148 * pxPerMm
      : 210 * pxPerMm
  const h = templateJson.paperSize === 'CUSTOM'
    ? templateJson.paperHeight * pxPerMm
    : templateJson.paperSize === 'A5'
      ? 210 * pxPerMm
      : 297 * pxPerMm
  return {
    width: `${w}px`,
    minHeight: `${h}px`,
    padding: `${templateJson.marginTop * pxPerMm}px ${templateJson.marginRight * pxPerMm}px ${templateJson.marginBottom * pxPerMm}px ${templateJson.marginLeft * pxPerMm}px`,
  }
})

const selectedComponent = computed(() => {
  if (selectedIndex.value >= 0 && selectedIndex.value < templateJson.components.length) {
    return templateJson.components[selectedIndex.value]
  }
  return null
})

// ── 初始化 ──────────────────────────────────────────────

onMounted(async () => {
  const id = route.params.id
  if (id && id !== 'new') {
    isEdit.value = true
    templateId.value = Number(id)
    await loadTemplate(Number(id))
  }
  document.addEventListener('keydown', handleKeydown)
  document.addEventListener('click', handleGlobalClick)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('click', handleGlobalClick)
})

async function loadTemplate(id: number) {
  try {
    const res = await printingApi.getTemplate(id)
    if (res.data) {
      templateName.value = (res.data as any).templateName
      pageCode.value = (res.data as any).pageCode
      Object.assign(templateJson, JSON.parse((res.data as any).templateJson))
    }
  } catch (e: any) {
    message.error('加载模板失败: ' + (e.message || '未知错误'))
  }
}

function saveSnapshot() {
  undoStack.value.push(JSON.stringify(templateJson))
  if (undoStack.value.length > 50) undoStack.value.shift()
  redoStack.value = []
}

function undo() {
  if (undoStack.value.length === 0) return
  redoStack.value.push(JSON.stringify(templateJson))
  const prev = undoStack.value.pop()!
  Object.assign(templateJson, JSON.parse(prev))
}

function redo() {
  if (redoStack.value.length === 0) return
  undoStack.value.push(JSON.stringify(templateJson))
  const next = redoStack.value.pop()!
  Object.assign(templateJson, JSON.parse(next))
}

// ── 缩放 ────────────────────────────────────────────────

function setZoom(level: number) {
  zoomLevel.value = level
}

// ── 组件选择 ────────────────────────────────────────────

function selectComponent(idx: number, event: MouseEvent) {
  event.stopPropagation()
  selectedIndex.value = idx
  activeLayerIndex.value = idx
}

function deselectAll() {
  selectedIndex.value = -1
  contextMenuVisible.value = false
}

// ── 拖放 ────────────────────────────────────────────────

function onDragStart(event: DragEvent, compType: typeof componentTypes[0]) {
  event.dataTransfer?.setData('text/plain', compType.type)
}

function onDrop(event: DragEvent) {
  const type = event.dataTransfer?.getData('text/plain')
  if (!type) return

  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const scale = zoomLevel.value / 100
  const pxPerMm = 3.75 * scale
  const x = Math.round((event.clientX - rect.left - templateJson.marginLeft * pxPerMm) / pxPerMm)
  const y = Math.round((event.clientY - rect.top - templateJson.marginTop * pxPerMm) / pxPerMm)

  saveSnapshot()

  const newComp: DesignerComponent = {
    type,
    x: Math.max(0, x),
    y: Math.max(0, y),
    w: type === 'line' ? 150 : 50,
    h: type === 'line' ? 1 : 20,
    fontSize: 12,
    fontWeight: 'normal',
    textAlign: 'left',
  }

  if (type === 'label') newComp.content = '新标签'
  if (type === 'field') newComp.field = 'fieldName'
  if (type === 'table') {
    newComp.columns = [{ header: '列1', field: 'col1' }]
    newComp.w = 150
    newComp.h = 50
  }
  if (type === 'image') {
    newComp.w = 60
    newComp.h = 60
  }

  templateJson.components.push(newComp)
  selectedIndex.value = templateJson.components.length - 1
}

// ── 拖拽移动 ────────────────────────────────────────────

let dragState: { idx: number; startX: number; startY: number; compX: number; compY: number } | null = null
let resizeState: { idx: number; startX: number; startY: number; compW: number; compH: number } | null = null

function onComponentMouseDown(idx: number, event: MouseEvent) {
  if (event.button !== 0) return
  const comp = templateJson.components[idx]
  dragState = {
    idx,
    startX: event.clientX,
    startY: event.clientY,
    compX: comp.x,
    compY: comp.y,
  }
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
}

function onDragMove(event: MouseEvent) {
  if (!dragState) return
  const scale = zoomLevel.value / 100
  const pxPerMm = 3.75 * scale
  const comp = templateJson.components[dragState.idx]
  comp.x = Math.max(0, dragState.compX + (event.clientX - dragState.startX) / pxPerMm)
  comp.y = Math.max(0, dragState.compY + (event.clientY - dragState.startY) / pxPerMm)
}

function onDragEnd() {
  if (dragState) {
    saveSnapshot()
    dragState = null
  }
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}

// ── 调整大小 ────────────────────────────────────────────

function startResize(event: MouseEvent, idx: number) {
  const comp = templateJson.components[idx]
  resizeState = {
    idx,
    startX: event.clientX,
    startY: event.clientY,
    compW: comp.w,
    compH: comp.h,
  }
  document.addEventListener('mousemove', onResizeMove)
  document.addEventListener('mouseup', onResizeEnd)
}

function onResizeMove(event: MouseEvent) {
  if (!resizeState) return
  const scale = zoomLevel.value / 100
  const pxPerMm = 3.75 * scale
  const comp = templateJson.components[resizeState.idx]
  comp.w = Math.max(10, resizeState.compW + (event.clientX - resizeState.startX) / pxPerMm)
  comp.h = Math.max(5, resizeState.compH + (event.clientY - resizeState.startY) / pxPerMm)
}

function onResizeEnd() {
  if (resizeState) {
    saveSnapshot()
    resizeState = null
  }
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeEnd)
}

watch(selectedIndex, (idx) => {
  if (idx >= 0) {
    // Re-bind drag for newly selected component
    nextTick(() => {
      // The @mousedown.stop is already bound in template
    })
  }
})

// ── 上下文菜单 ──────────────────────────────────────────

function onContextMenu(event: MouseEvent, idx: number) {
  event.preventDefault()
  contextMenuIdx.value = idx
  contextMenuPos.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

function onContextMenuClick({ key }: { key: string }) {
  const idx = contextMenuIdx.value
  if (idx < 0) return

  switch (key) {
    case 'delete':
      deleteComponent(idx)
      break
    case 'duplicate':
      duplicateComponent(idx)
      break
    case 'bringFront': {
      const comp = templateJson.components.splice(idx, 1)[0]
      templateJson.components.push(comp)
      saveSnapshot()
      break
    }
    case 'sendBack': {
      const comp = templateJson.components.splice(idx, 1)[0]
      templateJson.components.unshift(comp)
      saveSnapshot()
      break
    }
  }
  contextMenuVisible.value = false
}

function handleGlobalClick() {
  contextMenuVisible.value = false
}

function handleKeydown(event: KeyboardEvent) {
  if (event.ctrlKey && event.key === 'z') {
    event.preventDefault()
    undo()
  }
  if (event.ctrlKey && event.key === 'y') {
    event.preventDefault()
    redo()
  }
  if (event.key === 'Delete' && selectedIndex.value >= 0) {
    deleteComponent(selectedIndex.value)
  }
}

// ── 组件操作 ────────────────────────────────────────────

function deleteComponent(idx: number) {
  if (idx < 0 || idx >= templateJson.components.length) return
  saveSnapshot()
  templateJson.components.splice(idx, 1)
  if (selectedIndex.value === idx) selectedIndex.value = -1
  if (selectedIndex.value > idx) selectedIndex.value--
}

function duplicateComponent(idx: number) {
  saveSnapshot()
  const comp = JSON.parse(JSON.stringify(templateJson.components[idx]))
  comp.y += 10
  comp.x += 10
  templateJson.components.push(comp)
  selectedIndex.value = templateJson.components.length - 1
}

// ── 表格列编辑 ──────────────────────────────────────────

function addColumn() {
  if (!selectedComponent.value) return
  if (!selectedComponent.value.columns) selectedComponent.value.columns = []
  selectedComponent.value.columns.push({ header: '新列', field: 'newField' })
}

function removeColumn(idx: number) {
  if (!selectedComponent.value?.columns) return
  selectedComponent.value.columns.splice(idx, 1)
}

// ── 属性变更 ────────────────────────────────────────────

function onPropChange() {
  saveSnapshot()
}

function onLayerSelect(idx: number) {
  selectedIndex.value = idx
}

// ── 纸张设置 ────────────────────────────────────────────

function getComponentStyle(comp: DesignerComponent) {
  const scale = zoomLevel.value / 100
  const pxPerMm = 3.75 * scale
  return {
    left: `${comp.x * pxPerMm}px`,
    top: `${comp.y * pxPerMm}px`,
    width: `${comp.w * pxPerMm}px`,
    height: `${comp.h * pxPerMm}px`,
    fontSize: comp.fontSize ? `${comp.fontSize * pxPerMm / 3.75}px` : undefined,
    fontWeight: comp.fontWeight,
    textAlign: comp.textAlign,
    cursor: 'move',
  }
}

// ── 保存与发布 ──────────────────────────────────────────

function buildTemplateJson(): string {
  return JSON.stringify(templateJson)
}

const defaultFormData = {
  pageCode: '',
  templateName: '',
  paperSize: 'A4',
  paperWidth: 210,
  paperHeight: 297,
  margins: { top: 10, bottom: 10, left: 10, right: 10 },
}

const formData = reactive({ ...defaultFormData })

async function handleSaveDraft() {
  if (!templateName.value) {
    const name = await showNamePrompt()
    if (!name) return
    templateName.value = name
  }

  try {
    const payload = {
      pageCode: pageCode.value || 'common',
      templateName: templateName.value,
      templateJson: buildTemplateJson(),
      paperSize: templateJson.paperSize,
      paperWidth: templateJson.paperWidth,
      paperHeight: templateJson.paperHeight,
      margins: {
        top: templateJson.marginTop,
        bottom: templateJson.marginBottom,
        left: templateJson.marginLeft,
        right: templateJson.marginRight,
      },
    }

    if (isEdit.value && templateId.value) {
      await printingApi.updateTemplate(templateId.value, payload as any)
      message.success('模板已更新')
    } else {
      const res = await printingApi.createTemplate(payload as any)
      templateId.value = (res.data as any).templateId
      isEdit.value = true
      message.success('模板已保存')
    }
  } catch (e: any) {
    message.error('保存失败: ' + (e.message || '未知错误'))
  }
}

async function handlePublish() {
  if (!templateId.value) {
    await handleSaveDraft()
    if (!templateId.value) return
  }

  Modal.confirm({
    title: '确认发布',
    content: '发布后模板将可用于打印任务，确定发布吗？',
    onOk: async () => {
      try {
        await printingApi.publishTemplate(templateId.value!)
        message.success('模板已发布')
        router.push('/printing/template')
      } catch (e: any) {
        message.error('发布失败: ' + (e.message || '未知错误'))
      }
    },
  })
}

async function handlePreview() {
  try {
    const res = await printingApi.renderTemplate({
      templateJson: buildTemplateJson(),
      dataJson: '{}',
    })
    previewHtml.value = (res.data as any).html || '<p>渲染失败</p>'
    previewVisible.value = true
  } catch (e: any) {
    // Fallback: generate basic preview locally
    previewHtml.value = generateLocalPreview()
    previewVisible.value = true
  }
}

function generateLocalPreview(): string {
  const pxPerMm = 3.75
  const w = templateJson.paperSize === 'CUSTOM' ? templateJson.paperWidth * pxPerMm : 210 * pxPerMm
  const h = templateJson.paperSize === 'CUSTOM' ? templateJson.paperHeight * pxPerMm : 297 * pxPerMm

  let componentsHtml = ''
  for (const comp of templateJson.components) {
    const style = `position: absolute; left: ${comp.x * pxPerMm}px; top: ${comp.y * pxPerMm}px; width: ${comp.w * pxPerMm}px; height: ${comp.h * pxPerMm}px; font-size: ${(comp.fontSize || 12)}pt; font-weight: ${comp.fontWeight || 'normal'}; text-align: ${comp.textAlign || 'left'};`
    if (comp.type === 'label') {
      componentsHtml += `<div style="${style}">${comp.content || ''}</div>`
    } else if (comp.type === 'field') {
      componentsHtml += `<div style="${style}border: 1px dashed #ccc; padding: 2px;">{{${comp.field}}}</div>`
    } else if (comp.type === 'table') {
      let tableHtml = '<table style="width:100%;border-collapse:collapse;font-size:10pt;"><thead><tr>'
      for (const col of (comp.columns || [])) {
        tableHtml += `<th style="border:1px solid #000;padding:4px;">${col.header}</th>`
      }
      tableHtml += '</tr></thead><tbody><tr>'
      for (const col of (comp.columns || [])) {
        tableHtml += `<td style="border:1px solid #000;padding:4px;">{{${col.field}}}</td>`
      }
      tableHtml += '</tr></tbody></table>'
      componentsHtml += `<div style="${style}">${tableHtml}</div>`
    } else if (comp.type === 'barcode') {
      componentsHtml += `<div style="${style}border:1px solid #999; display:flex; align-items:center; justify-content:center; font-size:10px;">[条码] ${comp.field || ''}</div>`
    } else if (comp.type === 'qrcode') {
      componentsHtml += `<div style="${style}border:1px solid #999; display:flex; align-items:center; justify-content:center; font-size:10px;">[二维码] ${comp.field || ''}</div>`
    } else if (comp.type === 'image') {
      componentsHtml += `<div style="${style}border:1px dashed #ccc; display:flex; align-items:center; justify-content:center; overflow:hidden;">${comp.src ? `<img src="${comp.src}" style="max-width:100%;max-height:100%;">` : '[图片]'}</div>`
    } else if (comp.type === 'line') {
      componentsHtml += `<div style="${style}border-top:1px solid #000;"></div>`
    }
  }

  return `<!DOCTYPE html><html><head><style>
    @page { margin: 0; }
    body { margin: 0; font-family: sans-serif; }
    .page { position: relative; width: ${w}px; min-height: ${h}px;
            padding: ${templateJson.marginTop * pxPerMm}px ${templateJson.marginRight * pxPerMm}px ${templateJson.marginBottom * pxPerMm}px ${templateJson.marginLeft * pxPerMm}px;
            box-sizing: border-box; }
  </style></head><body><div class="page">${componentsHtml}</div></body></html>`
}

async function showNamePrompt(): Promise<string | null> {
  return new Promise((resolve) => {
    let inputValue = ''
    Modal.confirm({
      title: '模板名称',
      content: h('div', {}, [
        h('p', {}, '请为模板输入一个名称：'),
        h('a-input', {
          value: inputValue,
          'onUpdate:value': (v: string) => { inputValue = v },
          placeholder: '输入模板名称',
          style: { marginTop: '8px' },
          ref: 'inputRef',
        }),
      ]),
      onOk: () => resolve(inputValue || null),
      onCancel: () => resolve(null),
    })
  })
}

function h(tag: string, attrs: any, children?: any) {
  // Simple stub - for Modal content rendering
  return { tag, attrs, children }
}
</script>

<style scoped>
.template-designer {
  height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.toolbar-left { flex: 0 0 auto; }
.toolbar-center { flex: 1; text-align: center; }
.toolbar-right { flex: 0 0 auto; }

.designer-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧面板 */
.component-panel {
  width: 180px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  overflow-y: auto;
  flex-shrink: 0;
}

.panel-header {
  padding: 8px 12px;
  font-weight: 600;
  font-size: 13px;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.panel-body {
  padding: 8px;
}

.component-item {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  margin-bottom: 4px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  cursor: grab;
  transition: all 0.2s;
  user-select: none;
}

.component-item:hover {
  border-color: #1890ff;
  background: #e6f7ff;
}

.component-item:active {
  cursor: grabbing;
}

.comp-icon {
  margin-right: 8px;
  font-size: 16px;
  color: #666;
}

.comp-label {
  font-size: 12px;
}

/* 画布区 */
.canvas-area {
  flex: 1;
  overflow: auto;
  padding: 24px;
  display: flex;
  justify-content: center;
  background: #e8e8e8;
}

.designer-canvas {
  position: relative;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.designer-canvas.show-grid {
  background-image:
    linear-gradient(rgba(0,0,0,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,0,0,0.05) 1px, transparent 1px);
  background-size: 18.75px 18.75px; /* 5mm at 3.75px/mm */
}

.paper-size-label {
  position: absolute;
  top: -20px;
  left: 0;
  font-size: 11px;
  color: #999;
}

.empty-canvas-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #ccc;
  font-size: 14px;
  text-align: center;
  pointer-events: none;
}

/* 组件样式 */
.designer-component {
  position: absolute;
  border: 1px solid transparent;
  box-sizing: border-box;
  overflow: hidden;
}

.designer-component:hover {
  border-color: #1890ff;
  outline: 1px dashed #1890ff;
  outline-offset: 1px;
}

.designer-component.selected {
  border-color: #1890ff;
  background: rgba(24, 144, 255, 0.04);
  outline: 2px solid #1890ff;
  outline-offset: 1px;
}

.comp-content {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.label-content {
  font-size: inherit;
}

.field-content {
  color: #666;
  font-style: italic;
  border-bottom: 1px dashed #ccc;
}

.table-content {
  overflow: auto;
}

.barcode-content {
  justify-content: center;
}

.image-content {
  justify-content: center;
  color: #999;
  font-size: 12px;
}

.resize-handle {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #1890ff;
  cursor: se-resize;
  border: 1px solid #fff;
}

/* 右侧属性面板 */
.property-panel {
  width: 280px;
  background: #fff;
  border-left: 1px solid #e8e8e8;
  overflow-y: auto;
  flex-shrink: 0;
}

.property-panel .panel-body {
  padding: 12px;
}

.column-editor {
  margin-top: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
}

.column-item {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}
</style>
