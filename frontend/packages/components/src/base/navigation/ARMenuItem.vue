<template>
  <li
    :class="[
      'ar-menu-item',
      {
        'is-active': isActive,
        'is-disabled': disabled
      }
    ]"
    @click="handleClick"
  >
    <span v-if="icon" class="ar-menu-item__icon">
      <i :class="icon"></i>
    </span>
    <span class="ar-menu-item__text">
      <slot>{{ title }}</slot>
    </span>
    <span v-if="badge" class="ar-menu-item__badge">{{ badge }}</span>
  </li>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  index: string;
  title?: string;
  icon?: string;
  badge?: string | number;
  disabled?: boolean;
  route?: string | object;
}

const props = defineProps<Props>();

const isActive = computed(() => {
  // This would be computed from parent menu context
  return false;
});

const handleClick = () => {
  if (props.disabled) return;
  
  // Emit click event with index
  // Handle router navigation if router prop is true
};
</script>

<style lang="scss" scoped>
.ar-menu-item {
  position: relative;
  display: flex;
  align-items: center;
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
  user-select: none;
  color: inherit;

  &:hover {
    background-color: var(--ar-menu-item-hover-bg, #f5f7fa);
    
    .ar-menu-item__text {
      transform: translateX(2px);
    }
  }

  &.is-active {
    color: var(--ar-menu-item-active-color, #409eff);
    background-color: var(--ar-menu-item-active-bg, #ecf5ff);
    font-weight: 600;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background-color: currentColor;
    }
  }

  &.is-disabled {
    opacity: 0.6;
    cursor: not-allowed;
    
    &:hover {
      background-color: transparent;
    }
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    margin-right: 12px;
    font-size: 18px;
  }

  &__text {
    flex: 1;
    transition: transform 0.2s ease;
    font-size: 14px;
  }

  &__badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 20px;
    height: 20px;
    padding: 0 6px;
    border-radius: 10px;
    background-color: var(--ar-menu-item-badge-bg, #f56c6c);
    color: white;
    font-size: 12px;
    font-weight: 500;
    margin-left: 8px;
  }
}
</style>