<template>
  <div
    :class="[
      'ar-steps',
      `ar-steps--${direction}`,
      `ar-steps--${size}`,
      {
        'ar-steps--simple': simple,
        'ar-steps--center': center
      }
    ]"
    :style="stepsStyle"
  >
    <div
      v-for="(step, index) in steps"
      :key="index"
      :class="[
        'ar-step',
        {
          'is-active': step.status === 'active',
          'is-completed': step.status === 'completed',
          'is-error': step.status === 'error',
          'is-disabled': step.status === 'disabled'
        }
      ]"
    >
      <div class="ar-step__header">
        <div class="ar-step__icon">
          <template v-if="step.status === 'completed'">
            <i class="ar-step__check-icon">✓</i>
          </template>
          <template v-else-if="step.status === 'error'">
            <i class="ar-step__error-icon">✕</i>
          </template>
          <template v-else>
            <span class="ar-step__number">{{ index + 1 }}</span>
          </template>
        </div>
        
        <div v-if="direction === 'horizontal'" class="ar-step__line">
          <div
            :class="[
              'ar-step__line-inner',
              {
                'is-active': step.status === 'completed'
              }
            ]"
          ></div>
        </div>
      </div>
      
      <div class="ar-step__content">
        <div class="ar-step__title">
          {{ step.title }}
        </div>
        <div v-if="step.description" class="ar-step__description">
          {{ step.description }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Step {
  title: string;
  description?: string;
  status?: 'wait' | 'active' | 'completed' | 'error' | 'disabled';
}

interface Props {
  steps: Step[];
  direction?: 'horizontal' | 'vertical';
  size?: 'default' | 'small' | 'large';
  simple?: boolean;
  center?: boolean;
  activeColor?: string;
  completedColor?: string;
  errorColor?: string;
}

const props = withDefaults(defineProps<Props>(), {
  direction: 'horizontal',
  size: 'default',
  activeColor: '#409eff',
  completedColor: '#67c23a',
  errorColor: '#f56c6c'
});

const stepsStyle = computed(() => ({
  '--ar-steps-active-color': props.activeColor,
  '--ar-steps-completed-color': props.completedColor,
  '--ar-steps-error-color': props.errorColor
}));
</script>

<style lang="scss" scoped>
.ar-steps {
  display: flex;
  width: 100%;

  &--horizontal {
    flex-direction: row;
    justify-content: space-between;

    .ar-step {
      flex: 1;
      position: relative;
    }
  }

  &--vertical {
    flex-direction: column;

    .ar-step {
      position: relative;
      padding-left: 48px;
      margin-bottom: 20px;
    }
  }

  &--center {
    .ar-step__content {
      text-align: center;
    }
  }

  &--small {
    .ar-step__icon {
      width: 24px;
      height: 24px;
      font-size: 12px;
    }

    .ar-step__title {
      font-size: 12px;
    }

    .ar-step__description {
      font-size: 11px;
    }
  }

  &--large {
    .ar-step__icon {
      width: 40px;
      height: 40px;
      font-size: 18px;
    }

    .ar-step__title {
      font-size: 16px;
    }

    .ar-step__description {
      font-size: 14px;
    }
  }

  &--simple {
    .ar-step__icon,
    .ar-step__line {
      display: none;
    }
  }
}

.ar-step {
  display: flex;
  flex-direction: column;
  align-items: center;

  &.is-active {
    .ar-step__icon {
      border-color: var(--ar-steps-active-color, #409eff);
      color: var(--ar-steps-active-color, #409eff);
    }

    .ar-step__title {
      color: var(--ar-steps-active-color, #409eff);
      font-weight: 600;
    }
  }

  &.is-completed {
    .ar-step__icon {
      background-color: var(--ar-steps-completed-color, #67c23a);
      border-color: var(--ar-steps-completed-color, #67c23a);
      color: white;
    }

    .ar-step__title,
    .ar-step__description {
      color: var(--ar-steps-completed-color, #67c23a);
    }

    .ar-step__line-inner {
      background-color: var(--ar-steps-completed-color, #67c23a);
    }
  }

  &.is-error {
    .ar-step__icon {
      background-color: var(--ar-steps-error-color, #f56c6c);
      border-color: var(--ar-steps-error-color, #f56c6c);
      color: white;
    }

    .ar-step__title,
    .ar-step__description {
      color: var(--ar-steps-error-color, #f56c6c);
    }
  }

  &.is-disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  &__header {
    display: flex;
    align-items: center;
    width: 100%;
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: 2px solid var(--ar-steps-border-color, #e4e7ed);
    background-color: white;
    color: var(--ar-steps-text-color, #c0c4cc);
    font-size: 14px;
    font-weight: 600;
    transition: all 0.3s ease;
    flex-shrink: 0;
  }

  &__check-icon,
  &__error-icon {
    font-weight: bold;
  }

  &__line {
    flex: 1;
    height: 2px;
    background-color: var(--ar-steps-line-color, #e4e7ed);
    margin: 0 16px;

    &-inner {
      height: 100%;
      width: 0;
      background-color: var(--ar-steps-active-color, #409eff);
      transition: width 0.3s ease;

      &.is-active {
        width: 100%;
      }
    }
  }

  &__content {
    margin-top: 8px;
    text-align: center;
  }

  &__title {
    font-size: 14px;
    color: var(--ar-steps-text-color, #303133);
    transition: color 0.3s ease;
  }

  &__description {
    margin-top: 4px;
    font-size: 12px;
    color: var(--ar-steps-description-color, #909399);
    transition: color 0.3s ease;
  }
}
</style>