<template>
  <nav class="ar-breadcrumb" :style="breadcrumbStyle">
    <ol class="ar-breadcrumb__list">
      <li
        v-for="(item, index) in items"
        :key="index"
        :class="[
          'ar-breadcrumb__item',
          {
            'is-active': index === items.length - 1,
            'is-link': item.to && index !== items.length - 1
          }
        ]"
      >
        <template v-if="item.to && index !== items.length - 1">
          <router-link
            v-if="item.to"
            :to="item.to"
            class="ar-breadcrumb__link"
          >
            <span v-if="item.icon" class="ar-breadcrumb__icon">
              <i :class="item.icon"></i>
            </span>
            <span class="ar-breadcrumb__text">{{ item.title }}</span>
          </router-link>
        </template>
        <template v-else>
          <span v-if="item.icon" class="ar-breadcrumb__icon">
            <i :class="item.icon"></i>
          </span>
          <span class="ar-breadcrumb__text">{{ item.title }}</span>
        </template>
        
        <span
          v-if="index !== items.length - 1"
          class="ar-breadcrumb__separator"
        >
          {{ separator }}
        </span>
      </li>
    </ol>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface BreadcrumbItem {
  title: string;
  to?: string | object;
  icon?: string;
}

interface Props {
  items: BreadcrumbItem[];
  separator?: string;
  backgroundColor?: string;
  textColor?: string;
  activeColor?: string;
  separatorColor?: string;
}

const props = withDefaults(defineProps<Props>(), {
  separator: '/',
  textColor: '#606266',
  activeColor: '#303133',
  separatorColor: '#c0c4cc'
});

const breadcrumbStyle = computed(() => ({
  '--ar-breadcrumb-bg': props.backgroundColor,
  '--ar-breadcrumb-text-color': props.textColor,
  '--ar-breadcrumb-active-color': props.activeColor,
  '--ar-breadcrumb-separator-color': props.separatorColor
}));
</script>

<style lang="scss" scoped>
.ar-breadcrumb {
  padding: 12px 0;
  background-color: var(--ar-breadcrumb-bg, transparent);
  font-size: 14px;

  &__list {
    display: flex;
    align-items: center;
    margin: 0;
    padding: 0;
    list-style: none;
  }

  &__item {
    display: flex;
    align-items: center;
    color: var(--ar-breadcrumb-text-color, #606266);

    &.is-active {
      color: var(--ar-breadcrumb-active-color, #303133);
      font-weight: 500;

      .ar-breadcrumb__text {
        cursor: default;
      }
    }

    &.is-link {
      .ar-breadcrumb__text {
        cursor: pointer;

        &:hover {
          color: var(--ar-breadcrumb-hover-color, #409eff);
          text-decoration: underline;
        }
      }
    }
  }

  &__link {
    display: flex;
    align-items: center;
    text-decoration: none;
    color: inherit;
    transition: color 0.2s ease;

    &:hover {
      color: var(--ar-breadcrumb-hover-color, #409eff);
    }
  }

  &__icon {
    display: inline-flex;
    align-items: center;
    margin-right: 6px;
    font-size: 16px;
  }

  &__text {
    transition: color 0.2s ease;
  }

  &__separator {
    margin: 0 8px;
    color: var(--ar-breadcrumb-separator-color, #c0c4cc);
    font-weight: 300;
    user-select: none;
  }
}
</style>