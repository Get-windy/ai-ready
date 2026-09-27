import { message } from 'ant-design-vue'
import { docActionApi } from '@/api/analytics'
import type { DocHistoryItem } from '@/api/analytics'
import { getDocType } from './docTypes'
import type { DocActionSupport } from './docTypes'

export type DocActionKind = keyof DocActionSupport

/**
 * 单据动作分发：按 docTypeCode 找到对应业务域的既有端点执行
 *
 * ⚠️ 一律调各域自己的 submit/approve/reject/删除 端点，**不直改状态列** —— 各域在这些端点里
 * 还承担库存事件、凭证生成、核销等副作用，绕过即产生脏数据。
 */
export async function runDocAction(
  record: DocHistoryItem,
  kind: DocActionKind,
  payload?: { note?: string; reason?: string }
): Promise<boolean> {
  const meta = getDocType(record.docTypeCode)
  if (!meta || !meta.actions[kind]) {
    message.warning(`${record.docType} 不支持该操作`)
    return false
  }
  try {
    // ⚠️ 费用申请单**只有审批**（approve/reject）走 JPA 审批流，入参与其余单据不同；
    //    submit/remove 与其它单据一样调费用域自己的通用端点
    //    （`POST /erp/expense/application/{id}/submit`、`DELETE /erp/expense/application/{id}`）。
    //    2026-09-23 修复：原判断只看了 docTypeCode、未同时看 kind，而 docTypes.ts 给 EXPENSE 登记的能力是
    //    `submit/approve/reject/remove` 四种 ⇒ 草稿页点「删除」或「提交记账」都会被送成 action=REJECT
    //    （即"删除"实际执行了"驳回"，越权改变单据审批状态）。
    if (record.docTypeCode === 'EXPENSE' && (kind === 'approve' || kind === 'reject')) {
      await docActionApi.expenseApproval(
        String(record.docId),
        kind === 'approve' ? 'APPROVE' : 'REJECT',
        kind === 'approve' ? payload?.note : payload?.reason
      )
    } else if (kind === 'remove') {
      await docActionApi.remove(meta.base, String(record.docId))
    } else if (kind === 'approve') {
      await docActionApi.approve(meta.base, String(record.docId), payload?.note)
    } else if (kind === 'reject') {
      await docActionApi.reject(meta.base, String(record.docId), payload?.reason)
    } else if (kind === 'copy') {
      await docActionApi.copy(meta.base, String(record.docId))
    } else if (kind === 'cancel') {
      await docActionApi.cancel(meta.base, String(record.docId), payload?.reason)
    } else {
      await docActionApi.submit(meta.base, String(record.docId))
    }
    return true
  } catch (e: any) {
    console.warn(`[综合单据] ${record.docType} ${kind} 失败`, e)
    message.error(e?.message || `${record.docType}操作失败`)
    return false
  }
}

/** 该单据类型是否支持某动作（用于按钮显隐，避免给出点了必失败的入口） */
export function canDo(code: string, kind: DocActionKind): boolean {
  const meta = getDocType(code)
  return !!(meta && meta.actions[kind])
}

/** 批量执行：逐单调用并汇总成功/失败（单张失败不中断其余） */
export async function runBatchDocAction(
  records: DocHistoryItem[],
  kind: DocActionKind,
  payload?: { note?: string; reason?: string }
): Promise<{ ok: number; fail: number }> {
  let ok = 0
  let fail = 0
  for (const r of records) {
    if (await runDocAction(r, kind, payload)) ok++
    else fail++
  }
  return { ok, fail }
}

/**
 * 登记 / 修改单据备注（经营历程行级「备注」）
 *
 * 走各业务域的 PUT /{id}，只提交 remark 一个字段 —— 依赖 MyBatis-Plus 默认的
 * NOT_NULL 更新策略（null 字段不参与 SET），因此不会覆盖其余列。
 */
export async function updateDocRemark(record: DocHistoryItem, remark: string): Promise<boolean> {
  const meta = getDocType(record.docTypeCode)
  if (!meta) {
    message.warning(`${record.docType} 不支持登记备注`)
    return false
  }
  try {
    await docActionApi.updateRemark(meta.base, String(record.docId), remark)
    return true
  } catch (e: any) {
    console.warn(`[综合单据] ${record.docType} 登记备注失败`, e)
    message.error(e?.message || '登记备注失败')
    return false
  }
}

/** 打开单据表单页（对标：单据编号为蓝色链接） */
export function openDocForm(router: any, record: DocHistoryItem) {
  const meta = getDocType(record.docTypeCode)
  if (!meta) {
    message.info(`暂不支持跳转：${record.docType}`)
    return
  }
  router.push({ path: meta.formPath, query: { id: String(record.docId) } })
}

/** 金额格式化（三页合计行与金额列统一口径：千分位 + 2 位小数） */
export function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
