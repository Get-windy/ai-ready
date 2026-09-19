<template>
  <!--
    系统参数页的「配置项行」递归渲染器（卡片体里的子项 / 孙项 / 更深层）

    为什么需要递归：ql361 的「消息提醒」标签是**三层结构** —— 通知事件（销售订单通知…）
      → 通知对象（通知客户 / 通知经手人 / 通知操作员…）→ 通知方式（短信通知 / 系统通知）。
    这些层级都落在 `sys_config.parent_key` 上，页面按 parent_key 组装成树，
    这里按树递归渲染 —— **新增配置项不需要改前端代码**（纯数据驱动）。

    行的两种形态：
      · value_type = 'group' → 只有标题，没有自身的控件（分组行，如「通知经手人」）；
        它的**布尔子项**按 ql361 的排布**内联**在这一行的右侧（`通知经手人： ☑短信通知 ☐系统通知`），
        非布尔子项（下拉/数字）仍单独成行。
      · 其它              → boolean 用勾选框，其余交给 ParamControl 按 value_type 自适。

    锁定的行（locked = true，后端按「商品是否引用」实时算出）渲染为**禁用 + 悬浮说明原因**。
  -->
  <div
    class="param-node"
    :data-param-node="node.key"
  >
    <div
      class="param-row"
      :style="{ paddingInlineStart: indent }"
    >
      <span class="param-row__label">
        {{ node.name }}
        <a-tooltip
          v-if="node.help"
          :title="node.help"
          placement="bottom"
        >
          <QuestionCircleOutlined
            class="param-help"
            data-testid="param-help"
          />
        </a-tooltip>
        <a-tag
          v-if="node.locked"
          color="default"
          class="param-tag"
          data-testid="param-locked"
        >
          已锁定
        </a-tag>
      </span>

      <span class="param-row__control">
        <!-- ① 分组行的内联布尔子项（ql361：通知对象后面直接跟「短信通知 / 系统通知」两个勾选框） -->
        <span
          v-if="inlineChildren.length"
          class="param-inline"
        >
          <a-tooltip
            v-for="child in inlineChildren"
            :key="child.key"
            :title="child.locked ? (child.lockedReason || '该配置已被商品引用，不能更改') : (child.help || '')"
            placement="bottom"
          >
            <a-checkbox
              :checked="isOn(child.key)"
              :disabled="child.locked"
              :data-param-key="child.key"
              @change="(e: any) => emit('update', child.key, e.target.checked ? 'true' : 'false')"
            >
              {{ child.name }}
            </a-checkbox>
          </a-tooltip>
        </span>

        <!-- ② 锁定行：控件禁用 + 悬浮说明「为什么不能改」 -->
        <a-tooltip
          v-else-if="node.locked && node.valueType !== 'group'"
          :title="node.lockedReason || '该配置已被商品引用，不能更改'"
          placement="bottom"
        >
          <a-checkbox
            v-if="node.valueType === 'boolean'"
            :checked="isOn"
            disabled
            :data-param-key="node.key"
          />
          <ParamControl
            v-else
            :item-key="node.key"
            :value-type="node.valueType"
            :value="value"
            :options="optionsOf"
            :segment-options="segmentOptionsOf"
            disabled
          />
        </a-tooltip>

        <!-- ③ 普通行 -->
        <template v-else>
          <a-checkbox
            v-if="node.valueType === 'boolean'"
            :checked="isOn"
            :data-param-key="node.key"
            @change="(e: any) => emit('update', node.key, e.target.checked ? 'true' : 'false')"
          />
          <ParamControl
            v-else-if="node.valueType !== 'group'"
            :item-key="node.key"
            :value-type="node.valueType"
            :value="value"
            :options="optionsOf"
            :segment-options="segmentOptionsOf"
            @update="(v: string) => emit('update', node.key, v)"
          />
        </template>
      </span>
    </div>

    <div
      v-if="node.tip"
      class="param-tip"
      :style="{ paddingInlineStart: indent }"
    >
      温馨提示：{{ node.tip }}
    </div>

    <!-- 递归渲染「未内联」的子项（层级与顺序全部由 parent_key + sort_order 决定） -->
    <ParamNodeRow
      v-for="child in rowChildren"
      :key="child.key"
      :node="child"
      :depth="depth + 1"
      :values="values"
      :enum-options="enumOptions"
      :segment-options="segmentOptions"
      @update="(key: string, v: string) => emit('update', key, v)"
    />

    <!-- 内联布尔子项自己的温馨提示（ql361 把提示挂在被引用的那一项上，别丢） -->
    <div
      v-for="child in inlineChildren"
      v-show="child.tip"
      :key="`tip-${child.key}`"
      class="param-tip"
      :style="{ paddingInlineStart: indent }"
    >
      温馨提示：{{ child.tip }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import ParamControl from './ParamControl.vue'
import type { ConfigValueOption } from '@/api/config'

defineOptions({ name: 'ParamNodeRow' })

/** 配置项节点（= 卡片体里的一行；children 为它的子项） */
export interface ParamNode {
  key: string
  name: string
  valueType: string
  help: string
  tip: string
  locked: boolean
  lockedReason: string
  enabled: boolean
  children: ParamNode[]
}

const props = withDefaults(defineProps<{
  node: ParamNode
  /** 缩进层级（0 = 卡片体下的第一层子项） */
  depth?: number
  /** 全部值（配置键 → 值），统一按字符串处理 */
  values?: Record<string, string>
  /** 单选的候选值（配置键 → 选项集） */
  enumOptions?: Record<string, ConfigValueOption[]>
  /** 多段控件的分段候选值（配置键 → 按段的选项集） */
  segmentOptions?: Record<string, ConfigValueOption[][]>
}>(), {
  depth: 0,
  values: () => ({}),
  enumOptions: () => ({}),
  segmentOptions: () => ({}),
})

const emit = defineEmits<{
  (e: 'update', key: string, value: string): void
}>()

/** 每层缩进 20px（与页面的 .param-row--child 视觉一致） */
const indent = computed(() => `${(props.depth + 1) * 20}px`)
const value = computed(() => props.values[props.node.key] ?? '')
const isOn = computed(() => value.value === 'true')
const optionsOf = computed(() => props.enumOptions[props.node.key] || [])
const segmentOptionsOf = computed(() => props.segmentOptions[props.node.key] || [])

/** 分组行里内联显示的布尔子项（ql361：`通知经手人： ☑短信通知 ☐系统通知`） */
const inlineChildren = computed(() => (props.node.valueType === 'group'
  ? props.node.children.filter(child => child.valueType === 'boolean')
  : []))

/** 需要单独成行渲染的子项（非分组行 = 全部子项；分组行 = 剔除已内联的布尔子项） */
const rowChildren = computed(() => (props.node.valueType === 'group'
  ? props.node.children.filter(child => child.valueType !== 'boolean')
  : props.node.children))

// 说明：本组件的样式必须自备 —— 页面里同名的 scoped 类名**不会**作用到组件内部 DOM
// （Vue scoped 只对当前组件模板生成 data-v 属性），故这里重复一份行/提示样式。
</script>

<style scoped>
.param-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
}

.param-row__label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #595959;
}

.param-row__control {
  flex-shrink: 0;
}

/* 分组行右侧内联的勾选框组 */
.param-inline {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.param-help {
  color: #8c8c8c;
  cursor: help;
  font-size: 13px;
}

.param-tag {
  margin-inline-start: 4px;
  font-weight: 400;
}

.param-tip {
  padding: 2px 0 4px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
}
</style>
