<template>
  <div class="ar-calendar-view">
    <div class="ar-calendar-toolbar">
      <div class="ar-calendar-toolbar-left">
        <button
          class="ar-calendar-nav-btn"
          @click="handlePrevMonth"
        >
          <span class="ar-calendar-nav-icon">◀</span>
        </button>
        <span class="ar-calendar-current-month">{{ currentMonthText }}</span>
        <button
          class="ar-calendar-nav-btn"
          @click="handleNextMonth"
        >
          <span class="ar-calendar-nav-icon">▶</span>
        </button>
      </div>
      <div class="ar-calendar-toolbar-right">
        <button
          class="ar-calendar-today-btn"
          @click="handleToday"
        >今天</button>
        <slot name="toolbar-right" />
      </div>
    </div>

    <div class="ar-calendar-header">
      <div
        v-for="day in weekDays"
        :key="day"
        class="ar-calendar-header-cell"
      >
        <span class="ar-calendar-header-text">{{ day }}</span>
      </div>
    </div>

    <div class="ar-calendar-body">
      <div
        v-for="(week, weekIndex) in calendarWeeks"
        :key="weekIndex"
        class="ar-calendar-week"
      >
        <div
          v-for="(day, dayIndex) in week"
          :key="dayIndex"
          class="ar-calendar-day"
          :class="{
            'ar-calendar-day--today': isToday(day),
            'ar-calendar-day--current-month': isCurrentMonth(day),
            'ar-calendar-day--other-month': !isCurrentMonth(day),
            'ar-calendar-day--has-events': hasEvents(day)
          }"
          @click="handleDayClick(day)"
        >
          <div class="ar-calendar-day-header">
            <span class="ar-calendar-day-number">{{ getDayNumber(day) }}</span>
          </div>
          <div class="ar-calendar-day-events">
            <div
              v-for="event in getEventsByDay(day)"
              :key="event.id"
              class="ar-calendar-event"
              :class="`ar-calendar-event--${event.type || 'default'}`"
              @click.stop="handleEventClick(event)"
            >
              <span class="ar-calendar-event-title">{{ event.title }}</span>
            </div>
            <div
              v-if="getEventsByDay(day).length > maxEventsPerDay"
              class="ar-calendar-event-more"
            >
              <span class="ar-calendar-event-more-text">
                +{{ getEventsByDay(day).length - maxEventsPerDay }} 更多
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface CalendarEvent {
  id: string | number
  title: string
  date: string
  type?: 'order' | 'inbound' | 'outbound' | 'payment' | 'meeting' | 'default'
  [key: string]: any
}

interface Props {
  events?: CalendarEvent[]
  currentDate?: Date
  maxEventsPerDay?: number
  dateField?: string
}

const props = withDefaults(defineProps<Props>(), {
  events: () => [],
  currentDate: () => new Date(),
  maxEventsPerDay: 3,
  dateField: 'date'
})

const emit = defineEmits<{
  dayClick: [date: Date]
  eventClick: [event: CalendarEvent]
  monthChange: [date: Date]
}>()

const currentMonth = ref(props.currentDate)

watch(() => props.currentDate, (val) => {
  currentMonth.value = val
})

const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const currentMonthText = computed(() => {
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth() + 1
  return `${year}年${month}月`
})

const calendarWeeks = computed(() => {
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const weeks: Date[][] = []
  
  let currentWeek: Date[] = []
  let currentDate = new Date(firstDay)
  
  const firstDayOfWeek = firstDay.getDay()
  for (let i = 0; i < firstDayOfWeek; i++) {
    const prevDate = new Date(year, month, -firstDayOfWeek + i + 1)
    currentWeek.push(prevDate)
  }
  
  while (currentDate <= lastDay) {
    currentWeek.push(new Date(currentDate))
    if (currentWeek.length === 7) {
      weeks.push(currentWeek)
      currentWeek = []
    }
    currentDate.setDate(currentDate.getDate() + 1)
  }
  
  if (currentWeek.length > 0) {
    const remainingDays = 7 - currentWeek.length
    for (let i = 1; i <= remainingDays; i++) {
      const nextDate = new Date(year, month + 1, i)
      currentWeek.push(nextDate)
    }
    weeks.push(currentWeek)
  }
  
  return weeks
})

const getDayNumber = (date: Date) => {
  return date.getDate()
}

const isToday = (date: Date) => {
  const today = new Date()
  return date.getFullYear() === today.getFullYear() &&
         date.getMonth() === today.getMonth() &&
         date.getDate() === today.getDate()
}

const isCurrentMonth = (date: Date) => {
  return date.getMonth() === currentMonth.value.getMonth()
}

const formatDateKey = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getEventsByDay = (date: Date) => {
  const dateKey = formatDateKey(date)
  return props.events.filter(event => {
    const eventDate = event[props.dateField]
    if (typeof eventDate === 'string') {
      return eventDate.startsWith(dateKey)
    }
    return false
  })
}

const hasEvents = (date: Date) => {
  return getEventsByDay(date).length > 0
}

const handlePrevMonth = () => {
  const prevMonth = new Date(currentMonth.value)
  prevMonth.setMonth(prevMonth.getMonth() - 1)
  currentMonth.value = prevMonth
  emit('monthChange', prevMonth)
}

const handleNextMonth = () => {
  const nextMonth = new Date(currentMonth.value)
  nextMonth.setMonth(nextMonth.getMonth() + 1)
  currentMonth.value = nextMonth
  emit('monthChange', nextMonth)
}

const handleToday = () => {
  currentMonth.value = new Date()
  emit('monthChange', new Date())
}

const handleDayClick = (date: Date) => {
  emit('dayClick', date)
}

const handleEventClick = (event: CalendarEvent) => {
  emit('eventClick', event)
}
</script>

<style lang="scss" scoped>
.ar-calendar-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--ar-bg-color, #ffffff);
}

.ar-calendar-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ar-spacing-md, 12px) var(--ar-spacing-lg, 16px);
  border-bottom: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-calendar-toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-calendar-nav-btn {
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  border: 1px solid var(--ar-border-color-base, #dcdfe6);
  background-color: transparent;
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-base, 14px);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    border-color: var(--ar-color-primary, #409eff);
    color: var(--ar-color-primary, #409eff);
  }
}

.ar-calendar-nav-icon {
  font-size: var(--ar-font-size-small, 13px);
}

.ar-calendar-current-month {
  font-size: var(--ar-font-size-medium, 16px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-primary, #303133);
}

.ar-calendar-toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-calendar-today-btn {
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-md, 12px);
  border: 1px solid var(--ar-color-primary, #409eff);
  background-color: var(--ar-color-primary, #409eff);
  color: var(--ar-color-white, #ffffff);
  font-size: var(--ar-font-size-small, 13px);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    background-color: var(--ar-color-primary-dark-2, #3a8ee6);
    border-color: var(--ar-color-primary-dark-2, #3a8ee6);
  }
}

.ar-calendar-header {
  display: flex;
  border-bottom: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-calendar-header-cell {
  flex: 1;
  padding: var(--ar-spacing-sm, 8px);
  text-align: center;
}

.ar-calendar-header-text {
  font-size: var(--ar-font-size-small, 13px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-secondary, #909399);
}

.ar-calendar-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ar-calendar-week {
  display: flex;
  border-bottom: 1px solid var(--ar-border-color-lighter, #ebeef5);
}

.ar-calendar-day {
  flex: 1;
  min-height: 100px;
  padding: var(--ar-spacing-xs, 4px);
  border-right: 1px solid var(--ar-border-color-lighter, #ebeef5);
  cursor: pointer;
  transition: background-color var(--ar-transition-duration, 0.2s);

  &:last-child {
    border-right: none;
  }

  &:hover {
    background-color: var(--ar-fill-color-light, #f5f7fa);
  }

  &--today {
    background-color: var(--ar-color-primary-light-3, rgba(64, 158, 255, 0.3));
  }

  &--other-month {
    background-color: var(--ar-fill-color-lighter, #fafafa);
    color: var(--ar-text-color-placeholder, #c0c4cc);
  }

  &--has-events {
    background-color: var(--ar-fill-color-light, #f5f7fa);
  }
}

.ar-calendar-day-header {
  padding: var(--ar-spacing-xs, 4px);
}

.ar-calendar-day-number {
  font-size: var(--ar-font-size-base, 14px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-primary, #303133);
}

.ar-calendar-day-events {
  display: flex;
  flex-direction: column;
  gap: var(--ar-spacing-xs, 4px);
}

.ar-calendar-event {
  padding: var(--ar-spacing-xs, 4px);
  border-radius: var(--ar-border-radius-small, 2px);
  font-size: var(--ar-font-size-extra-small, 12px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &--order {
    background-color: var(--ar-color-primary-light-3, rgba(64, 158, 255, 0.3));
    color: var(--ar-color-primary, #409eff);
  }

  &--inbound {
    background-color: var(--ar-color-success-lighter, #f0f9eb);
    color: var(--ar-color-success, #67c23a);
  }

  &--outbound {
    background-color: var(--ar-color-warning-lighter, #fdf6ec);
    color: var(--ar-color-warning, #e6a23c);
  }

  &--payment {
    background-color: var(--ar-color-danger-lighter, #fef0f0);
    color: var(--ar-color-danger, #f56c6c);
  }

  &--meeting {
    background-color: var(--ar-color-info-lighter, #f4f4f5);
    color: var(--ar-color-info, #909399);
  }

  &--default {
    background-color: var(--ar-fill-color, #f0f2f5);
    color: var(--ar-text-color-regular, #606266);
  }

  &:hover {
    opacity: 0.8;
  }
}

.ar-calendar-event-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ar-calendar-event-more {
  padding: var(--ar-spacing-xs, 4px);
}

.ar-calendar-event-more-text {
  font-size: var(--ar-font-size-extra-small, 12px);
  color: var(--ar-color-primary, #409eff);
  cursor: pointer;
}

@media (max-width: 768px) {
  .ar-calendar-toolbar {
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-calendar-day {
    min-height: 60px;
  }

  .ar-calendar-day-number {
    font-size: var(--ar-font-size-small, 13px);
  }

  .ar-calendar-event {
    font-size: var(--ar-font-size-extra-small, 12px);
  }
}
</style>