<template>
  <a-cascader
    :value="innerValue"
    :options="options"
    :field-names="{ label: 'name', value: 'name', children: 'children' }"
    :placeholder="placeholder"
    :size="size"
    :allow-clear="allowClear"
    :disabled="disabled"
    :loading="loading"
    :show-search="true"
    :get-popup-container="getPopupContainer"
    change-on-select
    style="width: 100%"
    @change="handleChange"
  />
</template>

<script setup lang="ts">
/**
 * 行政区划三级联动（省 / 市 / 区县）
 *
 * 全系统公共组件：往来单位（客户 / 供应商 / 物流公司 / 其他往来单位）等所有需要
 * 「所在地区」的表单统一使用，数据源为后端 /sys/region/tree（sys_region 表，国家统计局口径）。
 *
 * 用法：
 *   <RegionCascader v-model="areaPath" />            // areaPath: string[] = [省, 市, 区]
 *   <RegionCascader v-model="areaPath" :text-value="regionText" />  // 也可用字符串双向绑定
 */
import { computed, onMounted, ref } from 'vue'
import { sysRegionApi, type SysRegion } from '@/api/sys-region'

const props = withDefaults(defineProps<{
  /** 选中路径：['甘肃省','酒泉市','肃州区']；也兼容 '甘肃省/酒泉市/肃州区' 字符串 */
  modelValue?: string[] | string | null
  placeholder?: string
  size?: 'large' | 'middle' | 'small'
  allowClear?: boolean
  disabled?: boolean
  /** 路径分隔符（用于字符串形态的 modelValue） */
  separator?: string
  /** 弹层挂载节点（在 Modal 内使用时传 false 跟随父级） */
  getPopupContainer?: (triggerNode: HTMLElement) => HTMLElement
}>(), {
  modelValue: () => [],
  placeholder: '请选择所在地区',
  size: 'small',
  allowClear: true,
  disabled: false,
  separator: '/',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
  /** 选中路径变化时同步抛出的拼接文本，便于写入单个 region 字段 */
  (e: 'change', value: string[], text: string): void
}>()

const options = ref<SysRegion[]>([])
const loading = ref(false)

async function loadOptions() {
  loading.value = true
  try {
    options.value = await sysRegionApi.tree()
  } catch (e) {
    console.warn('[RegionCascader] 行政区划加载失败', e)
    options.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadOptions)

const innerValue = computed<string[]>(() => {
  const v = props.modelValue
  if (Array.isArray(v)) return v.filter(Boolean)
  if (typeof v === 'string' && v) return v.split(props.separator).filter(Boolean)
  return []
})

function handleChange(value: any) {
  const path = (Array.isArray(value) ? value : []).filter(Boolean) as string[]
  emit('update:modelValue', path)
  emit('change', path, path.join(props.separator))
}

defineExpose({ reload: loadOptions })
</script>
