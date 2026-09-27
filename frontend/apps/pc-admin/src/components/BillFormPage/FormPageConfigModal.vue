<template>
  <a-modal
    :open="open"
    title="配置"
    :width="760"
    :footer="null"
    destroy-on-close
    @update:open="(v: boolean) => emit('update:open', v)"
  >
    <a-tabs v-model:active-key="activeTab" size="small">
      <!-- Tab 1: 页面配置（字段显示名 / 显隐 / 回车跳转） -->
      <a-tab-pane key="pageConfig" tab="页面配置">
        <p class="config-hint">勾选后自动保存，该设置对所有操作员生效</p>
        <a-table
          :columns="tableColumns"
          :data-source="fields"
          :pagination="false"
          size="small"
          row-key="key"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'displayName'">
              <a-input
                :value="record.displayName"
                size="small"
                @blur="(e: any) => emit('field-display-name-change', record.key, e.target.value)"
              />
            </template>
            <template v-if="column.key === 'visible'">
              <a-checkbox
                :checked="record.visible"
                @change="(e: any) => emit('field-visible-change', record.key, e.target.checked)"
              />
            </template>
            <template v-if="column.key === 'enterJump'">
              <a-checkbox
                :checked="record.enterJump"
                @change="(e: any) => emit('field-enter-jump-change', record.key, e.target.checked)"
              />
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- Tab 2: 录单默认值（各页字段不同，由页面通过 slot 提供） -->
      <a-tab-pane key="defaultValues" tab="录单默认值">
        <slot name="defaults" />
      </a-tab-pane>

      <!-- Tab 3: 打印设置（各页一致，收敛在这里） -->
      <a-tab-pane key="printSettings" tab="打印设置">
        <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="打印模板">
            <a-select
              :value="printConfig.template"
              size="small"
              style="width: 100%"
              @update:value="(v: string) => updatePrint('template', v)"
            >
              <a-select-option value="standard">标准模板</a-select-option>
              <a-select-option value="simple">简化模板</a-select-option>
              <a-select-option value="detailed">详细模板</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="打印份数">
            <a-input-number
              :value="printConfig.copies"
              :precision="0"
              :min="1"
              size="small"
              style="width: 100%"
              @update:value="(v: number) => updatePrint('copies', v)"
            />
          </a-form-item>
          <a-form-item label="纸张大小">
            <a-select
              :value="printConfig.paperSize"
              size="small"
              style="width: 100%"
              @update:value="(v: string) => updatePrint('paperSize', v)"
            >
              <a-select-option value="A4">A4</a-select-option>
              <a-select-option value="A5">A5</a-select-option>
              <a-select-option value="B5">B5</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="打印选项">
            <a-checkbox
              :checked="printConfig.alwaysLastTemplate"
              @change="(e: any) => updatePrint('alwaysLastTemplate', e.target.checked)"
            >
              始终使用最后一次打印的模板，打印时不再选择
            </a-checkbox>
            <a-checkbox
              :checked="printConfig.afterSubmit"
              @change="(e: any) => updatePrint('afterSubmit', e.target.checked)"
            >
              记账后立即打印
            </a-checkbox>
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup lang="ts">
/**
 * 单据表单页的「配置」弹窗（页面配置 / 录单默认值 / 打印设置）。
 *
 * <p>列表页请用 `PageConfigPanel`；本组件专给**单据表单页**用 —— 两者页签与语义都不同，
 * 此前只有采购入库表单页自己写了一份内联弹窗，退货/换货两页完全没有。</p>
 *
 * <p>配套状态见 `useFormPageConfig`（字段显隐/显示名/回车跳转 + 持久化）。</p>
 */
import { ref } from 'vue'

defineOptions({ name: 'FormPageConfigModal' })

/** 打印设置（各表单页结构一致，统一收敛在这里） */
export interface FormPrintConfig {
  template: string
  copies: number
  paperSize: string
  alwaysLastTemplate: boolean
  afterSubmit: boolean
}

const props = withDefaults(defineProps<{
  open: boolean
  /** 页面配置表格数据（来自 useFormPageConfig().pageConfigFields） */
  fields: Array<Record<string, any>>
  /** 表格列定义（来自 useFormPageConfig().pageConfigTableColumns） */
  tableColumns: Array<Record<string, any>>
  printConfig: FormPrintConfig
}>(), {
  fields: () => [],
  tableColumns: () => [],
})

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'field-visible-change', key: string, visible: boolean): void
  (e: 'field-enter-jump-change', key: string, checked: boolean): void
  (e: 'field-display-name-change', key: string, displayName: string): void
  (e: 'update:printConfig', value: FormPrintConfig): void
}>()

const activeTab = ref('pageConfig')

function updatePrint<K extends keyof FormPrintConfig>(key: K, value: FormPrintConfig[K]) {
  emit('update:printConfig', { ...props.printConfig, [key]: value })
}
</script>

<style scoped>
.config-hint {
  margin: 0 0 8px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>
