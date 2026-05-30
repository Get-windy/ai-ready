/**
 * 标准 CSS 动画
 * 可注入到 CSS-in-JS 或 <style> 标签中使用
 */

export const animations = {
  /** 淡入 */
  fadeIn: `
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
  `,
  /** 淡出 */
  fadeOut: `
    @keyframes fadeOut {
      from { opacity: 1; }
      to { opacity: 0; }
    }
  `,
  /** 从下方滑入 */
  slideUp: `
    @keyframes slideUp {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `,
  /** 从上方向下滑入 */
  slideDown: `
    @keyframes slideDown {
      from { opacity: 0; transform: translateY(-20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `,
  /** 骨架屏微光 */
  shimmer: `
    @keyframes shimmer {
      0% { background-position: -468px 0; }
      100% { background-position: 468px 0; }
    }
  `,
  /** 旋转（用于加载图标） */
  spin: `
    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
  `,
  /** 脉冲 */
  pulse: `
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }
  `,
  /** 缩放弹入 */
  scaleIn: `
    @keyframes scaleIn {
      from { opacity: 0; transform: scale(0.9); }
      to { opacity: 1; transform: scale(1); }
    }
  `,
} as const

/** 将多个动画注入到文档中 */
export function injectAnimations(doc: Document = document): void {
  const style = doc.createElement('style')
  style.id = 'ai-ready-animations'
  style.textContent = Object.values(animations).join('\n')
  doc.head.appendChild(style)
}
