<template>
  <section class="form-section">
    <!-- 标题行：左侧标题 + 右侧口径说明 / 自定义操作；无标题无附属内容时不渲染（纯栅格分区） -->
    <div
      v-if="title || tip || $slots.extra || collapsible"
      class="form-section__head"
      :class="{ 'is-clickable': collapsible }"
      @click="toggleCollapsed"
    >
      <span class="form-section__title">
        {{ title }}
        <slot name="title-suffix" />
      </span>
      <div class="form-section__right">
        <span
          v-if="tip"
          class="form-section__tip"
        >{{ tip }}</span>
        <slot name="extra" />
        <span
          v-if="collapsible"
          class="form-section__toggle"
        >{{ collapsed ? '点击展开' : '点击收起' }}</span>
      </div>
    </div>
    <slot v-if="!collapsed" />
  </section>
</template>

<script setup lang="ts">
/**
 * FormSection —— 档案（主数据）表单的「分区卡片」
 *
 * 用途：字段多、语义可分组、编辑频率低的主数据表单（配送员 / 车辆 / 物流公司 / 客户 / 员工…）
 * 之前每个页面各自手写 `.section-card + .section-head + .section-title + .section-tip`（19 个文件 14 种变体），
 * 本组件收敛为唯一口径（样式取自《资料模块》金标准 views/md/logistics/form.vue）。
 *
 * 用法：
 *   <FormSection title="基本信息" tip="姓名与手机号为必填">
 *     <a-row :gutter="24"><a-col :span="12"><a-form-item label="姓名">…</a-form-item></a-col></a-row>
 *   </FormSection>
 *
 * 标题右侧需要按钮等自定义内容时用 #extra 槽（与 tip 可同时使用，tip 在前）。
 *
 * 注意：**单据**（销售订单/出库单/配送单…）的表头字段区请继续用 BillFormPage 的 inline flow，
 * 不要套本组件（判据见记忆《表单布局选型：单据 vs 档案》）。
 */
defineOptions({ name: 'FormSection' })

const props = withDefaults(defineProps<{
  /** 分区标题（如「基本信息」「资格与证件」）；不传则只渲染卡片容器与默认插槽 */
  title?: string
  /** 标题右侧的灰字口径说明（一句话讲清该区字段的业务含义） */
  tip?: string
  /** 是否可折叠（点击标题行切换）；折叠态下不渲染默认插槽 */
  collapsible?: boolean
  /** 折叠状态（配合 v-model:collapsed 使用） */
  collapsed?: boolean
}>(), {
  collapsible: false,
  collapsed: false,
})

const emit = defineEmits<{
  'update:collapsed': [value: boolean]
}>()

function toggleCollapsed() {
  if (props.collapsible) emit('update:collapsed', !props.collapsed)
}
</script>

<style scoped>
.form-section {
  background: #fff;
  border-radius: 6px;
  padding: 16px 24px 4px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.form-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.form-section__title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.form-section__right {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.form-section__tip {
  font-size: 12px;
  color: #8c8c8c;
}

.form-section__toggle {
  font-size: 12px;
  font-weight: 400;
  color: #bfbfbf;
}

.form-section__head.is-clickable {
  cursor: pointer;
  user-select: none;
}
</style>
