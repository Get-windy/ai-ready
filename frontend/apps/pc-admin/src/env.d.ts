/* eslint-disable */
// @ts-nocheck

/// <reference types="vite/client" />

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
