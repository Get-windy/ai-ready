/**
 * 轻量级消息通知工具（替代 ant-design-vue 的 message）
 */

type MessageType = 'success' | 'warning' | 'error' | 'info'

interface MessageOptions {
  content: string
  type: MessageType
  duration?: number
}

let seed = 0

function createContainer(): HTMLElement {
  let el = document.getElementById('__message_container__')
  if (!el) {
    el = document.createElement('div')
    el.id = '__message_container__'
    el.style.cssText = `
      position: fixed; top: 16px; left: 50%; transform: translateX(-50%);
      z-index: 9999; display: flex; flex-direction: column; align-items: center;
      gap: 8px; pointer-events: none;
    `
    document.body.appendChild(el)
  }
  return el
}

function show(options: MessageOptions): void {
  const { content, type, duration = 3000 } = options
  const container = createContainer()
  const id = `msg_${++seed}`
  const el = document.createElement('div')
  el.id = id
  el.style.cssText = `
    pointer-events: auto;
    padding: 10px 20px;
    border-radius: 8px;
    font-size: 14px;
    color: #fff;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    animation: msgFadeIn 0.3s ease;
    max-width: 400px;
    word-break: break-word;
    text-align: center;
  `
  const bgColors: Record<MessageType, string> = {
    success: '#07c160',
    warning: '#ff976a',
    error: '#f44',
    info: '#1988fa'
  }
  el.style.background = bgColors[type]
  el.textContent = content
  container.appendChild(el)

  setTimeout(() => {
    el.style.opacity = '0'
    el.style.transition = 'opacity 0.3s'
    setTimeout(() => el.remove(), 300)
  }, duration)
}

// 注入动画
if (typeof document !== 'undefined') {
  const style = document.createElement('style')
  style.textContent = `
    @keyframes msgFadeIn {
      from { opacity: 0; transform: translateY(-12px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `
  document.head.appendChild(style)
}

const message = {
  success(content: string, duration?: number) {
    show({ content, type: 'success', duration })
  },
  warning(content: string, duration?: number) {
    show({ content, type: 'warning', duration })
  },
  error(content: string, duration?: number) {
    show({ content, type: 'error', duration })
  },
  info(content: string, duration?: number) {
    show({ content, type: 'info', duration })
  }
}

export default message
