<template>
  <div
    :class="[
      'ar-select',
      {
        'ar-select--disabled': disabled,
        'ar-select--multiple': multiple,
        'ar-select--filterable': filterable,
        [`ar-select--${size}`]: size,
      }
    ]"
    ref="selectRef"
    @click="handleClick"
  >
    <!-- 选择器主体 -->
    <div
      :class="[
        'ar-select__wrapper',
        {
          'ar-select__wrapper--focus': visible,
          'ar-select__wrapper--error': error,
        }
      ]"
    >
      <!-- 选中值显示 -->
      <div class="ar-select__selection">
        <!-- 多选模式 -->
        <template v-if="multiple && Array.isArray(modelValue)">
          <span
            v-for="item in selectedOptions"
            :key="item.value"
            class="ar-select__tag"
          >
            {{ item.label }}
            <span
              class="ar-select__tag-close"
              @click.stop="removeTag(item.value)"
            >
              ×
            </span>
          </span>
        </template>

        <!-- 单选模式 -->
        <template v-else>
          <span class="ar-select__value">
            {{ currentLabel }}
          </span>
        </template>

        <!-- 占位符 -->
        <span
          v-if="!hasValue"
          class="ar-select__placeholder"
        >
          {{ placeholder }}
        </span>
      </div>

      <!-- 清空按钮 -->
      <span
        v-if="clearable && hasValue && !disabled"
        class="ar-select__clear"
        @click.stop="handleClear"
      >
        ×
      </span>

      <!-- 箭头图标 -->
      <span class="ar-select__suffix">
        <svg
          :class="['ar-select__arrow', { 'ar-select__arrow--rotate': visible }]"
          viewBox="0 0 1024 1024"
        >
          <path d="M884 256h-75c-5.1 0-9.9 2.5-12.9 6.6L512 654.2 227.9 262.6c-3-4.1-7.8-6.6-12.9-6.6h-75c-6.5 0-10.3 7.4-6.5 12.7l352.6 486.1c12.8 17.6 39 17.6 51.7 0l352.6-486.1c3.9-5.3 0.1-12.7-6.4-12.7z" />
        </svg>
      </span>
    </div>

    <!-- 下拉选项 -->
    <Teleport to="body">
      <Transition name="ar-select-dropdown">
        <div
          v-show="visible"
          :class="[
            'ar-select__dropdown',
            `ar-select__dropdown--${placement}`
          ]"
          :style="dropdownStyle"
          ref="dropdownRef"
        >
          <!-- 搜索框 -->
          <div
            v-if="filterable"
            class="ar-select__search"
          >
            <input
              v-model="query"
              type="text"
              class="ar-select__search-input"
              :placeholder="filterPlaceholder"
              @click.stop
            />
          </div>

          <!-- 选项列表 -->
          <div class="ar-select__options">
            <div
              v-for="option in filteredOptions"
              :key="option.value"
              :class="[
                'ar-select__option',
                {
                  'ar-select__option--selected': isSelected(option.value),
                  'ar-select__option--disabled': option.disabled,
                }
              ]"
              @click="handleSelect(option)"
            >
              <!-- 多选框 -->
              <span
                v-if="multiple"
                class="ar-select__option-checkbox"
              >
                <svg
                  v-if="isSelected(option.value)"
                  class="ar-select__option-check"
                  viewBox="0 0 1024 1024"
                >
                  <path d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896zm-55.808 736a50.432 50.432 0 0 0 35.712-14.848l304.064-304.64a50.624 50.624 0 0 0-71.552-71.616l-268.8 268.8-155.776-155.712a50.56 50.56 0 1 0-71.552 71.552l191.488 191.488a50.56 50.56 0 0 0 35.84 14.848z" />
                </svg>
              </span>

              <!-- 选项文本 -->
              <span class="ar-select__option-label">
                {{ option.label }}
              </span>

              <!-- 单选选中标记 -->
              <svg
                v-if="!multiple && isSelected(option.value)"
                class="ar-select__option-check"
                viewBox="0 0 1024 1024"
              >
                <path d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896zm-55.808 736a50.432 50.432 0 0 0 35.712-14.848l304.064-304.64a50.624 50.624 0 0 0-71.552-71.616l-268.8 268.8-155.776-155.712a50.56 50.56 0 1 0-71.552 71.552l191.488 191.488a50.56 0 0 0 35.84 14.848z" />
              </svg>
            </div>

            <!-- 空状态 -->
            <div
              v-if="filteredOptions.length === 0"
              class="ar-select__empty"
            >
              {{ emptyText }}
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'

// 类型定义
export interface ISelectOption {
  label: string
  value: any
  disabled?: boolean
}

export interface ISelectProps {
  modelValue: any | any[]
  options?: ISelectOption[]
  placeholder?: string
  disabled?: boolean
  clearable?: boolean
  filterable?: boolean
  filterPlaceholder?: string
  multiple?: boolean
  size?: 'small' | 'medium' | 'large'
  placement?: 'bottom' | 'top'
  emptyText?: string
}

export interface ISelectEmits {
  (e: 'update:modelValue', value: any): void
  (e: 'change', value: any): void
  (e: 'visible-change', visible: boolean): void
  (e: 'clear'): void
  (e: 'remove-tag', value: any): void
}

// Props
const props = withDefaults(defineProps<ISelectProps>(), {
  options: () => [],
  placeholder: '请选择',
  disabled: false,
  clearable: false,
  filterable: false,
  filterPlaceholder: '请搜索',
  multiple: false,
  size: 'medium',
  placement: 'bottom',
  emptyText: '暂无数据',
})

// Emits
const emit = defineEmits<ISelectEmits>()

// Refs
const selectRef = ref<HTMLElement>()
const dropdownRef = ref<HTMLElement>()
const visible = ref(false)
const query = ref('')

// 计算属性
const hasValue = computed(() => {
  if (props.multiple) {
    return Array.isArray(props.modelValue) && props.modelValue.length > 0
  }
  return props.modelValue !== '' && props.modelValue !== null && props.modelValue !== undefined
})

const currentLabel = computed(() => {
  if (!hasValue.value) return ''
  const option = props.options.find(opt => opt.value === props.modelValue)
  return option?.label || String(props.modelValue)
})

const selectedOptions = computed(() => {
  if (!props.multiple || !Array.isArray(props.modelValue)) return []
  return props.modelValue
    .map(value => props.options.find(opt => opt.value === value))
    .filter(Boolean) as ISelectOption[]
})

const filteredOptions = computed(() => {
  if (!props.filterable || !query.value) {
    return props.options
  }
  return props.options.filter(opt =>
    opt.label.toLowerCase().includes(query.value.toLowerCase())
  )
})

const dropdownStyle = computed(() => {
  if (!selectRef.value) return {}
  const rect = selectRef.value.getBoundingClientRect()
  return {
    top: `${props.placement === 'bottom' ? rect.bottom + 4 : rect.top - 4}px`,
    left: `${rect.left}px`,
    width: `${rect.width}px`,
  }
})

// 方法
const isSelected = (value: any) => {
  if (props.multiple) {
    return Array.isArray(props.modelValue) && props.modelValue.includes(value)
  }
  return props.modelValue === value
}

const handleSelect = (option: ISelectOption) => {
  if (option.disabled) return

  if (props.multiple) {
    const values = Array.isArray(props.modelValue) ? [...props.modelValue] : []
    const index = values.indexOf(option.value)
    if (index > -1) {
      values.splice(index, 1)
      emit('remove-tag', option.value)
    } else {
      values.push(option.value)
    }
    emit('update:modelValue', values)
    emit('change', values)
  } else {
    emit('update:modelValue', option.value)
    emit('change', option.value)
    visible.value = false
  }
}

const removeTag = (value: any) => {
  if (!props.multiple || !Array.isArray(props.modelValue)) return
  const values = props.modelValue.filter((v: any) => v !== value)
  emit('update:modelValue', values)
  emit('change', values)
  emit('remove-tag', value)
}

const handleClear = () => {
  emit('update:modelValue', props.multiple ? [] : '')
  emit('change', props.multiple ? [] : '')
  emit('clear')
}

const handleClick = () => {
  if (props.disabled) return
  visible.value = !visible.value
}

// 点击外部关闭下拉
const handleClickOutside = (event: MouseEvent) => {
  if (!selectRef.value?.contains(event.target as Node) &&
      !dropdownRef.value?.contains(event.target as Node)) {
    visible.value = false
  }
}

// 监听
watch(visible, (val) => {
  emit('visible-change', val)
  if (val) {
    query.value = ''
    nextTick(() => {
      dropdownRef.value?.scrollIntoView({ block: 'nearest' })
    })
  }
})

// 生命周期
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped lang="scss">
.ar-select {
  position: relative;
  display: inline-block;
  width: 100%;
  cursor: pointer;

  &--disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

  &__wrapper {
    position: relative;
    display: flex;
    align-items: center;
    height: 36px;
    padding: 0 16px;
    background: #fff;
    border: 1px solid var(--ar-color-border);
    border-radius: var(--ar-radius-base);
    transition: all 0.3s;

    &--focus {
      border-color: var(--ar-color-primary);
      box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
    }

    &--error {
      border-color: var(--ar-color-danger);
      &:focus {
        box-shadow: 0 0 0 2px rgba(238, 10, 36, 0.2);
      }
    }
  }

  &--small &__wrapper {
    height: 32px;
    font-size: 13px;
  }

  &--large &__wrapper {
    height: 40px;
    font-size: 14px;
  }

  &--disabled &__wrapper {
    background-color: var(--ar-color-disabled-bg);
    cursor: not-allowed;
  }

  &__selection {
    flex: 1;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
    overflow: hidden;
  }

  &__value {
    flex: 1;
    color: var(--ar-color-text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__placeholder {
    color: var(--ar-color-text-placeholder);
  }

  &__tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    background: var(--ar-color-primary-light-9);
    color: var(--ar-color-primary);
    border-radius: 2px;
    font-size: 12px;
    white-space: nowrap;
  }

  &__tag-close {
    cursor: pointer;
    opacity: 0.7;
    &:hover {
      opacity: 1;
    }
  }

  &__clear {
    margin-right: 8px;
    font-size: 14px;
    color: var(--ar-color-text-placeholder);
    cursor: pointer;
    &:hover {
      color: var(--ar-color-text-primary);
    }
  }

  &__suffix {
    display: flex;
    align-items: center;
  }

  &__arrow {
    width: 12px;
    height: 12px;
    fill: var(--ar-color-text-placeholder);
    transition: transform 0.3s;

    &--rotate {
      transform: rotate(180deg);
    }
  }

  &__dropdown {
    position: fixed;
    z-index: 2000;
    max-height: 274px;
    margin-top: 4px;
    background: #fff;
    border: 1px solid var(--ar-color-border-light);
    border-radius: var(--ar-radius-base);
    box-shadow: var(--ar-shadow-base);
    overflow: hidden;

    &--top {
      margin-top: 0;
      margin-bottom: 4px;
    }
  }

  &__search {
    padding: 8px;
    border-bottom: 1px solid var(--ar-color-border-lighter);
  }

  &__search-input {
    width: 100%;
    height: 28px;
    padding: 0 8px;
    border: 1px solid var(--ar-color-border);
    border-radius: var(--ar-radius-small);
    font-size: 13px;
    outline: none;
    &:focus {
      border-color: var(--ar-color-primary);
    }
  }

  &__options {
    max-height: 274px;
    overflow-y: auto;
  }

  &__option {
    position: relative;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 16px;
    height: 36px;
    font-size: 14px;
    color: var(--ar-color-text-regular);
    cursor: pointer;
    transition: background 0.3s;

    &:hover {
      background: var(--ar-color-hover);
    }

    &--selected {
      color: var(--ar-color-primary);
      font-weight: 500;
    }

    &--disabled {
      color: var(--ar-color-text-disabled);
      cursor: not-allowed;
      &:hover {
        background: none;
      }
    }
  }

  &__option-checkbox {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 14px;
    height: 14px;
    border: 1px solid var(--ar-color-border);
    border-radius: 2px;
    transition: all 0.3s;
  }

  &__option-check {
    width: 14px;
    height: 14px;
    fill: var(--ar-color-primary);
  }

  &__empty {
    padding: 10px 0;
    text-align: center;
    color: var(--ar-color-text-secondary);
    font-size: 14px;
  }
}

// 动画
.ar-select-dropdown-enter-active,
.ar-select-dropdown-leave-active {
  transition: all 0.2s ease;
}

.ar-select-dropdown-enter-from,
.ar-select-dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>