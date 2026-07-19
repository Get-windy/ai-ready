<template>
  <div
    ref="wrapperRef"
    class="ss-search-select"
  >
    <input
      type="text"
      :value="displayText"
      :placeholder="placeholder"
      class="ss-native-input ss-native-text"
      @input="onInput"
      @focus="onFocus"
      @blur="onBlur"
      @keydown="onKeydown"
    >
    <Teleport to="body">
      <ul
        v-if="showDropdown"
        class="ss-search-dropdown"
        :style="dropdownStyle"
      >
        <li
          v-for="(opt, i) in filteredOptions"
          :key="opt.value"
          :class="['ss-search-item', { highlighted: i === highlightIndex }]"
          @mousedown.prevent="selectOption(opt)"
        >
          {{ opt.label }}
        </li>
        <li
          v-if="filteredOptions.length === 0 && query.trim()"
          class="ss-search-item ss-no-result"
        >
          无匹配结果
        </li>
      </ul>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

export interface SearchSelectOption {
  label: string
  value: string | number
  /** 搜索匹配文本（默认使用 label） */
  searchText?: string
}

const props = withDefaults(defineProps<{
  modelValue?: string | number | null
  options?: SearchSelectOption[]
  placeholder?: string
}>(), {
  modelValue: undefined,
  options: () => [],
  placeholder: '请搜索',
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number | undefined]
  /** 空关键字回车时触发，用于打开选择弹窗 */
  'openSelectModal': []
}>()

const wrapperRef = ref<HTMLElement>()

// ── 状态 ──
const query = ref('')
const filteredOptions = ref<SearchSelectOption[]>([])
const highlightIndex = ref(-1)
const showDropdown = ref(false)
const dropdownStyle = ref<Record<string, string>>({})

// ── 显示文本：有查询用查询，否则显示选中项的 label ──
const displayText = computed(() => {
  if (query.value) return query.value
  if (props.modelValue != null && props.options) {
    const opt = props.options.find(o => o.value === props.modelValue)
    if (opt) return opt.label
  }
  return ''
})

// ── 更新下拉位置 ──
function updatePosition(target: HTMLElement) {
  const rect = target.getBoundingClientRect()
  dropdownStyle.value = {
    position: 'fixed',
    top: `${rect.bottom}px`,
    left: `${rect.left}px`,
    width: `${rect.width}px`,
  }
}

// ── 过滤选项 ──
function filterOptions(keyword: string) {
  // 空关键词时不显示选项，等用户输入后才搜索
  if (!keyword.trim()) {
    filteredOptions.value = []
    return
  }
  const kw = keyword.trim().toLowerCase()
  // 模糊搜索匹配，最多显示10条
  filteredOptions.value = (props.options || [])
    .filter(opt =>
      (opt.searchText || opt.label || '').toLowerCase().includes(kw)
    )
    .slice(0, 10)
}

// ── 选中选项 ──
function selectOption(opt: SearchSelectOption) {
  query.value = opt.label
  filteredOptions.value = []
  highlightIndex.value = -1
  showDropdown.value = false
  emit('update:modelValue', opt.value)
}

// ── 事件处理 ──
function onInput(e: Event) {
  const input = e.target as HTMLInputElement
  query.value = input.value
  updatePosition(input)

  // 空值时清空选中，不显示下拉
  if (!query.value.trim()) {
    emit('update:modelValue', undefined)
    filteredOptions.value = []
    highlightIndex.value = -1
    showDropdown.value = false
    return
  }

  filterOptions(query.value)
  highlightIndex.value = filteredOptions.value.length > 0 ? 0 : -1
  // 输入后显示下拉列表（如果有匹配结果或显示"无匹配结果"提示）
  showDropdown.value = true
}

function onFocus(e: FocusEvent) {
  const target = e.target as HTMLElement
  updatePosition(target)
  // 获得焦点时不显示下拉，等用户输入后才搜索
}

function onBlur() {
  // 延迟关闭下拉，确保用户点击选项时能先触发 selectOption
  setTimeout(() => {
    showDropdown.value = false
    // 恢复显示选中项的 label
    if (props.modelValue != null && props.options) {
      const opt = props.options.find(o => o.value === props.modelValue)
      if (opt) query.value = opt.label
    } else {
      query.value = ''
    }
    filteredOptions.value = []
  }, 200)
}

function onKeydown(e: KeyboardEvent) {
  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      if (filteredOptions.value.length > 0) {
        highlightIndex.value = Math.min(highlightIndex.value + 1, filteredOptions.value.length - 1)
      }
      break
    case 'ArrowUp':
      e.preventDefault()
      highlightIndex.value = Math.max(highlightIndex.value - 1, 0)
      break
    case 'Enter':
      e.preventDefault()
      // 空关键字回车时，打开选择弹窗
      if (!query.value.trim()) {
        emit('openSelectModal')
        return
      }
      // 有匹配项时选中高亮的选项
      if (showDropdown.value && highlightIndex.value >= 0 && filteredOptions.value[highlightIndex.value]) {
        selectOption(filteredOptions.value[highlightIndex.value])
      } else if (query.value.trim()) {
        // 精确匹配文本时选中
        const kw = query.value.trim().toLowerCase()
        const matched = (props.options || []).find(opt =>
          (opt.label || '').toLowerCase() === kw || (opt.searchText || '').toLowerCase() === kw
        )
        if (matched) selectOption(matched)
      }
      break
    case 'Escape':
      e.preventDefault()
      showDropdown.value = false
      break
  }
}

// ── modelValue 外部变更时同步 ──
watch(() => props.modelValue, () => {
  query.value = ''
})
</script>

<style scoped>
.ss-search-select {
  width: 100%;
  height: 100%;
}

.ss-native-input {
  width: 100%;
  height: 100%;
  border: none !important;
  outline: none !important;
  background: transparent !important;
  font-size: 12px !important;
  color: #262626;
  padding: 0 6px !important;
  box-sizing: border-box;
  font-family: inherit;
  line-height: 28px;
}

.ss-native-input:focus {
  background: #e6f7ff !important;
}

.ss-native-input::placeholder {
  color: #bfbfbf;
}
</style>

<!-- Teleport 到 body 的浮层样式必须非 scoped -->
<style>
.ss-search-dropdown {
  position: fixed;
  z-index: 1050;
  max-height: 220px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 2px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
  list-style: none;
  margin: 0;
  padding: 2px 0;
}

.ss-search-item {
  display: flex;
  align-items: center;
  padding: 5px 10px;
  cursor: pointer;
  font-size: 12px;
  color: #262626;
  transition: background 0.15s;
}

.ss-search-item:hover,
.ss-search-item.highlighted {
  background: #e6f7ff;
}

.ss-no-result {
  color: #999;
  cursor: default;
  background: transparent;
}
.ss-no-result:hover {
  background: transparent;
}
</style>
