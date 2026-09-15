<template>
  <div class="ar-stat-cards">
    <a-skeleton
      v-if="loading"
      active
      :paragraph="{ rows: 1 }"
    />
    <template v-else>
      <div
        v-for="(item, index) in items"
        :key="item.label"
        class="ar-stat-card"
        :class="{ 'is-clickable': isClickable(item) }"
        :title="isClickable(item) ? '点击查看明细' : undefined"
        @click="handleCardClick(item, index)"
      >
        <div class="ar-stat-card__label">
          {{ item.label }}
        </div>
        <div
          class="ar-stat-card__value"
          :style="item.valueStyle"
        >
          <span
            v-if="item.prefix"
            class="ar-stat-card__prefix"
          >{{ item.prefix }}</span>{{ formatValue(item) }}<span
            v-if="item.suffix"
            class="ar-stat-card__suffix"
          >{{ item.suffix }}</span>
        </div>
        <div
          v-if="item.trend !== undefined && item.trend !== null"
          class="ar-stat-card__trend"
          :class="item.trend >= 0 ? 'is-up' : 'is-down'"
        >
          <ArrowUpOutlined v-if="item.trend >= 0" />
          <ArrowDownOutlined v-else />
          {{ Math.abs(item.trend).toFixed(1) }}% 同比
        </div>
      </div>
    </template>
  </div>
</template>

<script lang="ts">
import type { StatCardItem } from '@/components/ARReportPage/types'

export type { StatCardItem }
</script>

<script setup lang="ts">
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'ARStatCards' })

const props = withDefaults(defineProps<{
  items: StatCardItem[]
  loading?: boolean
  /** 卡片是否可点击下钻（true 时鼠标变手型并派发 card-click） */
  clickable?: boolean
}>(), {
  loading: false,
  clickable: false
})

const emit = defineEmits<{ (e: 'card-click', item: StatCardItem, index: number): void }>()

function isClickable(item: StatCardItem): boolean {
  return props.clickable && item.clickable !== false
}

function handleCardClick(item: StatCardItem, index: number) {
  if (!isClickable(item)) return
  emit('card-click', item, index)
}

function formatValue(item: StatCardItem): string {
  if (typeof item.value === 'number') {
    if (item.precision !== undefined) {
      return item.value.toLocaleString('zh-CN', {
        minimumFractionDigits: item.precision,
        maximumFractionDigits: item.precision
      })
    }
    return item.value.toLocaleString('zh-CN')
  }
  return item.value ?? '-'
}
</script>

<style scoped>
.ar-stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.ar-stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.ar-stat-card.is-clickable {
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.ar-stat-card.is-clickable:hover {
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.24);
  transform: translateY(-1px);
}

.ar-stat-card__label {
  font-size: 13px;
  color: #888;
  margin-bottom: 8px;
}

.ar-stat-card__value {
  font-size: 22px;
  font-weight: 600;
  color: #262626;
  line-height: 1.3;
}

.ar-stat-card__prefix {
  font-size: 14px;
  margin-right: 2px;
}

.ar-stat-card__suffix {
  font-size: 13px;
  font-weight: 400;
  color: #888;
  margin-left: 4px;
}

.ar-stat-card__trend {
  font-size: 12px;
  margin-top: 6px;
}

.ar-stat-card__trend.is-up {
  color: #f5222d;
}

.ar-stat-card__trend.is-down {
  color: #52c41a;
}
</style>
