<template>
  <ul
    :class="[
      'ar-menu',
      `ar-menu--${mode}`,
      `ar-menu--${theme}`,
      {
        'ar-menu--collapse': collapse,
        'ar-menu--horizontal': mode === 'horizontal',
        'ar-menu--vertical': mode === 'vertical'
      }
    ]"
    :style="menuStyle"
  >
    <slot></slot>
  </ul>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  mode?: 'horizontal' | 'vertical';
  theme?: 'light' | 'dark';
  collapse?: boolean;
  backgroundColor?: string;
  textColor?: string;
  activeTextColor?: string;
  defaultActive?: string;
  router?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'vertical',
  theme: 'light',
  defaultActive: ''
});

const menuStyle = computed(() => ({
  '--ar-menu-bg': props.backgroundColor,
  '--ar-menu-text-color': props.textColor,
  '--ar-menu-active-text-color': props.activeTextColor
}));

// Provide context to child menu items
const activeIndex = ref(props.defaultActive);
const mode = ref(props.mode);
const router = ref(props.router);
</script>

<style lang="scss" scoped>
.ar-menu {
  margin: 0;
  padding: 0;
  list-style: none;
  border-right: 1px solid var(--ar-menu-border-color, #e6e6e6);

  &--horizontal {
    display: flex;
    border-right: none;
    border-bottom: 1px solid var(--ar-menu-border-color, #e6e6e6);
  }

  &--vertical {
    display: block;
    width: 200px;
  }

  &--collapse {
    width: 64px;

    .ar-menu__item {
      justify-content: center;
      padding: 0 12px;

      .ar-menu__item-text {
        display: none;
      }
    }
  }

  &--light {
    background-color: var(--ar-menu-bg, #ffffff);
    color: var(--ar-menu-text-color, #303133);

    .ar-menu__item {
      &:hover {
        background-color: var(--ar-menu-hover-bg, #f5f7fa);
      }

      &.is-active {
        color: var(--ar-menu-active-text-color, #409eff);
        background-color: var(--ar-menu-active-bg, #ecf5ff);
      }
    }
  }

  &--dark {
    background-color: var(--ar-menu-bg, #304156);
    color: var(--ar-menu-text-color, #bfcbd9);

    .ar-menu__item {
      &:hover {
        background-color: var(--ar-menu-hover-bg, #263445);
      }

      &.is-active {
        color: var(--ar-menu-active-text-color, #409eff);
        background-color: var(--ar-menu-active-bg, #2d8cf0);
      }
    }
  }

  &__item {
    position: relative;
    display: flex;
    align-items: center;
    padding: 12px 20px;
    cursor: pointer;
    transition: all 0.2s ease-in-out;
    user-select: none;

    &:hover {
      .ar-menu__item-text {
        transform: translateX(2px);
      }
    }

    &.is-active {
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

    &-icon {
      margin-right: 12px;
      font-size: 18px;
    }

    &-text {
      flex: 1;
      transition: transform 0.2s ease;
    }

    &-arrow {
      margin-left: auto;
      transition: transform 0.2s ease;

      &.is-expanded {
        transform: rotate(180deg);
      }
    }
  }

  &__submenu {
    overflow: hidden;
    transition: max-height 0.3s ease-in-out;

    &-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      cursor: pointer;
    }

    &-items {
      padding-left: 24px;
    }
  }
}
</style>