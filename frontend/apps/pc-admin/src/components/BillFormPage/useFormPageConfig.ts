import { computed, reactive } from 'vue'
import { userPageConfigApi } from '@/api/erp'
import type { BasicInfoField } from './types'

/** 单个基本信息字段的页面配置 */
export interface FormFieldConfig {
  /** 是否显示 */
  visible: boolean
  /** 回车是否跳转下一格 */
  enterJump: boolean
  /** 全局显示名（留空则用字段自身的 label） */
  displayName: string
}

export interface UseFormPageConfigOptions {
  /** 基本信息字段全集（各页静态定义；动态选项请放 decorate） */
  baseFields: BasicInfoField[]
  /** 后端页面配置的模块标识，如 `purchase-return-form`；page 固定 `form` */
  module: string
  page?: string
  /** 默认隐藏的字段 key */
  defaultHiddenFields?: string[]
  /** 把动态选项/加载态等贴到字段上（各页不同） */
  decorate?: (field: BasicInfoField) => Partial<BasicInfoField>
  /** 保存时附带收集的「录单默认值 / 打印设置」（各页字段不同，由页面提供） */
  collectDefaults?: () => Record<string, any>
  /** 加载时回填「录单默认值 / 打印设置」 */
  applyDefaults?: (defaults: Record<string, any>) => void
}

/**
 * 单据表单页的「页面配置」状态与持久化。
 *
 * <p>为什么抽出来：采购订单/入库/退货/换货等单据表单页都需要同一套能力
 * （字段显隐 + 显示名 + 回车跳转 + 录单默认值 + 打印设置，持久化到 `userPageConfigApi`），
 * 而此前只有入库表单页自己写了一遍内联弹窗，退货/换货两页完全没有 —— 于是「页面配置」
 * 在文档里要求 35/36 个字段，落地为 0（2026-09-22 审计 P1）。
 * 注意：**列表页用的 `PageConfigPanel` 不适用于单据表单页**（它的页签是查询条件/功能按钮）。</p>
 */
export function useFormPageConfig(options: UseFormPageConfigOptions) {
  const {
    baseFields,
    module,
    page = 'form',
    defaultHiddenFields = [],
    decorate,
    collectDefaults,
    applyDefaults,
  } = options

  const pageConfig = reactive<Record<string, FormFieldConfig>>({})
  for (const f of baseFields) {
    pageConfig[f.key] = {
      visible: !defaultHiddenFields.includes(f.key),
      enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
      displayName: f.label,
    }
  }

  /** 实际渲染的基本信息字段：过滤隐藏项 + 套用显示名 + 注入动态选项/加载态 */
  const basicInfoFields = computed<BasicInfoField[]>(() =>
    baseFields
      .filter(f => pageConfig[f.key]?.visible !== false)
      .map(f => ({
        ...f,
        label: pageConfig[f.key]?.displayName || f.label,
        ...(decorate ? decorate(f) : {}),
      }) as BasicInfoField)
  )

  /**
   * 页面配置弹窗里的表格数据。返回**可变副本**（不是纯 computed 只读），
   * 否则「显示名」列改不动 —— 入库表单页此前的实现就是写进 computed 的临时对象里，
   * 刷新即丢，形同虚设。
   */
  const pageConfigFields = computed(() =>
    baseFields.map((f, i) => ({
      key: f.key,
      index: i + 1,
      name: f.label,
      displayName: pageConfig[f.key]?.displayName || f.label,
      visible: pageConfig[f.key]?.visible !== false,
      enterJump: pageConfig[f.key]?.enterJump ?? false,
    }))
  )

  const pageConfigTableColumns = [
    { title: '序号', key: 'index', width: 60 },
    { title: '名称', key: 'name', width: 120 },
    { title: '显示名', key: 'displayName', width: 160 },
    { title: '显示', key: 'visible', width: 70, align: 'center' as const },
    { title: '回车键跳转', key: 'enterJump', width: 100, align: 'center' as const },
  ]

  /** 加载已保存的配置；返回 defaults 供调用方回填（同时也会调 applyDefaults） */
  async function loadConfig(): Promise<Record<string, any> | null> {
    try {
      const raw = await userPageConfigApi.get(module, page)
      if (!raw) return null
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
      if (parsed.fields && typeof parsed.fields === 'object') {
        Object.keys(parsed.fields).forEach((k) => {
          if (pageConfig[k]) {
            pageConfig[k].visible = parsed.fields[k].visible !== false
            pageConfig[k].enterJump = !!parsed.fields[k].enterJump
            if (parsed.fields[k].displayName) {
              pageConfig[k].displayName = String(parsed.fields[k].displayName)
            }
          }
        })
      }
      const defaults = parsed.defaults || null
      if (defaults && applyDefaults) applyDefaults(defaults)
      return defaults
    } catch {
      // API 不可用时保持默认，不阻断录单
      return null
    }
  }

  /** 保存配置（字段显隐 + 页面自带的录单默认值/打印设置） */
  async function saveConfig(): Promise<void> {
    const payload = {
      fields: Object.fromEntries(
        Object.entries(pageConfig).map(([k, v]) => [
          k,
          { visible: v.visible, enterJump: v.enterJump, displayName: v.displayName },
        ])
      ),
      defaults: collectDefaults ? collectDefaults() : {},
    }
    try {
      await userPageConfigApi.save(module, page, JSON.stringify(payload))
    } catch {
      // 静默失败：配置存不下不应阻断录单
    }
  }

  function setFieldVisible(fieldKey: string, visible: boolean) {
    if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
    saveConfig()
  }

  function setEnterJump(fieldKey: string, checked: boolean) {
    if (pageConfig[fieldKey]) pageConfig[fieldKey].enterJump = checked
    saveConfig()
  }

  function setDisplayName(fieldKey: string, displayName: string) {
    if (!pageConfig[fieldKey]) return
    pageConfig[fieldKey].displayName = displayName
    saveConfig()
  }

  return {
    pageConfig,
    basicInfoFields,
    pageConfigFields,
    pageConfigTableColumns,
    loadConfig,
    saveConfig,
    setFieldVisible,
    setEnterJump,
    setDisplayName,
  }
}
