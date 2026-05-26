<template>
  <div :class="listClasses">
    <div v-if="items.length === 0 && !loading" class="ar-list__empty">
      <slot name="empty">
        <div class="ar-list__empty-content">
          <svg class="ar-list__empty-icon" viewBox="0 0 24 24">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z" fill="currentColor"/>
          </svg>
          <p class="ar-list__empty-text">{{ emptyText }}</p>
        </div>
      </slot>
    </div>

    <div v-else class="ar-list__items">
      <div
        v-for="(item, index) in items"
        :key="getItemKey(item, index)"
        :class="['ar-list__item', { 'ar-list__item--clickable': itemClickable }]"
        @click="handleItemClick(item, index)"
      >
        <slot :item="item" :index="index">
          <div class="ar-list__item-content">
            <div class="ar-list__item-left" v-if="item.image || item.icon">
              <img v-if="item.image" :src="item.image" class="ar-list__item-image" />
              <span v-else class="ar-list__item-icon">{{ item.icon }}</span>
            </div>
            <div class="ar-list__item-main">
              <div class="ar-list__item-title">{{ item.title }}</div>
              <div v-if="item.description" class="ar-list__item-desc">{{ item.description }}</div>
            </div>
            <div class="ar-list__item-right" v-if="item.rightText || item.tag">
              <span v-if="item.tag" :class="['ar-list__item-tag', `ar-list__item-tag--${item.tagType || 'primary'}`]">
                {{ item.tag }}
              </span>
              <span v-if="item.rightText" class="ar-list__item-right-text">{{ item.rightText }}</span>
            </div>
            <div v-if="showArrow" class="ar-list__item-arrow">
              <svg viewBox="0 0 24 24" class="ar-list__arrow-icon">
                <path d="M9.29 6.71c-.39.39-.39 1.02 0 1.41L13.17 12l-3.88 3.88c-.39.39-.39 1.02 0 1.41.39.39 1.02.39 1.41 0l4.59-4.59c.39-.39.39-1.02 0-1.41L10.7 6.71c-.39-.39-1.02-.39-1.41 0z" fill="currentColor"/>
              </svg>
            </div>
          </div>
        </slot>
      </div>
    </div>

    <div v-if="loading" class="ar-list__loading">
      <svg class="ar-list__loading-icon" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2">
          <animateTransform attributeName="transform" type="rotate" from="0 12 12" to="360 12 12" dur="1s" repeatCount="indefinite"/>
        </circle>
      </svg>
      <span>{{ loadingText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'

interface ListItem {
  id?: string | number
  title?: string
  description?: string
  image?: string
  icon?: string
  tag?: string
  tagType?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  rightText?: string
  [key: string]: unknown
}

const props = defineProps({
  items: { type: Array as PropType<ListItem[]>, default: () => [] },
  itemKey: { type: String, default: 'id' },
  loading: { type: Boolean, default: false },
  itemClickable: { type: Boolean, default: true },
  showArrow: { type: Boolean, default: true },
  divider: { type: Boolean, default: true },
  emptyText: { type: String, default: '暂无数据' },
  loadingText: { type: String, default: '加载中...' },
  customClass: { type: String, default: '' },
})

const emit = defineEmits<{
  (e: 'click', item: ListItem, index: number): void
}>()

const listClasses = computed(() => [
  'ar-list',
  { 'ar-list--divider': props.divider },
  props.customClass,
])

const getItemKey = (item: ListItem, index: number): string | number => {
  return item[props.itemKey] ?? index
}

const handleItemClick = (item: ListItem, index: number) => {
  if (!props.itemClickable) return
  emit('click', item, index)
}
</script>

<style scoped lang="scss">
.ar-list {
  background: var(--ar-color-background-white);

  &__empty {
    padding: 60px 20px;
    text-align: center;
  }

  &__empty-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }

  &__empty-icon {
    width: 64px;
    height: 64px;
    color: var(--ar-color-text-quaternary);
  }

  &__empty-text {
    color: var(--ar-color-text-tertiary);
    font-size: var(--ar-font-size-md);
  }

  &__items {
    .ar-list--divider & {
      .ar-list__item:not(:last-child) {
        border-bottom: 1px solid var(--ar-color-border);
      }
    }
  }

  &__item {
    padding: 16px;
    background: var(--ar-color-background-white);
    transition: background 0.2s ease;

    &--clickable {
      cursor: pointer;

      &:active {
        background: var(--ar-color-background-gray);
      }
    }
  }

  &__item-content {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__item-left {
    flex-shrink: 0;
  }

  &__item-image {
    width: 48px;
    height: 48px;
    border-radius: var(--ar-border-radius-md);
    object-fit: cover;
  }

  &__item-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--ar-color-background-gray);
    border-radius: var(--ar-border-radius-md);
    font-size: var(--ar-font-size-lg);
  }

  &__item-main {
    flex: 1;
    min-width: 0;
  }

  &__item-title {
    font-size: var(--ar-font-size-md);
    color: var(--ar-color-text-primary);
    font-weight: var(--font-weight-bold);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__item-desc {
    font-size: var(--ar-font-size-sm);
    color: var(--ar-color-text-secondary);
    margin-top: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__item-right {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  &__item-tag {
    padding: 2px 8px;
    border-radius: var(--ar-border-radius-sm);
    font-size: var(--ar-font-size-xs);

    &--primary {
      background: rgba(25, 137, 250, 0.1);
      color: var(--ar-color-primary);
    }

    &--success {
      background: rgba(7, 193, 96, 0.1);
      color: var(--ar-color-success);
    }

    &--warning {
      background: rgba(255, 151, 106, 0.1);
      color: var(--ar-color-warning);
    }

    &--danger {
      background: rgba(238, 10, 36, 0.1);
      color: var(--ar-color-danger);
    }

    &--info {
      background: rgba(150, 151, 153, 0.1);
      color: var(--ar-color-info);
    }
  }

  &__item-right-text {
    font-size: var(--ar-font-size-sm);
    color: var(--ar-color-text-tertiary);
  }

  &__item-arrow {
    flex-shrink: 0;
    color: var(--ar-color-text-quaternary);
  }

  &__arrow-icon {
    width: 20px;
    height: 20px;
  }

  &__loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 20px;
    color: var(--ar-color-text-tertiary);
    font-size: var(--ar-font-size-sm);
  }

  &__loading-icon {
    width: 20px;
    height: 20px;
    animation: rotate 1s linear infinite;

    @keyframes rotate {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
  }
}
</style>
