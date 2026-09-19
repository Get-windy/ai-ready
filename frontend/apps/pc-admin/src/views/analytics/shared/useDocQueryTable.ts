import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { docQueryApi } from '@/api/analytics'
import type { DocHistoryItem, PendingDocSummaryItem } from '@/api/analytics'

/** 综合单据三页共用的取数模式 */
export type DocQueryMode = 'PENDING' | 'DRAFT' | 'ALL'

/**
 * 综合单据台账取数（待审批单据 / 业务草稿 / 经营历程 共用）
 *
 * 三页表格结构、分页行为、合计行口径完全一致，仅查询条件与行级动作不同，
 * 故把「取数 + 分页 + 合计 + 待审批计数」收在一处；页面只负责把自身查询态转成后端参数。
 */
export function useDocQueryTable(mode: DocQueryMode, buildParams: () => Record<string, any>) {
  const loading = ref(false)
  const dataSource = ref<DocHistoryItem[]>([])
  const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
  /** 合计行金额（后端按当前过滤范围 SUM，非本页求和） */
  const summaryAmount = ref(0)
  /** 待审批单据：各单据类型待审批计数（其他两页恒为空） */
  const pendingSummary = ref<PendingDocSummaryItem[]>([])

  function clean(params: Record<string, any>): Record<string, any> {
    const out: Record<string, any> = {}
    Object.keys(params).forEach(k => {
      const v = params[k]
      if (v !== undefined && v !== null && v !== '') out[k] = v
    })
    return out
  }

  async function fetchData() {
    loading.value = true
    try {
      const params = clean({
        ...buildParams(),
        page: pagination.current,
        size: pagination.pageSize
      })
      const res: any = mode === 'PENDING'
        ? await docQueryApi.pendingDocsPage(params)
        : mode === 'DRAFT'
          ? await docQueryApi.draftDocsPage(params)
          : await docQueryApi.businessHistoryPage(params)
      // 行键：docTypeCode + docId（跨表 UNION，单独 docId 不足以表达「哪个域的哪张单」）
      dataSource.value = (res?.list || []).map((r: any) => ({
        ...r,
        rowKey: `${r.docTypeCode}:${r.docId}`
      }))
      pagination.total = Number(res?.total) || 0
      summaryAmount.value = Number(res?.summary?.amount) || 0
      pendingSummary.value = Array.isArray(res?.pendingSummary) ? res.pendingSummary : []
    } catch (e) {
      console.warn('[综合单据] 取数失败', e)
      message.error('获取数据失败')
      dataSource.value = []
      pagination.total = 0
      summaryAmount.value = 0
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    pagination.current = 1
    return fetchData()
  }

  function handlePageChange(page: number, size: number) {
    pagination.current = page
    pagination.pageSize = size
    return fetchData()
  }

  /** 金额排序（对标「金额」列可排序）：切换后端排序字段后回到第 1 页 */
  function handleSort(sortField: string, sortOrder: 'asc' | 'desc', sortState: { field: string; order: string }) {
    sortState.field = sortField
    sortState.order = sortOrder
    handleSearch()
  }

  return {
    loading,
    dataSource,
    pagination,
    summaryAmount,
    pendingSummary,
    fetchData,
    handleSearch,
    handlePageChange,
    handleSort
  }
}
