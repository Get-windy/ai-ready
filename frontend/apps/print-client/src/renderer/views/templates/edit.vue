<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

interface TemplateElement {
  id: string
  type: 'text' | 'image' | 'table' | 'line' | 'barcode' | 'qrcode' | 'variable'
  x: number
  y: number
  width: number
  height: number
  content?: string
  fontSize?: number
  fontWeight?: string
  textAlign?: string
  color?: string
  borderColor?: string
  borderWidth?: number
  variableKey?: string
  tableColumns?: TableColumn[]
  src?: string
}

interface TableColumn {
  key: string
  label: string
  width: number
}

interface PrintTemplate {
  id: string
  name: string
  type: 'order' | 'invoice' | 'contract' | 'label' | 'report'
  width: number
  height: number
  orientation: 'portrait' | 'landscape'
  paperSize: 'A4' | 'A5' | 'Letter' | 'Custom'
  elements: TemplateElement[]
  variables: VariableDefinition[]
}

interface VariableDefinition {
  key: string
  label: string
  type: 'text' | 'number' | 'date' | 'list' | 'image'
  defaultValue?: string
  source?: string
}

const templateId = computed(() => route.params.id as string)
const isNewTemplate = computed(() => templateId.value === 'new' || !templateId.value)

const template = ref<PrintTemplate>({
  id: '',
  name: '',
  type: 'order',
  width: 210,
  height: 297,
  orientation: 'portrait',
  paperSize: 'A4',
  elements: [],
  variables: []
})

const selectedElement = ref<TemplateElement | null>(null)
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const canvasRef = ref<HTMLDivElement | null>(null)
const showVariablePanel = ref(false)
const showPreview = ref(false)

const paperSizes = {
  A4: { width: 210, height: 297 },
  A5: { width: 148, height: 210 },
  Letter: { width: 216, height: 279 },
  Custom: { width: 100, height: 100 }
}

const elementTypes = [
  { type: 'text', label: '文本', icon: '📝' },
  { type: 'variable', label: '变量', icon: '🔗' },
  { type: 'table', label: '表格', icon: '📊' },
  { type: 'image', label: '图片', icon: '🖼️' },
  { type: 'line', label: '线条', icon: '➖' },
  { type: 'barcode', label: '条码', icon: '📱' },
  { type: 'qrcode', label: '二维码', icon: '🔲' }
]

const defaultVariables: VariableDefinition[] = [
  { key: 'order_no', label: '订单编号', type: 'text', source: 'order.orderNo' },
  { key: 'customer_name', label: '客户名称', type: 'text', source: 'customer.name' },
  { key: 'order_date', label: '订单日期', type: 'date', source: 'order.date' },
  { key: 'total_amount', label: '总金额', type: 'number', source: 'order.totalAmount' },
  { key: 'items', label: '商品明细', type: 'list', source: 'order.items' },
  { key: 'company_logo', label: '公司Logo', type: 'image', source: 'company.logo' },
  { key: 'company_name', label: '公司名称', type: 'text', source: 'company.name' },
  { key: 'company_address', label: '公司地址', type: 'text', source: 'company.address' },
  { key: 'contact_phone', label: '联系电话', type: 'text', source: 'company.phone' },
  { key: 'salesperson', label: '销售人员', type: 'text', source: 'order.salesperson' },
  { key: 'remark', label: '备注', type: 'text', source: 'order.remark' },
  { key: 'signature', label: '签名', type: 'image', source: 'signature' }
]

const scale = ref(1)
const canvasStyle = computed(() => ({
  width: `${template.value.width * scale.value}mm`,
  height: `${template.value.height * scale.value}mm`,
  transform: `scale(${scale.value})`
}))

onMounted(async () => {
  if (!isNewTemplate.value) {
    await loadTemplate()
  } else {
    template.value.variables = [...defaultVariables]
  }
})

const loadTemplate = async () => {
  try {
    const data = await window.electronAPI.templates.getTemplate(templateId.value)
    if (data) {
      template.value = data
    }
  } catch (err) {
    console.error('加载模板失败:', err)
  }
}

const saveTemplate = async () => {
  try {
    if (isNewTemplate.value) {
      const result = await window.electronAPI.templates.createTemplate(template.value)
      template.value.id = result.id
      router.replace(`/templates/${result.id}/edit`)
    } else {
      await window.electronAPI.templates.updateTemplate(template.value)
    }
    alert('保存成功')
  } catch (err) {
    alert('保存失败: ' + err)
  }
}

const addElement = (type: string) => {
  const newElement: TemplateElement = {
    id: `el_${Date.now()}`,
    type: type as any,
    x: 20,
    y: 20,
    width: type === 'table' ? 170 : type === 'line' ? 100 : 50,
    height: type === 'table' ? 50 : type === 'line' ? 1 : 20,
    content: type === 'text' ? '示例文本' : '',
    fontSize: 12,
    fontWeight: 'normal',
    textAlign: 'left',
    color: '#333333',
    borderColor: '#cccccc',
    borderWidth: 1
  }
  
  if (type === 'table') {
    newElement.tableColumns = [
      { key: 'product_name', label: '商品名称', width: 60 },
      { key: 'quantity', label: '数量', width: 30 },
      { key: 'price', label: '单价', width: 40 },
      { key: 'amount', label: '金额', width: 40 }
    ]
  }
  
  template.value.elements.push(newElement)
  selectedElement.value = newElement
}

const addVariableElement = (variable: VariableDefinition) => {
  const newElement: TemplateElement = {
    id: `el_${Date.now()}`,
    type: 'variable',
    x: 20,
    y: template.value.elements.length * 25 + 20,
    width: 80,
    height: 15,
    content: `{{${variable.key}}}`,
    variableKey: variable.key,
    fontSize: 12,
    fontWeight: 'normal',
    textAlign: 'left',
    color: '#333333'
  }
  
  template.value.elements.push(newElement)
  selectedElement.value = newElement
  showVariablePanel.value = false
}

const deleteElement = (element: TemplateElement) => {
  template.value.elements = template.value.elements.filter(el => el.id !== element.id)
  if (selectedElement.value?.id === element.id) {
    selectedElement.value = null
  }
}

const handleElementMouseDown = (element: TemplateElement, event: MouseEvent) => {
  event.stopPropagation()
  selectedElement.value = element
  isDragging.value = true
  
  const rect = canvasRef.value?.getBoundingClientRect()
  if (rect) {
    dragOffset.value = {
      x: event.clientX - rect.left - element.x * scale.value,
      y: event.clientY - rect.top - element.y * scale.value
    }
  }
}

const handleMouseMove = (event: MouseEvent) => {
  if (!isDragging.value || !selectedElement.value) return
  
  const rect = canvasRef.value?.getBoundingClientRect()
  if (rect) {
    const newX = (event.clientX - rect.left - dragOffset.value.x) / scale.value
    const newY = (event.clientY - rect.top - dragOffset.value.y) / scale.value
    
    selectedElement.value.x = Math.max(0, Math.min(template.value.width - selectedElement.value.width, newX))
    selectedElement.value.y = Math.max(0, Math.min(template.value.height - selectedElement.value.height, newY))
  }
}

const handleMouseUp = () => {
  isDragging.value = false
}

const handleCanvasClick = () => {
  selectedElement.value = null
}

const updatePaperSize = () => {
  const size = paperSizes[template.value.paperSize]
  if (template.value.paperSize !== 'Custom') {
    template.value.width = size.width
    template.value.height = size.height
  }
  if (template.value.orientation === 'landscape') {
    [template.value.width, template.value.height] = [template.value.height, template.value.width]
  }
}

const toggleOrientation = () => {
  template.value.orientation = template.value.orientation === 'portrait' ? 'landscape' : 'portrait'
  [template.value.width, template.value.height] = [template.value.height, template.value.width]
}

const previewTemplate = () => {
  showPreview.value = true
}

const exportTemplate = () => {
  const json = JSON.stringify(template.value, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${template.value.name || 'template'}.json`
  a.click()
  URL.revokeObjectURL(url)
}

const getElementStyle = (element: TemplateElement) => {
  const style: any = {
    position: 'absolute',
    left: `${element.x}mm`,
    top: `${element.y}mm`,
    width: `${element.width}mm`,
    height: element.type === 'line' ? `${element.borderWidth || 1}px` : `${element.height}mm`,
    cursor: 'move',
    userSelect: 'none'
  }
  
  if (element.type === 'text' || element.type === 'variable') {
    style.fontSize = `${element.fontSize}pt`
    style.fontWeight = element.fontWeight
    style.textAlign = element.textAlign
    style.color = element.color
    style.lineHeight = `${element.height}mm`
    style.overflow = 'hidden'
  }
  
  if (element.type === 'table') {
    style.border = `${element.borderWidth}px solid ${element.borderColor}`
    style.fontSize = `${element.fontSize}pt`
  }
  
  if (element.type === 'line') {
    style.backgroundColor = element.borderColor || '#333333'
  }
  
  if (element.type === 'barcode' || element.type === 'qrcode') {
    style.border = `${element.borderWidth}px solid ${element.borderColor}`
    style.display = 'flex'
    style.alignItems = 'center'
    style.justifyContent = 'center'
  }
  
  if (selectedElement.value?.id === element.id) {
    style.outline = '2px solid #1988fa'
    style.outlineOffset = '1px'
  }
  
  return style
}

const goBack = () => {
  router.push('/templates')
}
</script>

<template>
  <div class="template-editor">
    <div class="editor-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">← 返回</button>
        <input 
          v-model="template.name"
          type="text"
          class="template-name-input"
          placeholder="模板名称"
        />
        <select v-model="template.type" class="type-select">
          <option value="order">订单模板</option>
          <option value="invoice">发票模板</option>
          <option value="contract">合同模板</option>
          <option value="label">标签模板</option>
          <option value="report">报表模板</option>
        </select>
      </div>
      
      <div class="header-right">
        <button class="preview-btn" @click="previewTemplate">预览</button>
        <button class="export-btn" @click="exportTemplate">导出</button>
        <button class="save-btn" @click="saveTemplate">保存</button>
      </div>
    </div>
    
    <div class="editor-body">
      <div class="left-panel">
        <div class="panel-section">
          <div class="section-title">页面设置</div>
          <div class="section-content">
            <div class="setting-row">
              <label>纸张大小</label>
              <select v-model="template.paperSize" @change="updatePaperSize">
                <option value="A4">A4</option>
                <option value="A5">A5</option>
                <option value="Letter">Letter</option>
                <option value="Custom">自定义</option>
              </select>
            </div>
            <div class="setting-row">
              <label>方向</label>
              <button class="orientation-btn" @click="toggleOrientation">
                {{ template.orientation === 'portrait' ? '纵向 ↕' : '横向 ↔' }}
              </button>
            </div>
            <div class="setting-row">
              <label>宽度(mm)</label>
              <input 
                v-model.number="template.width"
                type="number"
                min="50"
                max="500"
                :disabled="template.paperSize !== 'Custom'"
              />
            </div>
            <div class="setting-row">
              <label>高度(mm)</label>
              <input 
                v-model.number="template.height"
                type="number"
                min="50"
                max="500"
                :disabled="template.paperSize !== 'Custom'"
              />
            </div>
            <div class="setting-row">
              <label>缩放</label>
              <input 
                v-model.number="scale"
                type="range"
                min="0.5"
                max="2"
                step="0.1"
              />
              <span>{{ scale.toFixed(1) }}x</span>
            </div>
          </div>
        </div>
        
        <div class="panel-section">
          <div class="section-title">添加元素</div>
          <div class="element-buttons">
            <button 
              v-for="et in elementTypes"
              :key="et.type"
              class="element-btn"
              @click="addElement(et.type)"
            >
              <span class="icon">{{ et.icon }}</span>
              <span class="label">{{ et.label }}</span>
            </button>
          </div>
        </div>
        
        <div class="panel-section">
          <div class="section-title">
            <span>变量列表</span>
            <button class="add-var-btn" @click="showVariablePanel = true">+</button>
          </div>
          <div class="variable-list">
            <div 
              v-for="varItem in template.variables"
              :key="varItem.key"
              class="variable-item"
              @click="addVariableElement(varItem)"
            >
              <span class="var-key">{{ varItem.key }}</span>
              <span class="var-label">{{ varItem.label }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="canvas-container">
        <div 
          ref="canvasRef"
          class="template-canvas"
          :style="canvasStyle"
          @click="handleCanvasClick"
          @mousemove="handleMouseMove"
          @mouseup="handleMouseUp"
          @mouseleave="handleMouseUp"
        >
          <div 
            v-for="element in template.elements"
            :key="element.id"
            class="template-element"
            :style="getElementStyle(element)"
            @mousedown="handleElementMouseDown(element, $event)"
          >
            <template v-if="element.type === 'text'">
              <span>{{ element.content }}</span>
            </template>
            
            <template v-if="element.type === 'variable'">
              <span class="variable-text">{{ element.content }}</span>
            </template>
            
            <template v-if="element.type === 'table'">
              <table class="element-table">
                <thead>
                  <tr>
                    <th 
                      v-for="col in element.tableColumns"
                      :key="col.key"
                      :style="{ width: col.width + 'mm' }"
                    >
                      {{ col.label }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td v-for="col in element.tableColumns" :key="col.key">
                      {{ '{{item.' + col.key + '}}' }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </template>
            
            <template v-if="element.type === 'barcode'">
              <div class="barcode-placeholder">条码区域</div>
            </template>
            
            <template v-if="element.type === 'qrcode'">
              <div class="qrcode-placeholder">二维码</div>
            </template>
            
            <template v-if="element.type === 'image'">
              <div class="image-placeholder">图片区域</div>
            </template>
            
            <button 
              v-if="selectedElement?.id === element.id"
              class="delete-element-btn"
              @click.stop="deleteElement(element)"
            >
              ×
            </button>
          </div>
        </div>
      </div>
      
      <div class="right-panel" v-if="selectedElement">
        <div class="panel-section">
          <div class="section-title">元素属性</div>
          <div class="section-content">
            <div class="setting-row">
              <label>X位置(mm)</label>
              <input v-model.number="selectedElement.x" type="number" min="0" />
            </div>
            <div class="setting-row">
              <label>Y位置(mm)</label>
              <input v-model.number="selectedElement.y" type="number" min="0" />
            </div>
            <div class="setting-row">
              <label>宽度(mm)</label>
              <input v-model.number="selectedElement.width" type="number" min="10" />
            </div>
            <div class="setting-row">
              <label>高度(mm)</label>
              <input v-model.number="selectedElement.height" type="number" min="5" />
            </div>
            
            <template v-if="selectedElement.type === 'text' || selectedElement.type === 'variable'">
              <div class="setting-row">
                <label>内容</label>
                <input v-model="selectedElement.content" type="text" />
              </div>
              <div class="setting-row">
                <label>字号(pt)</label>
                <input v-model.number="selectedElement.fontSize" type="number" min="8" max="72" />
              </div>
              <div class="setting-row">
                <label>字重</label>
                <select v-model="selectedElement.fontWeight">
                  <option value="normal">正常</option>
                  <option value="bold">粗体</option>
                </select>
              </div>
              <div class="setting-row">
                <label>对齐</label>
                <select v-model="selectedElement.textAlign">
                  <option value="left">左对齐</option>
                  <option value="center">居中</option>
                  <option value="right">右对齐</option>
                </select>
              </div>
              <div class="setting-row">
                <label>颜色</label>
                <input v-model="selectedElement.color" type="color" />
              </div>
            </template>
            
            <template v-if="selectedElement.type === 'table'">
              <div class="setting-row">
                <label>边框颜色</label>
                <input v-model="selectedElement.borderColor" type="color" />
              </div>
              <div class="setting-row">
                <label>边框宽度</label>
                <input v-model.number="selectedElement.borderWidth" type="number" min="0" max="5" />
              </div>
              <div class="table-columns-editor">
                <div class="columns-title">表格列</div>
                <div 
                  v-for="(col, index) in selectedElement.tableColumns"
                  :key="index"
                  class="column-row"
                >
                  <input v-model="col.label" placeholder="列名" />
                  <input v-model.number="col.width" type="number" placeholder="宽度" />
                  <button @click="selectedElement.tableColumns?.splice(index, 1)">×</button>
                </div>
                <button 
                  class="add-column-btn"
                  @click="selectedElement.tableColumns?.push({ key: 'new', label: '新列', width: 30 })"
                >
                  + 添加列
                </button>
              </div>
            </template>
            
            <template v-if="selectedElement.type === 'line'">
              <div class="setting-row">
                <label>线条颜色</label>
                <input v-model="selectedElement.borderColor" type="color" />
              </div>
              <div class="setting-row">
                <label>线条宽度</label>
                <input v-model.number="selectedElement.borderWidth" type="number" min="1" max="10" />
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
    
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showPreview" class="preview-overlay" @click="showPreview = false">
          <div class="preview-panel" @click.stop>
            <div class="preview-header">
              <span>模板预览</span>
              <button @click="showPreview = false">×</button>
            </div>
            <div class="preview-content">
              <div 
                class="preview-canvas"
                :style="{ width: template.width + 'mm', height: template.height + 'mm' }"
              >
                <div 
                  v-for="element in template.elements"
                  :key="element.id"
                  :style="getElementStyle(element)"
                >
                  <template v-if="element.type === 'text'">
                    {{ element.content }}
                  </template>
                  <template v-if="element.type === 'variable'">
                    <span style="color: #1988fa;">[{{ element.variableKey }}]</span>
                  </template>
                  <template v-if="element.type === 'table'">
                    <table style="width: 100%; border-collapse: collapse;">
                      <tr>
                        <th 
                          v-for="col in element.tableColumns"
                          :key="col.key"
                          style="border: 1px solid #ccc; padding: 4px;"
                        >
                          {{ col.label }}
                        </th>
                      </tr>
                      <tr>
                        <td 
                          v-for="col in element.tableColumns"
                          :key="col.key"
                          style="border: 1px solid #ccc; padding: 4px; color: #1988fa;"
                        >
                          [{{ col.key }}]
                        </td>
                      </tr>
                    </table>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style lang="scss" scoped>
.template-editor {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #ebedf0;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .back-btn {
      padding: 8px 16px;
      background: #f7f8fa;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      cursor: pointer;
      
      &:hover {
        background: #ebedf0;
      }
    }
    
    .template-name-input {
      padding: 8px 12px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      font-size: 16px;
      width: 200px;
      
      &:focus {
        border-color: #1988fa;
      }
    }
    
    .type-select {
      padding: 8px 12px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
    }
  }
  
  .header-right {
    display: flex;
    gap: 8px;
    
    .preview-btn, .export-btn {
      padding: 8px 16px;
      background: #f7f8fa;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      cursor: pointer;
      
      &:hover {
        background: #ebedf0;
      }
    }
    
    .save-btn {
      padding: 8px 16px;
      background: #1988fa;
      color: #fff;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      
      &:hover {
        background: #0e7cd3;
      }
    }
  }
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.left-panel, .right-panel {
  width: 280px;
  background: #fff;
  border-right: 1px solid #ebedf0;
  overflow-y: auto;
  padding: 16px;
}

.right-panel {
  border-right: none;
  border-left: 1px solid #ebedf0;
}

.panel-section {
  margin-bottom: 20px;
  
  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .add-var-btn {
      padding: 2px 8px;
      background: #1988fa;
      color: #fff;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
    }
  }
  
  .section-content {
    .setting-row {
      display: flex;
      align-items: center;
      margin-bottom: 8px;
      
      label {
        width: 80px;
        font-size: 12px;
        color: #666;
      }
      
      input[type="number"], input[type="text"], select {
        flex: 1;
        padding: 6px 8px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        font-size: 12px;
        
        &:disabled {
          background: #f7f8fa;
        }
      }
      
      input[type="color"] {
        width: 40px;
        height: 24px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        cursor: pointer;
      }
      
      input[type="range"] {
        flex: 1;
      }
      
      .orientation-btn {
        flex: 1;
        padding: 6px 8px;
        background: #f7f8fa;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        cursor: pointer;
        font-size: 12px;
        
        &:hover {
          background: #ebedf0;
        }
      }
    }
  }
}

.element-buttons {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  
  .element-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 12px;
    background: #f7f8fa;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
    
    &:hover {
      background: #1988fa;
      color: #fff;
      border-color: #1988fa;
    }
    
    .icon {
      font-size: 20px;
      margin-bottom: 4px;
    }
    
    .label {
      font-size: 12px;
    }
  }
}

.variable-list {
  .variable-item {
    display: flex;
    justify-content: space-between;
    padding: 8px 12px;
    background: #f7f8fa;
    border-radius: 4px;
    margin-bottom: 4px;
    cursor: pointer;
    
    &:hover {
      background: #1988fa;
      color: #fff;
    }
    
    .var-key {
      font-size: 12px;
      font-weight: 600;
    }
    
    .var-label {
      font-size: 12px;
      color: #969799;
    }
  }
}

.canvas-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  overflow: auto;
  background: #e8eaed;
}

.template-canvas {
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  position: relative;
  transform-origin: center center;
}

.template-element {
  &:hover {
    outline: 1px dashed #1988fa;
  }
  
  .delete-element-btn {
    position: absolute;
    top: -8px;
    right: -8px;
    width: 20px;
    height: 20px;
    background: #f44;
    color: #fff;
    border: none;
    border-radius: 50%;
    cursor: pointer;
    font-size: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.variable-text {
  color: #1988fa;
  font-style: italic;
}

.element-table {
  width: 100%;
  border-collapse: collapse;
  
  th, td {
    border: 1px solid #ccc;
    padding: 4px;
    font-size: 10pt;
    text-align: center;
  }
  
  th {
    background: #f7f8fa;
    font-weight: 600;
  }
}

.barcode-placeholder, .qrcode-placeholder, .image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #969799;
  font-size: 10px;
  background: #f7f8fa;
}

.table-columns-editor {
  margin-top: 12px;
  padding: 12px;
  background: #f7f8fa;
  border-radius: 4px;
  
  .columns-title {
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 8px;
  }
  
  .column-row {
    display: flex;
    gap: 4px;
    margin-bottom: 4px;
    
    input {
      flex: 1;
      padding: 4px 8px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      font-size: 12px;
    }
    
    button {
      padding: 4px 8px;
      background: #f44;
      color: #fff;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }
  }
  
  .add-column-btn {
    width: 100%;
    padding: 6px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
  }
}

.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-panel {
  width: 90%;
  max-width: 800px;
  background: #fff;
  border-radius: 8px;
  
  .preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #ebedf0;
    
    span {
      font-size: 16px;
      font-weight: 600;
    }
    
    button {
      background: none;
      border: none;
      font-size: 20px;
      color: #969799;
      cursor: pointer;
    }
  }
  
  .preview-content {
    padding: 20px;
    display: flex;
    justify-content: center;
    overflow: auto;
    
    .preview-canvas {
      background: #fff;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      position: relative;
      transform: scale(0.7);
      transform-origin: top center;
    }
  }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>