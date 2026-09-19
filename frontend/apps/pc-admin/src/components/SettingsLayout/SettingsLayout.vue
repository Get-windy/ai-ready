<template>
  <div class="settings-layout">
    <!-- 左列：纵向标签（视图选择器） -->
    <aside
      class="settings-layout__side"
      :style="{ width: `${sideWidth}px` }"
    >
      <div
        v-if="title"
        class="settings-layout__side-title"
      >
        {{ title }}
      </div>
      <div
        v-for="item in items"
        :key="item.key"
        class="settings-layout__tab"
        :class="{ 'is-active': item.key === modelValue }"
        role="tab"
        :aria-selected="item.key === modelValue"
        :tabindex="item.key === modelValue ? 0 : -1"
        @click="handleSelect(item.key)"
        @keydown.enter="handleSelect(item.key)"
      >
        <span class="settings-layout__tab-text">{{ item.label }}</span>
        <span
          v-if="item.badge !== undefined && item.badge !== null"
          class="settings-layout__tab-badge"
        >
          {{ item.badge }}
        </span>
      </div>
    </aside>

    <!-- 右栏：当前视图内容（由页面通过默认插槽提供） -->
    <section class="settings-layout__main">
      <slot />
    </section>
  </div>
</template>

<script setup lang="ts">
/**
 * SettingsLayout - 设置模块「参数类」页面的通用外壳
 *
 * 形态：**左列纵向标签（视图选择器）+ 右栏内容插槽**。
 * 对标 ql361「设置 → 系统配置 → 系统参数」的「左列纵向 8 标签 + 右栏分组卡片表单 + 底部统一保存」，
 * 本组件只负责**左列与整体布局**，右栏内容（分组卡片 / 底部保存按钮）由页面自由组合 ——
 * 设置模块的其它参数类页面（打印设置 / 企业信息 / 支付配置 / 应用中心）可直接复用，
 * 不必每页自绘一套左导航。
 *
 * 设计口径：
 *   · 只借**交互形态**（纵向标签导航 + 选中态左侧竖条），**不抄** ql361 的橙色皮肤；
 *     配色走本系统主题（antd v5 主色变量 `--ant-color-primary`，回退 #1677ff）。
 *   · 标签项由调用方传入（通常来自后端接口，如 `GET /config/nav-groups`），组件内不写死业务标签。
 *   · 受控组件：`modelValue` + `update:modelValue`，并抛出 `change` 事件。
 *
 * 用法：
 * ```vue
 * <SettingsLayout v-model="currentView" :items="navItems">
 *   <div class="param-card">…当前视图的卡片…</div>
 * </SettingsLayout>
 * ```
 */
defineOptions({ name: 'SettingsLayout' })

/** 左列标签项 */
export interface SettingsLayoutItem {
  /** 唯一标识（视图编码，如 industry） */
  key: string
  /** 展示文案（如「行业设置」） */
  label: string
  /** 可选角标（如该视图下的配置项数量） */
  badge?: number | string
}

const props = withDefaults(
  defineProps<{
    /** 标签项（key 唯一） */
    items: SettingsLayoutItem[]
    /** 当前选中的标签 key（v-model） */
    modelValue: string
    /** 左列标题（可选） */
    title?: string
    /** 左列宽度（px） */
    sideWidth?: number
  }>(),
  {
    title: '',
    sideWidth: 168,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', key: string): void
  (e: 'change', key: string): void
}>()

function handleSelect(key: string) {
  if (key === props.modelValue) return
  emit('update:modelValue', key)
  emit('change', key)
}
</script>

<style scoped>
.settings-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 16px;
  padding: 12px 16px;
  box-sizing: border-box;
  overflow: hidden;
}

/* ── 左列：纵向标签 ── */
.settings-layout__side {
  flex-shrink: 0;
  overflow: auto;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  padding: 8px 0;
}

.settings-layout__side-title {
  padding: 4px 16px 10px;
  font-size: 13px;
  color: #8c8c8c;
}

.settings-layout__tab {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  /* 选中态左侧竖条：未选中时也占位，避免选中时文字横向跳动 */
  border-left: 3px solid transparent;
  transition: background-color 0.2s, color 0.2s;
  user-select: none;
}

.settings-layout__tab:hover {
  background: #fafafa;
}

.settings-layout__tab.is-active {
  color: var(--ant-color-primary, #1677ff);
  background: #e6f4ff;
  border-left-color: var(--ant-color-primary, #1677ff);
  font-weight: 600;
}

.settings-layout__tab-badge {
  min-width: 20px;
  height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  background: #f0f0f0;
  color: #8c8c8c;
  font-size: 12px;
  font-weight: 400;
  line-height: 18px;
  text-align: center;
}

.settings-layout__tab.is-active .settings-layout__tab-badge {
  background: #fff;
  color: var(--ant-color-primary, #1677ff);
}

/* ── 右栏：内容 ── */
.settings-layout__main {
  flex: 1;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
}
</style>
