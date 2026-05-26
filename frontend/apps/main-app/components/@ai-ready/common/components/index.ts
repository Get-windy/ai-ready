/**
 * AI-Ready 基础组件库
 * 包含可复用的基础 UI 组件
 */

import { App } from 'vue'
import ARButton from './base/ARButton.vue'
import ARInput from './base/ARInput.vue'
import ARCard from './base/ARCard.vue'
import ARForm from './base/ARForm.vue'
import ARFormItem from './base/ARFormItem.vue'
import ARSelect from './base/ARSelect.vue'
import ARCheckbox from './base/ARCheckbox.vue'
import ARRadioButton from './base/ARRadioButton.vue'

// 布局组件
import ARSidebar from './layout/ARSidebar.vue'
import ARMobileTabBar from './layout/ARMobileTabBar.vue'

// 表格组件
import ARMobileTable from './table/ARMobileTable.vue'

// 导出组件
import ARExportDialog from './export/ARExportDialog.vue'

// 组件列表
const components = [
  ARButton,
  ARInput,
  ARCard,
  ARForm,
  ARFormItem,
  ARSelect,
  ARCheckbox,
  ARRadioButton,
  ARSidebar,
  ARMobileTabBar,
  ARMobileTable,
  ARExportDialog,
]

// 安装方法
const install = (app: App) => {
  components.forEach((component: any) => {
    const name = component.name || component.__name
    if (name) {
      app.component(name, component)
    }
  })
}

// 导出单个组件
export {
  ARButton,
  ARInput,
  ARCard,
  ARForm,
  ARFormItem,
  ARSelect,
  ARCheckbox,
  ARRadioButton,
  ARSidebar,
  ARMobileTabBar,
  ARMobileTable,
  ARExportDialog,
}

// 导出类型
export type {
  ButtonProps,
} from './base/ARButton.vue'

export type {
  InputProps,
} from './base/ARInput.vue'

export type {
  CardProps,
} from './base/ARCard.vue'

export type {
  FormProps,
} from './base/ARForm.vue'

export type {
  FormItemProps,
  FormItemRule,
} from './base/ARFormItem.vue'

export type {
  ISelectProps,
  ISelectEmits,
  ISelectOption,
} from './base/ARSelect.vue'

export type {
  ICheckboxProps,
  ICheckboxEmits,
} from './base/ARCheckbox.vue'

export type {
  IRadioProps,
  IRadioEmits,
} from './base/ARRadioButton.vue'

// 默认导出
export default {
  install,
}