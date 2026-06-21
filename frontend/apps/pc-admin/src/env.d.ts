/* eslint-disable */
// @ts-nocheck

/// <reference types="vite/client" />

// Vue 单文件组件声明
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'vxe-pc-ui/types/components/table' {
  interface VxeTableSlots<D = any> {
    /**
     * 自定义 bodyCell 插槽
     */
    bodyCell?(params: {
      row: D
      rowIndex: number
      column: any
      columnIndex: number
      _rowIndex: number
      _columnIndex: number
    }): any
  }
}

export {}
