<template>
  <div class="basic-form-page">
    <!-- 顶部操作栏 -->
    <div class="form-header">
      <div class="header-left">
        <a-button v-if="showBack" size="small" @click="handleBack">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
      </div>
      <div class="header-center">
        <h2 class="form-title">{{ title }}</h2>
      </div>
      <div class="header-right">
        <a-button size="small" :loading="saving" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
        <a-button v-if="showSubmit" size="small" type="primary" :loading="saving" @click="handleSubmit">
          <template #icon><SendOutlined /></template>
          提交
        </a-button>
      </div>
    </div>

    <!-- 表单内容区 -->
    <div class="form-body">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="24">
          <a-col v-for="field in fields" :key="field.name" :span="field.span || 12">
            <a-form-item :label="field.label" :required="field.required">
              <!-- 自定义插槽 -->
              <template v-if="field.type === 'slot' && field.slotName">
                <slot :name="field.slotName" :formData="formData" />
              </template>
              <!-- 输入框 -->
              <a-input
                v-else-if="field.type === 'input'"
                v-model:value="formData[field.name]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                size="small"
              />
              <!-- 下拉选择 -->
              <a-select
                v-else-if="field.type === 'select'"
                v-model:value="formData[field.name]"
                :placeholder="field.placeholder || `请选择${field.label}`"
                :show-search="field.showSearch !== false"
                :filter-option="filterOption"
                style="width: 100%"
                size="small"
              >
                <a-select-option v-for="opt in (field.options || [])" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </a-select-option>
              </a-select>
              <!-- 日期选择 -->
              <a-date-picker
                v-else-if="field.type === 'date'"
                v-model:value="formData[field.name]"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                size="small"
              />
              <!-- 数字输入 -->
              <a-input-number
                v-else-if="field.type === 'number'"
                v-model:value="formData[field.name]"
                :min="field.min ?? 0"
                :max="field.max"
                :precision="field.precision"
                style="width: 100%"
                size="small"
              />
              <!-- 文本域 -->
              <a-textarea
                v-else-if="field.type === 'textarea'"
                v-model:value="formData[field.name]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :rows="3"
                size="small"
              />
              <!-- 开关 -->
              <a-switch
                v-else-if="field.type === 'switch'"
                v-model:checked="formData[field.name]"
                size="small"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <!-- 额外内容插槽 -->
      <slot name="extra" />
    </div>

    <!-- 底部操作栏 -->
    <div class="form-footer">
      <div class="footer-left" />
      <div class="footer-right">
        <a-button size="large" :loading="saving" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          保存
          <span class="shortcut-hint">Ctrl+S</span>
        </a-button>
        <a-button v-if="showSubmit" type="primary" size="large" :loading="saving" @click="handleSubmit">
          <template #icon><SendOutlined /></template>
          提交
          <span class="shortcut-hint">Ctrl+Enter</span>
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { SaveOutlined, SendOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue'

export interface BasicField {
  name: string
  label: string
  type: 'input' | 'select' | 'date' | 'number' | 'textarea' | 'switch' | 'slot'
  placeholder?: string
  required?: boolean
  options?: { label: string; value: string | number }[]
  showSearch?: boolean
  span?: number
  slotName?: string
  precision?: number
  min?: number
  max?: number
}

interface FormApi {
  create: (data: any) => Promise<any>
  update?: (id: number, data: any) => Promise<any>
  getById?: (id: number) => Promise<any>
}

const props = withDefaults(defineProps<{
  title: string
  fields: BasicField[]
  api: FormApi
  redirectPath?: string
  showBack?: boolean
  showSubmit?: boolean
  /** 显式指定模式；不指定时根据路由参数自动推断 */
  mode?: 'create' | 'edit' | 'view'
}>(), {
  redirectPath: '',
  showBack: true,
  showSubmit: true,
  mode: undefined,
})

const router = useRouter()
const route = useRoute()
const formData = reactive<Record<string, any>>({})
const saving = ref(false)

// 根据路由参数自动推断模式：存在 id 参数则进入编辑模式
const effectiveMode = computed(() => {
  if (props.mode) return props.mode
  const editId = route.params.id || route.query.id
  return editId ? 'edit' : 'create'
})

// 初始化字段默认值
for (const field of props.fields) {
  formData[field.name] = field.type === 'number' ? 0 : field.type === 'switch' ? false : undefined
}

const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.children || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

function handleBack() {
  if (props.redirectPath) {
    router.push(props.redirectPath)
  } else {
    router.back()
  }
}

async function loadDetail(id: number) {
  if (!props.api.getById) return
  try {
    const data = await props.api.getById(id)
    if (data) Object.assign(formData, data)
  } catch (err: any) {
    message.error('加载详情失败: ' + (err?.message || ''))
  }
}

function validate(): boolean {
  for (const field of props.fields) {
    if (field.required && !formData[field.name] && formData[field.name] !== 0) {
      message.warning(`请填写${field.label}`)
      return false
    }
  }
  return true
}

async function handleSave() {
  if (!validate()) return
  saving.value = true
  try {
    if (effectiveMode.value === 'edit' && formData.id && props.api.update) {
      await props.api.update(formData.id, { ...formData })
    } else {
      await props.api.create({ ...formData })
    }
    message.success('保存成功')
    if (props.redirectPath) router.push(props.redirectPath)
  } catch (err: any) {
    message.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (!validate()) return
  saving.value = true
  try {
    const payload = { ...formData, status: 1 }
    if (effectiveMode.value === 'edit' && formData.id && props.api.update) {
      await props.api.update(formData.id, payload)
    } else {
      await props.api.create(payload)
    }
    message.success('提交成功')
    if (props.redirectPath) router.push(props.redirectPath)
  } catch (err: any) {
    message.error(err?.message || '提交失败')
  } finally {
    saving.value = false
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSave()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  // 编辑模式：加载详情（支持路由 params 和 query 两种方式）
  const editId = route.params.id || route.query.id
  if (effectiveMode.value === 'edit' && editId) {
    loadDetail(Number(editId))
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.basic-form-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f5f5;
  overflow: hidden;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.header-left, .header-right {
  display: flex;
  gap: 8px;
}

.header-center {
  flex: 1;
  text-align: center;
}

.form-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}

.form-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #fff;
  margin: 16px 24px;
  border-radius: 8px;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.footer-right {
  display: flex;
  gap: 12px;
}

.shortcut-hint {
  margin-left: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small) {
  height: 28px;
  line-height: 28px;
}

:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}

:deep(.ant-input-number-sm input) {
  height: 26px;
}
</style>
