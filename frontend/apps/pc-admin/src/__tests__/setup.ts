/**
 * Vitest 全局 setup 文件
 * 提供 jsdom 环境缺失的浏览器 API polyfills
 */
import { vi } from 'vitest'

// ============= ResizeObserver polyfill =============
class ResizeObserverMock {
  private _callback: ResizeObserverCallback | null = null

  constructor(callback?: ResizeObserverCallback) {
    this._callback = callback ?? null
  }

  observe() {}
  unobserve() {}
  disconnect() {}
}

global.ResizeObserver = ResizeObserverMock as any

// ============= IntersectionObserver polyfill =============
class IntersectionObserverMock {
  private _callback: IntersectionObserverCallback | null = null

  constructor(callback?: IntersectionObserverCallback) {
    this._callback = callback ?? null
  }

  observe() {}
  unobserve() {}
  disconnect() {}
}

global.IntersectionObserver = IntersectionObserverMock as any

// ============= matchMedia polyfill =============
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

// ============= scrollTo polyfill =============
// jsdom 不实现 window.scrollTo
Object.defineProperty(window, 'scrollTo', {
  writable: true,
  value: vi.fn(),
})

// ============= getComputedStyle 安全补丁 =============
// 确保 getComputedStyle 不会因 null 元素抛出
const originalGetComputedStyle = window.getComputedStyle
window.getComputedStyle = (elt: Element, pseudoElt?: string | null) => {
  if (!elt) return originalGetComputedStyle(document.documentElement, pseudoElt)
  return originalGetComputedStyle(elt, pseudoElt)
}
