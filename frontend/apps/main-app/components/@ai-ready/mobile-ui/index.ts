/**
 * AI-Ready Mobile UI 组件库
 * @package @ai-ready/mobile-ui
 * @description 企智连移动端UI组件库 - 基于Vue 3的移动端组件库
 */

// 导出类型
export * from './types'

// 导出组合式函数
export * from './composables'

// 导出组件
export * from './components'

// 导入样式
import './styles/index.scss'

// 组件列表
import { ARButton } from './components/ARButton'
import { ARInput } from './components/ARInput'
import { ARList } from './components/ARList'
import { ARCard } from './components/ARCard'

// 所有组件
export const allComponents = {
  ARButton,
  ARInput,
  ARList,
  ARCard,
}

// 插件安装
export default {
  install(app: import('vue').App) {
    Object.entries(allComponents).forEach(([name, component]) => {
      app.component(name, component)
    })
  },
}
