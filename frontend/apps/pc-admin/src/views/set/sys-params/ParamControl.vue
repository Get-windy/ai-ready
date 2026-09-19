<template>
  <!--
    按 value_type 分发的配置项控件（四选一，互斥分支）：
      list   → 多段下拉（ql361「批次条码规则生成 = 无 + 无 + 无」，段数由当前值逗号分隔决定；
               每段的候选值优先取 `segmentOptions[段序号]`，没有分段候选值时回退扁平候选值）
      enum   → 枚举下拉（候选值来自后端 /config/value-options，未采集到的候选值不编造）
      json   → 多行文本（当前无此类数据，保底）
      其它   → 文本输入（string/number，以及无候选值的 enum；存量 8 行如「系统名称」走这里）
  -->
  <span
    v-if="valueType === 'list'"
    class="param-list"
  >
    <template
      v-for="(part, index) in listParts"
      :key="index"
    >
      <span
        v-if="index > 0"
        class="param-list__join"
      >+</span>
      <a-select
        :value="part"
        :options="segmentOptionsFor(index)"
        :disabled="disabled"
        :data-param-key="itemKey"
        style="width: 120px"
        @change="(v: any) => onListChange(index, v)"
      />
    </template>
  </span>
  <a-select
    v-else-if="valueType === 'enum' && options.length"
    :value="value"
    :options="options"
    :disabled="disabled"
    :data-param-key="itemKey"
    style="width: 220px"
    @change="(v: any) => emit('update', String(v ?? ''))"
  />
  <a-textarea
    v-else-if="valueType === 'json'"
    :value="value"
    :rows="2"
    :disabled="disabled"
    :data-param-key="itemKey"
    @update:value="(v: string) => emit('update', v ?? '')"
  />
  <a-input
    v-else
    :value="value"
    :disabled="disabled"
    :data-param-key="itemKey"
    style="width: 260px"
    placeholder="请填写配置值"
    @update:value="(v: string) => emit('update', v ?? '')"
  />
</template>

<script setup lang="ts">
/**
 * 系统参数页的配置项控件（按 `value_type` 自适）
 *
 * 布尔型不在此组件内（开关/复选由页面的卡片头与子项行直接渲染，便于保持「开关右对齐」的形态）；
 * 这里只处理：多段下拉（list）/ 枚举下拉（enum）/ 结构化（json）/ 文本（string、number）。
 */
import { computed } from 'vue'
import type { ConfigValueOption } from '@/api/config'

defineOptions({ name: 'SysParamControl' })

const props = withDefaults(defineProps<{
  /** 配置键（用于给控件打 data-param-key，供联调/验收定位） */
  itemKey?: string
  /** 值类型：string/number/boolean/enum/list/json */
  valueType?: string
  /** 当前值（sys_config.param_value 是文本列，统一按字符串处理） */
  value?: string
  /** 枚举/多段控件的候选值（扁平形态） */
  options?: ConfigValueOption[]
  /** 多段控件的**分段**候选值（外层下标 = 段序号；有值时优先于 {@link options}） */
  segmentOptions?: ConfigValueOption[][]
  /** 是否禁用（不可逆配置 locked = true 时禁用） */
  disabled?: boolean
}>(), {
  itemKey: '',
  valueType: 'string',
  value: '',
  options: () => [],
  segmentOptions: () => [],
  disabled: false,
})

const emit = defineEmits<{
  (e: 'update', value: string): void
}>()

/** 多段值的分段（空值也保底 1 段，保证至少能看到一个下拉） */
const listParts = computed(() => (props.value ? props.value.split(',') : ['']))

/** 取第 index 段的候选值：优先分段候选值，没有时回退扁平候选值 */
function segmentOptionsFor(index: number): ConfigValueOption[] {
  const perSegment = props.segmentOptions?.[index]
  return perSegment && perSegment.length ? perSegment : props.options
}

function onListChange(index: number, v: any) {
  const next = [...listParts.value]
  next[index] = String(v ?? '')
  emit('update', next.join(','))
}
</script>

<style scoped>
.param-list {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.param-list__join {
  color: #8c8c8c;
}
</style>
