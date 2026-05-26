<template>
  <article :class="cardClasses" @click="handleClick">
    <!-- 卡片头部：供应商logo和名称 -->
    <div class="supplier-card__header">
      <div class="supplier-card__logo">
        <img 
          v-if="supplier.logo" 
          :src="supplier.logo" 
          :alt="`${supplier.name} logo`"
          class="supplier-card__logo-image"
        />
        <div v-else class="supplier-card__logo-placeholder">
          {{ supplier.name.charAt(0).toUpperCase() }}
        </div>
      </div>
      
      <div class="supplier-card__header-info">
        <h3 class="supplier-card__name">{{ supplier.name }}</h3>
        <div class="supplier-card__meta">
          <span class="supplier-card__id">ID: {{ supplier.id }}</span>
          <span class="supplier-card__type">{{ supplier.type }}</span>
        </div>
      </div>
      
      <!-- 状态标记 -->
      <div :class="statusClasses">
        <span class="supplier-card__status-dot"></span>
        <span class="supplier-card__status-text">{{ statusText }}</span>
      </div>
    </div>

    <!-- 卡片内容：供应商信息 -->
    <div class="supplier-card__content">
      <!-- 联系方式 -->
      <div v-if="supplier.contact" class="supplier-card__contact">
        <div class="supplier-card__contact-item">
          <i class="supplier-card__contact-icon">📞</i>
          <span class="supplier-card__contact-text">{{ supplier.contact.phone }}</span>
        </div>
        <div class="supplier-card__contact-item">
          <i class="supplier-card__contact-icon">📧</i>
          <span class="supplier-card__contact-text">{{ supplier.contact.email }}</span>
        </div>
      </div>

      <!-- 地址信息 -->
      <div v-if="supplier.address" class="supplier-card__address">
        <i class="supplier-card__address-icon">📍</i>
        <span class="supplier-card__address-text">{{ supplier.address }}</span>
      </div>

      <!-- 资质信息 -->
      <div v-if="supplier.qualifications" class="supplier-card__qualifications">
        <div class="supplier-card__qualifications-title">
          <span>资质认证</span>
          <span class="supplier-card__qualifications-count">
            {{ supplier.qualifications.length }}项
          </span>
        </div>
        <div class="supplier-card__qualifications-tags">
          <span 
            v-for="qualification in supplier.qualifications.slice(0, 3)" 
            :key="qualification"
            class="supplier-card__qualification-tag"
          >
            {{ qualification }}
          </span>
          <span 
            v-if="supplier.qualifications.length > 3"
            class="supplier-card__qualification-more"
          >
            +{{ supplier.qualifications.length - 3 }}
          </span>
        </div>
      </div>

      <!-- 合作指标 -->
      <div v-if="supplier.metrics" class="supplier-card__metrics">
        <div class="supplier-card__metrics-item">
          <span class="supplier-card__metrics-label">合作时长</span>
          <span class="supplier-card__metrics-value">{{ supplier.metrics.cooperationYears }}年</span>
        </div>
        <div class="supplier-card__metrics-item">
          <span class="supplier-card__metrics-label">订单数量</span>
          <span class="supplier-card__metrics-value">{{ supplier.metrics.orderCount }}笔</span>
        </div>
        <div class="supplier-card__metrics-item">
          <span class="supplier-card__metrics-label">评分</span>
          <span class="supplier-card__metrics-value">{{ supplier.metrics.rating }}分</span>
        </div>
      </div>

      <!-- 插槽内容 -->
      <slot />
    </div>

    <!-- 卡片底部：操作按钮 -->
    <div v-if="showActions" class="supplier-card__footer">
      <slot name="actions">
        <button 
          class="supplier-card__action-btn supplier-card__action-btn--primary"
          @click.stop="handleAction('detail')"
        >
          查看详情
        </button>
        <button 
          class="supplier-card__action-btn supplier-card__action-btn--secondary"
          @click.stop="handleAction('contact')"
        >
          联系供应商
        </button>
      </slot>
    </div>

    <!-- 角标（如推荐、新品等） -->
    <div v-if="supplier.badge" :class="badgeClasses">
      {{ supplier.badge }}
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Supplier, SupplierStatus } from '../types/supplier'

// 组件属性定义
interface Props {
  supplier: Supplier
  variant?: 'default' | 'compact' | 'detailed'
  clickable?: boolean
  showActions?: boolean
  highlight?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  clickable: true,
  showActions: true,
  highlight: false
})

// 定义事件
const emit = defineEmits<{
  click: [supplier: Supplier]
  action: [action: string, supplier: Supplier]
}>()

// 计算属性
const cardClasses = computed(() => ({
  'supplier-card': true,
  [`supplier-card--${props.variant}`]: true,
  'supplier-card--clickable': props.clickable,
  'supplier-card--highlight': props.highlight,
  [`supplier-card--status-${props.supplier.status}`]: true
}))

const statusClasses = computed(() => ({
  'supplier-card__status': true,
  [`supplier-card__status--${props.supplier.status}`]: true
}))

const statusText = computed(() => {
  const statusMap: Record<SupplierStatus, string> = {
    active: '合作中',
    pending: '待审核',
    inactive: '已停用',
    rejected: '已拒绝'
  }
  return statusMap[props.supplier.status] || '未知状态'
})

const badgeClasses = computed(() => ({
  'supplier-card__badge': true,
  [`supplier-card__badge--${props.supplier.badgeType || 'default'}`]: true
}))

// 事件处理
const handleClick = () => {
  if (props.clickable) {
    emit('click', props.supplier)
  }
}

const handleAction = (action: string) => {
  emit('action', action, props.supplier)
}

// 暴露给父组件的方法
defineExpose({
  getSupplierInfo: () => props.supplier
})
</script>

<style scoped>
.supplier-card {
  position: relative;
  background: var(--color-supplier-bg-card);
  border: 1px solid var(--color-supplier-border);
  border-radius: var(--card-radius);
  padding: var(--space-6);
  transition: all var(--duration-normal) var(--ease-in-out);
  overflow: hidden;
}

.supplier-card--clickable {
  cursor: pointer;
}

.supplier-card--clickable:hover {
  border-color: var(--color-supplier-primary);
  box-shadow: var(--hover-shadow);
  transform: translateY(-2px);
}

.supplier-card--highlight {
  border-left: 4px solid var(--color-supplier-primary);
}

/* 头部样式 */
.supplier-card__header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.supplier-card__logo {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
}

.supplier-card__logo-image {
  width: 100%;
  height: 100%;
  border-radius: var(--radius-md);
  object-fit: cover;
}

.supplier-card__logo-placeholder {
  width: 100%;
  height: 100%;
  border-radius: var(--radius-md);
  background: var(--color-supplier-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}

.supplier-card__header-info {
  flex: 1;
  min-width: 0;
}

.supplier-card__name {
  margin: 0 0 var(--space-1);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-supplier-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.supplier-card__meta {
  display: flex;
  gap: var(--space-2);
  font-size: var(--font-size-xs);
  color: var(--color-supplier-text-secondary);
}

.supplier-card__id {
  font-family: var(--font-family-mono);
}

/* 状态样式 */
.supplier-card__status {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
}

.supplier-card__status--active {
  background: rgba(102, 187, 106, 0.1);
  color: #2e7d32;
}

.supplier-card__status--pending {
  background: rgba(255, 167, 38, 0.1);
  color: #ff8f00;
}

.supplier-card__status--inactive {
  background: rgba(239, 83, 80, 0.1);
  color: #d32f2f;
}

.supplier-card__status--rejected {
  background: rgba(158, 158, 158, 0.1);
  color: #616161;
}

.supplier-card__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* 内容区域样式 */
.supplier-card__content {
  margin-bottom: var(--space-4);
}

.supplier-card__contact,
.supplier-card__address,
.supplier-card__qualifications,
.supplier-card__metrics {
  margin-bottom: var(--space-3);
}

.supplier-card__contact-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-1);
  font-size: var(--font-size-sm);
  color: var(--color-supplier-text-secondary);
}

.supplier-card__address {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  color: var(--color-supplier-text-secondary);
}

/* 资质标签 */
.supplier-card__qualifications-title {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-2);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-supplier-text-primary);
}

.supplier-card__qualifications-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.supplier-card__qualification-tag {
  padding: var(--space-1) var(--space-2);
  background: var(--color-supplier-bg-light);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--color-supplier-text-secondary);
}

.supplier-card__qualification-more {
  padding: var(--space-1) var(--space-2);
  font-size: var(--font-size-xs);
  color: var(--color-supplier-primary);
}

/* 指标样式 */
.supplier-card__metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-2);
  padding: var(--space-2);
  background: var(--color-supplier-bg-light);
  border-radius: var(--radius-md);
}

.supplier-card__metrics-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.supplier-card__metrics-label {
  font-size: var(--font-size-xs);
  color: var(--color-supplier-text-secondary);
  margin-bottom: var(--space-1);
}

.supplier-card__metrics-value {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-supplier-text-primary);
}

/* 底部操作按钮 */
.supplier-card__footer {
  display: flex;
  gap: var(--space-2);
}

.supplier-card__action-btn {
  flex: 1;
  padding: var(--space-2) var(--space-4);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-in-out);
}

.supplier-card__action-btn--primary {
  background: var(--color-supplier-primary);
  color: white;
}

.supplier-card__action-btn--primary:hover {
  background: var(--color-supplier-primary-hover);
}

.supplier-card__action-btn--secondary {
  background: white;
  border-color: var(--color-supplier-border);
  color: var(--color-supplier-text-primary);
}

.supplier-card__action-btn--secondary:hover {
  border-color: var(--color-supplier-primary);
  color: var(--color-supplier-primary);
}

/* 角标样式 */
.supplier-card__badge {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-transform: uppercase;
}

.supplier-card__badge--recommended {
  background: var(--color-transaction-completed);
  color: white;
}

.supplier-card__badge--new {
  background: var(--color-supplier-primary);
  color: white;
}

/* 紧凑模式 */
.supplier-card--compact {
  padding: var(--space-4);
}

.supplier-card--compact .supplier-card__logo {
  width: 36px;
  height: 36px;
}

.supplier-card--compact .supplier-card__name {
  font-size: var(--font-size-base);
}

.supplier-card--compact .supplier-card__content {
  display: none;
}

/* 详细模式 */
.supplier-card--detailed {
  padding: var(--space-8);
}

.supplier-card--detailed .supplier-card__header {
  margin-bottom: var(--space-6);
}

.supplier-card--detailed .supplier-card__logo {
  width: 64px;
  height: 64px;
}

.supplier-card--detailed .supplier-card__name {
  font-size: var(--font-size-xl);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .supplier-card__header {
    flex-wrap: wrap;
  }
  
  .supplier-card__status {
    order: 3;
    width: 100%;
    margin-top: var(--space-2);
  }
  
  .supplier-card__footer {
    flex-direction: column;
  }
}
</style>