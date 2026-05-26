/**
 * AI-Ready Mobile UI 组件库入口
 * @package @ai-ready/mobile-ui
 */

// 按钮组件
export { ARButton } from './ARButton'
export type { IButtonProps, IButtonEmits } from '../types'

// 输入框组件
export { ARInput } from './ARInput'

// 列表组件
export { ARList } from './ARList'

// 卡片组件
export { ARCard } from './ARCard'

// 组件映射
export const components = {
  ARButton: () => import('./ARButton/ARButton.vue'),
  ARInput: () => import('./ARInput/ARInput.vue'),
  ARList: () => import('./ARList/ARList.vue'),
  ARCard: () => import('./ARCard/ARCard.vue'),
}
