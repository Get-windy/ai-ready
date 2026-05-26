<template>
  <div class="ar-date-picker">
    <div 
      class="picker-trigger"
      :class="{ disabled: disabled, 'has-value': internalValue }"
      @click="handleClick"
    >
      <span v-if="internalValue" class="picker-value">
        {{ formatValue(internalValue) }}
      </span>
      <span v-else class="picker-placeholder">
        {{ placeholder }}
      </span>
      <span class="picker-icon">
        <svg viewBox="0 0 24 24" width="16" height="16">
          <path fill="currentColor" d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zm0-12H5V6h14v2z"/>
        </svg>
      </span>
    </div>
    
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showPicker" class="picker-overlay" @click="handleCancel">
          <Transition name="slide-up">
            <div v-if="showPicker" class="picker-panel" @click.stop>
              <div class="picker-header">
                <button class="picker-btn" @click="handleCancel">取消</button>
                <span class="picker-title">{{ title }}</span>
                <button class="picker-btn confirm" @click="handleConfirm">确定</button>
              </div>
              
              <div class="picker-body">
                <div class="picker-columns">
                  <div class="picker-column">
                    <div class="column-header">年</div>
                    <div class="column-items" ref="yearRef">
                      <div 
                        v-for="year in years"
                        :key="year"
                        class="column-item"
                        :class="{ selected: tempYear === year }"
                        @click="tempYear = year"
                      >
                        {{ year }}
                      </div>
                    </div>
                  </div>
                  
                  <div class="picker-column">
                    <div class="column-header">月</div>
                    <div class="column-items" ref="monthRef">
                      <div 
                        v-for="month in months"
                        :key="month"
                        class="column-item"
                        :class="{ selected: tempMonth === month }"
                        @click="tempMonth = month"
                      >
                        {{ month }}
                      </div>
                    </div>
                  </div>
                  
                  <div class="picker-column" v-if="showDay">
                    <div class="column-header">日</div>
                    <div class="column-items" ref="dayRef">
                      <div 
                        v-for="day in days"
                        :key="day"
                        class="column-item"
                        :class="{ selected: tempDay === day }"
                        @click="tempDay = day"
                      >
                        {{ day }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'

interface Props {
  modelValue?: string | Date | null
  placeholder?: string
  title?: string
  disabled?: boolean
  format?: string
  minDate?: Date
  maxDate?: Date
  showDay?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请选择日期',
  title: '选择日期',
  disabled: false,
  format: 'YYYY-MM-DD',
  showDay: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | Date | null): void
  (e: 'change', value: string | Date | null): void
}>()

const showPicker = ref(false)
const internalValue = ref<Date | null>(null)
const tempYear = ref(2024)
const tempMonth = ref(1)
const tempDay = ref(1)

const yearRef = ref<HTMLElement | null>(null)
const monthRef = ref<HTMLElement | null>(null)
const dayRef = ref<HTMLElement | null>(null)

const currentYear = new Date().getFullYear()
const years = computed(() => {
  const minYear = props.minDate?.getFullYear() || currentYear - 10
  const maxYear = props.maxDate?.getFullYear() || currentYear + 10
  const arr = []
  for (let i = minYear; i <= maxYear; i++) {
    arr.push(i)
  }
  return arr
})

const months = computed(() => {
  return Array.from({ length: 12 }, (_, i) => i + 1)
})

const days = computed(() => {
  const daysInMonth = new Date(tempYear.value, tempMonth.value, 0).getDate()
  return Array.from({ length: daysInMonth }, (_, i) => i + 1)
})

watch(() => props.modelValue, (val) => {
  if (val) {
    const date = typeof val === 'string' ? new Date(val) : val
    internalValue.value = date
    tempYear.value = date.getFullYear()
    tempMonth.value = date.getMonth() + 1
    tempDay.value = date.getDate()
  } else {
    internalValue.value = null
    tempYear.value = currentYear
    tempMonth.value = new Date().getMonth() + 1
    tempDay.value = new Date().getDate()
  }
}, { immediate: true })

const handleClick = () => {
  if (props.disabled) return
  showPicker.value = true
  
  nextTick(() => {
    scrollToSelected()
  })
}

const scrollToSelected = () => {
  const scrollTo = (ref: HTMLElement | null, value: number) => {
    if (!ref) return
    const items = ref.querySelectorAll('.column-item')
    const itemHeight = 40
    const index = value - 1
    ref.scrollTop = index * itemHeight - 80
  }
  
  scrollTo(yearRef.value, tempYear.value - (years.value[0] || currentYear - 10))
  scrollTo(monthRef.value, tempMonth.value)
  scrollTo(dayRef.value, tempDay.value)
}

const handleCancel = () => {
  showPicker.value = false
}

const handleConfirm = () => {
  const date = new Date(tempYear.value, tempMonth.value - 1, tempDay.value)
  internalValue.value = date
  
  const formattedValue = formatValue(date)
  emit('update:modelValue', formattedValue)
  emit('change', formattedValue)
  
  showPicker.value = false
}

const formatValue = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  
  if (props.showDay) {
    return `${year}-${month}-${day}`
  } else {
    return `${year}-${month}`
  }
}
</script>

<style lang="scss" scoped>
.ar-date-picker {
  .picker-trigger {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 12px;
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover:not(.disabled) {
      border-color: #1988fa;
    }
    
    &.disabled {
      background: #f5f7fa;
      cursor: not-allowed;
      opacity: 0.6;
    }
    
    &.has-value {
      .picker-value {
        color: #333;
      }
    }
    
    .picker-value {
      color: #333;
      font-size: 14px;
    }
    
    .picker-placeholder {
      color: #969799;
      font-size: 14px;
    }
    
    .picker-icon {
      color: #969799;
    }
  }
}

.picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.picker-panel {
  width: 100%;
  background: #fff;
  border-radius: 16px 16px 0 0;
  
  .picker-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #ebedf0;
    
    .picker-title {
      font-size: 16px;
      font-weight: 600;
    }
    
    .picker-btn {
      padding: 8px 16px;
      font-size: 14px;
      color: #969799;
      background: none;
      border: none;
      cursor: pointer;
      
      &.confirm {
        color: #1988fa;
      }
    }
  }
  
  .picker-body {
    padding: 16px;
    
    .picker-columns {
      display: flex;
      justify-content: center;
      
      .picker-column {
        flex: 1;
        max-width: 100px;
        text-align: center;
        
        .column-header {
          font-size: 12px;
          color: #969799;
          padding: 8px 0;
        }
        
        .column-items {
          height: 200px;
          overflow-y: auto;
          scroll-snap-type: y mandatory;
          
          .column-item {
            height: 40px;
            line-height: 40px;
            font-size: 16px;
            color: #333;
            scroll-snap-align: center;
            cursor: pointer;
            
            &:hover {
              background: #f7f8fa;
            }
            
            &.selected {
              color: #1988fa;
              font-weight: 600;
            }
          }
        }
      }
    }
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}
</style>