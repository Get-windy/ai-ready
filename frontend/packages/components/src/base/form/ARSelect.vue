<template>
  <div
    :class="[
      'ar-select',
      {
        'is-focused': isFocused,
        'is-disabled': disabled,
        'is-error': hasError,
        'is-multiple': multiple,
        'is-filterable': filterable,
        'is-loading': loading
      }
    ]"
    :style="selectStyle"
  >
    <div
      class="ar-select__trigger"
      @click="toggleDropdown"
    >
      <div class="ar-select__value">
        <template v-if="multiple && selectedValues.length > 0">
          <span
            v-for="value in selectedValues"
            :key="value.value"
            class="ar-select__tag"
          >
            {{ getOptionLabel(value) }}
            <span
              v-if="!disabled"
              class="ar-select__tag-close"
              @click.stop="removeTag(value)"
            >
              ×
            </span>
          </span>
          <span
            v-if="selectedValues.length === 0"
            class="ar-select__placeholder"
          >
            {{ placeholder }}
          </span>
        </template>
        <template v-else>
          <span class="ar-select__single-value">
            {{ selectedLabel || placeholder }}
          </span>
        </template>
      </div>
      
      <div class="ar-select__suffix">
        <span
          v-if="loading"
          class="ar-select__loading"
        >
          <i class="ar-select__loading-icon"></i>
        </span>
        <span
          v-else-if="clearable && selectedValue"
          class="ar-select__clear"
          @click.stop="clearValue"
        >
          ×
        </span>
        <span class="ar-select__arrow">
          <i :class="dropdownIcon"></i>
        </span>
      </div>
    </div>
    
    <div
      v-if="isDropdownVisible"
      class="ar-select__dropdown"
      :style="dropdownStyle"
    >
      <div
        v-if="filterable"
        class="ar-select__filter"
      >
        <input
          v-model="filterText"
          class="ar-select__filter-input"
          type="text"
          :placeholder="filterPlaceholder"
          @keydown="handleFilterKeydown"
        />
      </div>
      
      <div class="ar-select__options">
        <div
          v-for="option in filteredOptions"
          :key="option.value"
          :class="[
            'ar-select__option',
            {
              'is-selected': isOptionSelected(option),
              'is-disabled': option.disabled,
              'is-focused': isOptionFocused(option)
            }
          ]"
          @click="selectOption(option)"
          @mouseenter="setFocusedOption(option)"
        >
          <span class="ar-select__option-label">
            {{ option.label }}
          </span>
          <span
            v-if="isOptionSelected(option)"
            class="ar-select__option-check"
          >
            ✓
          </span>
        </div>
      </div>
      
      <div
        v-if="filteredOptions.length === 0"
        class="ar-select__empty"
      >
        {{ emptyText }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

interface SelectOption {
  value: string | number;
  label: string;
  disabled?: boolean;
}

interface Props {
  options: SelectOption[];
  modelValue?: string | number | Array<string | number>;
  placeholder?: string;
  disabled?: boolean;
  clearable?: boolean;
  filterable?: boolean;
  multiple?: boolean;
  loading?: boolean;
  emptyText?: string;
  filterPlaceholder?: string;
  width?: string;
  maxHeight?: string;
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请选择',
  emptyText: '无匹配数据',
  filterPlaceholder: '输入关键词搜索',
  width: '100%',
  maxHeight: '200px'
});

const emit = defineEmits(['update:modelValue', 'change', 'clear']);

const isFocused = ref(false);
const isDropdownVisible = ref(false);
const filterText = ref('');
const focusedOption = ref<SelectOption | null>(null);

const selectedValue = computed(() => {
  if (props.multiple) {
    return Array.isArray(props.modelValue) ? props.modelValue : [];
  }
  return props.modelValue;
});

const selectedValues = computed(() => {
  if (!props.multiple) return [];
  
  const values = Array.isArray(selectedValue.value) ? selectedValue.value : [];
  return props.options.filter(option => values.includes(option.value));
});

const selectedLabel = computed(() => {
  if (props.multiple) return '';
  
  const option = props.options.find(opt => opt.value === selectedValue.value);
  return option ? option.label : '';
});

const filteredOptions = computed(() => {
  if (!filterText.value) return props.options;
  
  return props.options.filter(option =>
    option.label.toLowerCase().includes(filterText.value.toLowerCase())
  );
});

const selectStyle = computed(() => ({
  '--ar-select-width': props.width
}));

const dropdownStyle = computed(() => ({
  '--ar-select-dropdown-max-height': props.maxHeight
}));

const dropdownIcon = computed(() =>
  isDropdownVisible.value ? 'el-icon-arrow-up' : 'el-icon-arrow-down'
);

const hasError = computed(() => {
  // Add your validation logic here
  return false;
});

const toggleDropdown = () => {
  if (props.disabled || props.loading) return;
  isDropdownVisible.value = !isDropdownVisible.value;
  if (isDropdownVisible.value) {
    isFocused.value = true;
  }
};

const selectOption = (option: SelectOption) => {
  if (option.disabled) return;
  
  if (props.multiple) {
    const values = Array.isArray(selectedValue.value) ? [...selectedValue.value] : [];
    const index = values.indexOf(option.value);
    
    if (index > -1) {
      values.splice(index, 1);
    } else {
      values.push(option.value);
    }
    
    emit('update:modelValue', values);
    emit('change', values);
  } else {
    emit('update:modelValue', option.value);
    emit('change', option.value);
    isDropdownVisible.value = false;
  }
};

const removeTag = (option: SelectOption) => {
  if (props.disabled || props.loading) return;
  
  const values = Array.isArray(selectedValue.value) ? [...selectedValue.value] : [];
  const index = values.indexOf(option.value);
  
  if (index > -1) {
    values.splice(index, 1);
    emit('update:modelValue', values);
    emit('change', values);
  }
};

const clearValue = () => {
  if (props.disabled || props.loading) return;
  
  if (props.multiple) {
    emit('update:modelValue', []);
  } else {
    emit('update:modelValue', '');
  }
  
  emit('clear');
  isDropdownVisible.value = false;
};

const isOptionSelected = (option: SelectOption) => {
  if (props.multiple) {
    return Array.isArray(selectedValue.value) && selectedValue.value.includes(option.value);
  }
  return selectedValue.value === option.value;
};

const isOptionFocused = (option: SelectOption) => {
  return focusedOption.value?.value === option.value;
};

const setFocusedOption = (option: SelectOption) => {
  focusedOption.value = option;
};

const getOptionLabel = (option: SelectOption) => {
  return option.label;
};

const handleFilterKeydown = (event: KeyboardEvent) => {
  // Handle keyboard navigation
  if (event.key === 'Enter' && focusedOption.value) {
    selectOption(focusedOption.value);
  }
};

// Close dropdown when clicking outside
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement;
  if (!target.closest('.ar-select')) {
    isDropdownVisible.value = false;
    isFocused.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style lang="scss" scoped>
.ar-select {
  position: relative;
  width: var(--ar-select-width, 100%);
  font-size: 14px;

  &__trigger {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    border: 1px solid var(--ar-select-border-color, #dcdfe6);
    border-radius: 4px;
    background-color: white;
    cursor: pointer;
    transition: border-color 0.2s ease;
    min-height: 40px;

    &:hover {
      border-color: var(--ar-select-hover-border-color, #c0c4cc);
    }
  }

  &.is-focused {
    .ar-select__trigger {
      border-color: var(--ar-select-focus-border-color, #409eff);
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }

  &.is-disabled {
    .ar-select__trigger {
      background-color: var(--ar-select-disabled-bg, #f5f7fa);
      cursor: not-allowed;
      opacity: 0.6;
    }
  }

  &.is-error {
    .ar-select__trigger {
      border-color: var(--ar-select-error-color, #f56c6c);
    }
  }

  &__value {
    flex: 1;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px;
    overflow: hidden;
  }

  &__placeholder {
    color: var(--ar-select-placeholder-color, #c0c4cc);
  }

  &__single-value {
    color: var(--ar-select-text-color, #606266);
  }

  &__tag {
    display: inline-flex;
    align-items: center;
    padding: 2px 6px;
    background-color: var(--ar-select-tag-bg, #f0f2f5);
    border-radius: 3px;
    font-size: 12px;
    color: var(--ar-select-tag-color, #606266);
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &-close {
      margin-left: 4px;
      cursor: pointer;
      font-size: 14px;
      line-height: 1;

      &:hover {
        color: var(--ar-select-tag-close-hover-color, #f56c6c);
      }
    }
  }

  &__suffix {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-left: 8px;
  }

  &__clear {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    cursor: pointer;
    color: var(--ar-select-clear-color, #c0c4cc);
    font-size: 16px;
    line-height: 1;

    &:hover {
      color: var(--ar-select-clear-hover-color, #909399);
    }
  }

  &__arrow {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    color: var(--ar-select-arrow-color, #c0c4cc);
    transition: transform 0.2s ease;
    font-size: 12px;
  }

  &__loading {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    animation: ar-select-loading 1s linear infinite;
  }

  &__dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    margin-top: 4px;
    background-color: white;
    border: 1px solid var(--ar-select-dropdown-border, #e4e7ed);
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    z-index: 1000;
    overflow: hidden;
  }

  &__filter {
    padding: 8px;
    border-bottom: 1px solid var(--ar-select-filter-border, #e4e7ed);
  }

  &__filter-input {
    width: 100%;
    padding: 6px 8px;
    border: 1px solid var(--ar-select-filter-input-border, #dcdfe6);
    border-radius: 3px;
    font-size: 14px;
    outline: none;

    &:focus {
      border-color: var(--ar-select-filter-input-focus-border, #409eff);
    }
  }

  &__options {
    max-height: var(--ar-select-dropdown-max-height, 200px);
    overflow-y: auto;
  }

  &__option {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    cursor: pointer;
    transition: background-color 0.2s ease;

    &:hover {
      background-color: var(--ar-select-option-hover-bg, #f5f7fa);
    }

    &.is-selected {
      color: var(--ar-select-option-selected-color, #409eff);
      font-weight: 600;
      background-color: var(--ar-select-option-selected-bg, #f0f9ff);
    }

    &.is-disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    &.is-focused {
      background-color: var(--ar-select-option-focus-bg, #f5f7fa);
    }

    &-label {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &-check {
      margin-left: 8px;
      font-weight: bold;
    }
  }

  &__empty {
    padding: 20px;
    text-align: center;
    color: var(--ar-select-empty-color, #909399);
    font-size: 13px;
  }

  @keyframes ar-select-loading {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }
}
</style>