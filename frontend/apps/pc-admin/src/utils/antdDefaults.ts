import { Tooltip } from 'ant-design-vue'

/**
 * Ant Design Vue 组件级默认值（全局生效，无需各页重复传参）
 *
 * 背景（用户反馈 2026-09-10）：顶部工具栏的图标按钮若沿用 Tooltip 默认
 * `placement="top"`，浮层会向上弹出并压住按钮本身与上方标签栏（"遮住半边按钮"）。
 *
 * 处理：组件级改默认值——
 *   placement        : top → bottom（浮层下置，不再遮挡触发按钮）
 *   mouseEnterDelay  : 0.1 → 0.4（鼠标扫过工具栏时不误弹）
 *   mouseLeaveDelay  : 0.1 → 0.05（移开即收，避免残留）
 *
 * 说明：AntDV 的 `withInstall` 直接返回组件对象本身，因此修改 `Tooltip.props.*.default`
 * 会作用到全局所有（含按需导入的）Tooltip 使用点；`autoAdjustOverflow` 仍为 true，
 * 贴底元素会自动翻转方向，不会跑出视口。
 */
const tooltipProps = (Tooltip as any)?.props

if (tooltipProps) {
  if (tooltipProps.placement) tooltipProps.placement.default = 'bottom'
  if (tooltipProps.mouseEnterDelay) tooltipProps.mouseEnterDelay.default = 0.4
  if (tooltipProps.mouseLeaveDelay) tooltipProps.mouseLeaveDelay.default = 0.05
}
